package com.everycent.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.everycent.EveryCentApp;
import com.everycent.config.AsyncSyncConfiguration;
import com.everycent.config.JacksonConfiguration;
import com.everycent.domain.BehaviorTag;
import com.everycent.domain.Budget;
import com.everycent.domain.EmotionTag;
import com.everycent.domain.Ledger;
import com.everycent.domain.NotificationMessage;
import com.everycent.domain.TransactionRecord;
import com.everycent.domain.User;
import com.everycent.domain.UserLedgerPermission;
import com.everycent.domain.enumeration.BudgetCycle;
import com.everycent.domain.enumeration.EmotionValence;
import com.everycent.domain.enumeration.PermissionLevel;
import com.everycent.domain.enumeration.PermissionStatus;
import com.everycent.domain.enumeration.RecordSource;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.llm.client.LlmClient;
import com.everycent.llm.client.LlmClientException;
import com.everycent.llm.dto.AiAlertRequestDTO;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateRequestDTO;
import com.everycent.llm.dto.TransactionParseRequestDTO;
import com.everycent.repository.BehaviorTagRepository;
import com.everycent.repository.BudgetRepository;
import com.everycent.repository.EmotionTagRepository;
import com.everycent.repository.LedgerRepository;
import com.everycent.repository.NotificationMessageRepository;
import com.everycent.repository.TransactionRecordRepository;
import com.everycent.repository.UserLedgerPermissionRepository;
import com.everycent.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@SpringBootTest(classes = { EveryCentApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class })
@Transactional
@WithMockUser("llm-it-user")
class LlmResourceIT {

    private static final String PASSWORD_HASH = "$2a$10$L7T44Yo4U6V3uTjwm2vXFeXfzx1Nr4iS7M0iDU1pmf.8uVvDy.7Wq";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private BehaviorTagRepository behaviorTagRepository;

    @Autowired
    private EmotionTagRepository emotionTagRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    @Autowired
    private NotificationMessageRepository notificationMessageRepository;

    @Autowired
    private UserLedgerPermissionRepository userLedgerPermissionRepository;

    @MockitoBean
    private LlmClient llmClient;

    private User currentUser;
    private User ownerUser;
    private Ledger writableLedger;
    private Ledger readOnlyLedger;
    private BehaviorTag foodTag;
    private EmotionTag happyTag;

