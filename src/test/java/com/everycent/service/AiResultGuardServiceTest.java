package com.everycent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.everycent.EveryCentApp;
import com.everycent.domain.BehaviorTag;
import com.everycent.domain.EmotionTag;
import com.everycent.domain.Ledger;
import com.everycent.domain.User;
import com.everycent.domain.enumeration.EmotionValence;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.llm.dto.TransactionParseResultDTO;
import com.everycent.repository.BehaviorTagRepository;
import com.everycent.repository.EmotionTagRepository;
import com.everycent.repository.LedgerRepository;
import com.everycent.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = EveryCentApp.class, properties = "spring.docker.compose.enabled=false")
@Transactional
class AiResultGuardServiceTest {

    @Autowired
    private AiResultGuardService guardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private BehaviorTagRepository behaviorTagRepository;

    @Autowired
    private EmotionTagRepository emotionTagRepository;

    @Test
    void shouldConnectDatabaseReadTagsAndValidateAiResult() {
        TestData testData = prepareTestData();

        assertThat(ledgerRepository.findById(testData.ledger().getId())).isPresent();
        assertThat(behaviorTagRepository.findOneByCode(testData.behaviorTag().getCode())).isPresent();
        assertThat(emotionTagRepository.findOneByCode(testData.emotionTag().getCode())).isPresent();

        TransactionParseResultDTO result = validResult(testData);
        result.setRawInput("今天午饭 50 元，很开心");

        TransactionParseResultDTO validated = guardService.validateTransactionResult(result, testData.ledger().getId(), testData.user());

        assertThat(validated.getBehaviorTagId()).isEqualTo(testData.behaviorTag().getId());
        assertThat(validated.getBehaviorTagName()).isEqualTo(testData.behaviorTag().getName());
        assertThat(validated.getEmotionTagId()).isEqualTo(testData.emotionTag().getId());
        assertThat(validated.getEmotionTagName()).isEqualTo(testData.emotionTag().getName());
        assertThat(validated.getNeedUserConfirm()).isFalse();
    }

    @Test
    void shouldRejectAmountOutOfRange() {
        TestData testData = prepareTestData();
        TransactionParseResultDTO result = validResult(testData);
        result.setAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> guardService.validateTransactionResult(result, testData.ledger().getId(), testData.user()))
            .isInstanceOf(InvalidAiResultException.class)
            .hasMessageContaining("金额");
    }

    @Test
    void shouldRejectUnknownBehaviorTag() {
        TestData testData = prepareTestData();
        TransactionParseResultDTO result = validResult(testData);
        result.setBehaviorTagCode("NOT_EXISTS_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));

        assertThatThrownBy(() -> guardService.validateTransactionResult(result, testData.ledger().getId(), testData.user()))
            .isInstanceOf(InvalidAiResultException.class)
            .hasMessageContaining("行为标签不存在");
    }

    @Test
    void shouldRejectTransactionDateOutOfRange() {
        TestData testData = prepareTestData();
        TransactionParseResultDTO result = validResult(testData);
        result.setTransactionDate(LocalDate.now().plusYears(2));

        assertThatThrownBy(() -> guardService.validateTransactionResult(result, testData.ledger().getId(), testData.user()))
            .isInstanceOf(InvalidAiResultException.class)
            .hasMessageContaining("日期超出合理范围");
    }

    @Test
    void shouldRequireUserConfirmWhenConfidenceIsLow() {
        TestData testData = prepareTestData();
        TransactionParseResultDTO result = validResult(testData);
        result.setConfidence(0.1);

        TransactionParseResultDTO validated = guardService.validateTransactionResult(result, testData.ledger().getId(), testData.user());

        assertThat(validated.getNeedUserConfirm()).isTrue();
    }

    private TestData prepareTestData() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        User user = new User();
        user.setLogin("llmtest" + suffix);
        user.setPassword("$2a$10$7EqJtq98hPqEX7fNZaFWoOHiUyJNrHHu4XW7l96jWcD5fJmOVeLaK");
        user.setEmail("llmtest" + suffix + "@example.com");
        user.setActivated(true);
        user.setLangKey("zh-cn");
        user = userRepository.saveAndFlush(user);

        Ledger ledger = new Ledger()
            .name("LLM测试账本" + suffix)
            .defaultCurrency("CNY")
            .currentMonthBalance(BigDecimal.ZERO)
            .createdDate(Instant.now())
            .creator(user);
        ledger = ledgerRepository.saveAndFlush(ledger);

        BehaviorTag behaviorTag = new BehaviorTag().code("FOOD_" + suffix).name("餐饮").systemDefault(false);
        behaviorTag = behaviorTagRepository.saveAndFlush(behaviorTag);

        EmotionTag emotionTag = new EmotionTag().code("HAPPY_" + suffix).name("开心").valence(EmotionValence.POSITIVE).systemDefault(false);
        emotionTag = emotionTagRepository.saveAndFlush(emotionTag);

        return new TestData(user, ledger, behaviorTag, emotionTag);
    }

    private TransactionParseResultDTO validResult(TestData testData) {
        TransactionParseResultDTO result = new TransactionParseResultDTO();
        result.setAmount(new BigDecimal("50.00"));
        result.setType(TransactionType.EXPENSE);
        result.setBehaviorTagCode(testData.behaviorTag().getCode());
        result.setEmotionTagCode(testData.emotionTag().getCode());
        result.setTransactionDate(LocalDate.now());
        result.setDescription("中午吃饭");
        result.setConfidence(0.92);
        return result;
    }

    private record TestData(User user, Ledger ledger, BehaviorTag behaviorTag, EmotionTag emotionTag) {}
}