    @BeforeEach
    void setUp() {
        String suffix = Long.toString(System.nanoTime());
        currentUser = saveUser("llm-it-user-" + suffix);
        ownerUser = saveUser("llm-it-owner-" + suffix);
        SecurityContextHolder
            .getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    currentUser.getLogin(),
                    "password",
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
            );
        writableLedger = saveLedger("可写账本", currentUser);
        readOnlyLedger = saveLedger("只读账本", ownerUser);
        userLedgerPermissionRepository.save(
            new UserLedgerPermission()
                .user(currentUser)
                .ledger(readOnlyLedger)
                .permissionLevel(PermissionLevel.READ_ONLY)
                .status(PermissionStatus.ACTIVE)
                .createdDate(Instant.now())
        );
        foodTag = behaviorTagRepository.save(new BehaviorTag().code(unique("FOOD")).name("餐饮").systemDefault(true));
        happyTag = emotionTagRepository.save(new EmotionTag().code(unique("HAPPY")).name("开心").valence(EmotionValence.POSITIVE).systemDefault(true));
    }

    @Test
    void shouldReturnStructuredResultFromAiParsePreviewApi() throws Exception {
        when(llmClient.complete(org.mockito.ArgumentMatchers.anyString())).thenReturn(transactionJson(foodTag.getCode(), happyTag.getCode()));
        TransactionParseRequestDTO request = new TransactionParseRequestDTO();
        request.setLedgerId(writableLedger.getId());
        request.setText("今天午饭花了 50 元");
        request.setTransactionDate(LocalDate.of(2026, 6, 17));

        mockMvc
            .perform(post("/api/ai/transaction/parse").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value(50.00))
            .andExpect(jsonPath("$.type").value("EXPENSE"))
            .andExpect(jsonPath("$.behaviorTagName").value("餐饮"))
            .andExpect(jsonPath("$.emotionTagName").value("开心"))
            .andExpect(jsonPath("$.needUserConfirm").value(false));
    }

    @Test
    void shouldRejectReadOnlyUserForAiParsePreviewApi() throws Exception {
        TransactionParseRequestDTO request = new TransactionParseRequestDTO();
        request.setLedgerId(readOnlyLedger.getId());
        request.setText("今天午饭花了 50 元");
        request.setTransactionDate(LocalDate.of(2026, 6, 17));

        mockMvc
            .perform(post("/api/ai/transaction/parse").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldFailWhenLlmReturnsUnknownTag() throws Exception {
        when(llmClient.complete(org.mockito.ArgumentMatchers.anyString())).thenReturn(transactionJson("UNKNOWN_BEHAVIOR", happyTag.getCode()));
        TransactionParseRequestDTO request = new TransactionParseRequestDTO();
        request.setLedgerId(writableLedger.getId());
        request.setText("今天午饭花了 50 元");
        request.setTransactionDate(LocalDate.of(2026, 6, 17));

        mockMvc
            .perform(post("/api/ai/transaction/parse").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGenerateBudgetAlertFromBudgetAndTransactions() throws Exception {
        Budget budget = saveBudget();
        saveExpense(new BigDecimal("820.00"), LocalDate.of(2026, 6, 15));
        when(llmClient.complete(org.mockito.ArgumentMatchers.anyString())).thenReturn(alertJson());
        AiAlertRequestDTO request = new AiAlertRequestDTO();
        request.setLedgerId(writableLedger.getId());
        request.setBudgetId(budget.getId());
        request.setSaveAsNotification(false);

        mockMvc
            .perform(post("/api/ai/budget-alert/generate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("预算提醒"))
            .andExpect(jsonPath("$.usedAmount").value(820.00))
            .andExpect(jsonPath("$.limitAmount").value(1000.00))
            .andExpect(jsonPath("$.usedRatio").value(0.8200))
            .andExpect(jsonPath("$.needNotification").value(true));
    }

    @Test
    void shouldReturnServiceUnavailableWhenLlmClientFails() throws Exception {
        when(llmClient.complete(org.mockito.ArgumentMatchers.anyString())).thenThrow(new LlmClientException("LLM 网络超时或不可达，请使用手动记账"));
        TransactionParseRequestDTO request = new TransactionParseRequestDTO();
        request.setLedgerId(writableLedger.getId());
        request.setText("今天午饭花了 50 元");
        request.setTransactionDate(LocalDate.of(2026, 6, 17));

        mockMvc
            .perform(post("/api/ai/transaction/parse").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.detail").value("LLM 网络超时或不可达，请使用手动记账"));
    }

    @Test
    void shouldPersistNotificationWhenSaveAsNotificationIsTrue() throws Exception {
        Budget budget = saveBudget();
        saveExpense(new BigDecimal("900.00"), LocalDate.of(2026, 6, 15));
        long notificationCountBefore = notificationMessageRepository.count();
        when(llmClient.complete(org.mockito.ArgumentMatchers.anyString())).thenReturn(alertJson());
        AiAlertRequestDTO request = new AiAlertRequestDTO();
        request.setLedgerId(writableLedger.getId());
        request.setBudgetId(budget.getId());
        request.setSaveAsNotification(true);

        mockMvc
            .perform(post("/api/ai/budget-alert/generate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notificationId").isNumber());

        assertThat(notificationMessageRepository.count()).isEqualTo(notificationCountBefore + 1);
        NotificationMessage notification = notificationMessageRepository.findAll().get((int) notificationCountBefore);
        assertThat(notification.getTitle()).isEqualTo("预算提醒");
        assertThat(notification.getBudget()).isEqualTo(budget);
        assertThat(notification.getLedger()).isEqualTo(writableLedger);
        assertThat(notification.getUser()).isEqualTo(currentUser);
    }

    @Test
    void shouldCreateTransactionFromNaturalLanguageAfterUserConfirm() throws Exception {
        when(llmClient.complete(org.mockito.ArgumentMatchers.anyString())).thenReturn(transactionJson(foodTag.getCode(), happyTag.getCode()));
        long recordCountBefore = transactionRecordRepository.count();
        NaturalLanguageTransactionCreateRequestDTO request = new NaturalLanguageTransactionCreateRequestDTO();
        request.setText("今天午饭花了 50 元");
        request.setTransactionDate(LocalDate.of(2026, 6, 17));
        request.setConfirm(true);

        mockMvc
            .perform(
                post("/api/ledgers/{ledgerId}/transactions/natural-language", writableLedger.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactionId").isNumber())
            .andExpect(jsonPath("$.amount").value(50.00))
            .andExpect(jsonPath("$.behaviorTagName").value("餐饮"));

        assertThat(transactionRecordRepository.count()).isEqualTo(recordCountBefore + 1);
        TransactionRecord record = transactionRecordRepository.findAll().get((int) recordCountBefore);
        assertThat(record.getSource()).isEqualTo(RecordSource.NATURAL_LANGUAGE);
        assertThat(record.getLedger()).isEqualTo(writableLedger);
        assertThat(record.getCreator()).isEqualTo(currentUser);
    }

    private User saveUser(String login) {
        User user = new User();
        user.setLogin(login);
        user.setPassword(PASSWORD_HASH);
        user.setEmail(login + "@example.com");
        user.setActivated(true);
        user.setLangKey("zh-cn");
        return userRepository.saveAndFlush(user);
    }

    private Ledger saveLedger(String name, User creator) {
        return ledgerRepository.saveAndFlush(new Ledger().name(name).createdDate(Instant.now()).creator(creator));
    }

    private Budget saveBudget() {
        return budgetRepository.saveAndFlush(
            new Budget()
                .ledger(writableLedger)
                .cycle(BudgetCycle.MONTHLY)
                .periodStart(LocalDate.of(2026, 6, 1))
                .periodEnd(LocalDate.of(2026, 6, 30))
                .limitAmount(new BigDecimal("1000.00"))
                .alertThreshold(new BigDecimal("0.80"))
                .enabled(true)
        );
    }

    private void saveExpense(BigDecimal amount, LocalDate date) {
        transactionRecordRepository.saveAndFlush(
            new TransactionRecord()
                .ledger(writableLedger)
                .creator(currentUser)
                .amount(amount)
                .type(TransactionType.EXPENSE)
                .transactionDate(date)
                .source(RecordSource.MANUAL)
                .createdDate(Instant.now())
                .behaviorTag(foodTag)
                .emotionTag(happyTag)
        );
    }

    private String transactionJson(String behaviorTagCode, String emotionTagCode) {
        return """
            {
              "amount": "50.00",
              "type": "EXPENSE",
              "behaviorTagCode": "%s",
              "emotionTagCode": "%s",
              "transactionDate": "2026-06-17",
              "description": "午饭",
              "confidence": 0.95
            }
            """.formatted(behaviorTagCode, emotionTagCode);
    }

    private String alertJson() {
        return """
            {
              "title": "预算提醒",
              "content": "本月预算使用偏高，请留意后续支出。",
              "level": "WARNING",
              "needNotification": true
            }
            """;
    }

    private String unique(String prefix) {
        return prefix + "_" + System.nanoTime();
    }
}
