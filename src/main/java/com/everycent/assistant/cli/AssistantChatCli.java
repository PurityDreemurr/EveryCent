package com.everycent.assistant.cli;

import com.everycent.assistant.AssistantReplyPostProcessor;
import com.everycent.assistant.dto.MemoryContextDTO;
import com.everycent.assistant.emotion.AiEmotionPromptAdapter;
import com.everycent.assistant.emotion.AiEmotionStateModel;
import com.everycent.assistant.emotion.AiEmotionTransitionResult;
import com.everycent.assistant.emotion.MecotEmotionReasoningResponseParser;
import com.everycent.assistant.emotion.MecotEmotionService;
import com.everycent.assistant.emotion.MecotRationalEmotionVector;
import com.everycent.assistant.prompt.AssistantPromptBuilder;
import com.everycent.assistant.prompt.MecotEmotionReasoningPromptBuilder;
import com.everycent.assistant.rewrite.LlmRewriteService;
import com.everycent.assistant.rewrite.RewritePromptBuilder;
import com.everycent.assistant.validation.DialogueScene;
import com.everycent.assistant.validation.DialogueSceneClassifier;
import com.everycent.assistant.validation.ReplyOutputValidator;
import com.everycent.assistant.validation.ReplyValidationResult;
import com.everycent.domain.AiEmotionState;
import com.everycent.domain.EmotionTag;
import com.everycent.domain.enumeration.EmotionValence;
import com.everycent.llm.client.LlmClientException;
import com.everycent.llm.client.OpenAiCompatibleLlmClient;
import com.everycent.llm.config.LlmProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class AssistantChatCli {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int HISTORY_LIMIT = 5;
    private static final String INITIAL_USER_EMOTION_STATE = "中立";

    private final AssistantPromptBuilder promptBuilder = new AssistantPromptBuilder();
    private final MecotEmotionReasoningPromptBuilder emotionReasoningPromptBuilder = new MecotEmotionReasoningPromptBuilder();
    private final MecotEmotionService mecotEmotionService = new MecotEmotionService();
    private final AiEmotionPromptAdapter emotionPromptAdapter = new AiEmotionPromptAdapter();
    private final MecotEmotionReasoningResponseParser emotionReasoningResponseParser = new MecotEmotionReasoningResponseParser(OBJECT_MAPPER);
    private final OpenAiCompatibleLlmClient llmClient;
    private final DialogueSceneClassifier dialogueSceneClassifier = new DialogueSceneClassifier();
    private final ReplyOutputValidator replyOutputValidator = new ReplyOutputValidator(OBJECT_MAPPER);
    private final AssistantReplyPostProcessor replyPostProcessor;
    private final ArrayDeque<String> dialogueHistory = new ArrayDeque<>();

    private AiEmotionState aiEmotionState = AiEmotionStateModel.defaultState();
    private boolean debug;

    private AssistantChatCli(OpenAiCompatibleLlmClient llmClient, boolean debug) {
        this.llmClient = llmClient;
        this.replyPostProcessor =
            new AssistantReplyPostProcessor(
                new ReplyOutputValidator(OBJECT_MAPPER),
                new LlmRewriteService(new RewritePromptBuilder(), llmClient)
            );
        this.debug = debug;
    }

    public static void main(String[] args) throws IOException {
        Map<String, String> env = loadLocalEnv();
        boolean debug = hasArg(args, "--debug") || Boolean.parseBoolean(env("APP_ASSISTANT_CLI_DEBUG", "false", env));
        AssistantChatCli cli = new AssistantChatCli(new OpenAiCompatibleLlmClient(llmProperties(env), OBJECT_MAPPER), debug);
        cli.run();
    }

    private void run() throws IOException {
        printBanner();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (true) {
                System.out.print("\n你> ");
                String input = reader.readLine();
                if (input == null || isExit(input)) {
                    System.out.println("EveryCent> 已退出。");
                    return;
                }
                if (handleCommand(input)) {
                    continue;
                }
                if (!StringUtils.hasText(input)) {
                    continue;
                }
                reply(input.trim());
            }
        }
    }

    private void reply(String userInput) {
        String prompt = promptBuilder.buildSingleTurnPrompt(userInput, INITIAL_USER_EMOTION_STATE, List.of());

        if (debug) {
            printDebugPrompt(prompt);
        }

        try {
            String rawAnswer = llmClient.complete(prompt);
            DialogueScene scene = dialogueSceneClassifier.classify(userInput);
            String answer = replyPostProcessor.process(userInput, rawAnswer, scene);
            if (debug) {
                printDebugPostProcess(scene, rawAnswer, answer);
            }
            System.out.println("\nEveryCent> " + answer.strip());
        } catch (LlmClientException e) {
            System.out.println("\n[LLM 调用失败] " + e.getMessage());
            if (e.getCause() != null && StringUtils.hasText(e.getCause().getMessage())) {
                System.out.println("[原因] " + e.getCause().getMessage());
            }
        }
    }

    private MecotRationalEmotionVector reasonEmotionVector(String emotionReasoningPrompt) {
        try {
            String response = llmClient.completeRaw(emotionReasoningPrompt);
            return emotionReasoningResponseParser.parse(response);
        } catch (LlmClientException e) {
            System.out.println("\n[MeCOT 情绪推理失败，使用中性向量] " + e.getMessage());
            return new MecotRationalEmotionVector(0.0, 0.0, "calm", "llm_reasoning_failed");
        }
    }

    private boolean handleCommand(String input) {
        String command = input.trim().toLowerCase(Locale.ROOT);
        if ("/debug".equals(command)) {
            debug = !debug;
            System.out.println("debug=" + debug);
            return true;
        }
        if ("/state".equals(command)) {
            System.out.println("MeCOT 已屏蔽：当前 CLI 只使用系统提示词和用户输入。");
            return true;
        }
        if ("/reset".equals(command)) {
            dialogueHistory.clear();
            aiEmotionState = AiEmotionStateModel.defaultState();
            System.out.println("已重置。当前未启用历史记忆和 MeCOT 状态。");
            return true;
        }
        if ("/help".equals(command)) {
            printHelp();
            return true;
        }
        if (command.startsWith("/validate")) {
            validateReply(input.substring("/validate".length()).trim());
            return true;
        }
        return false;
    }

    private void validateReply(String reply) {
        if (!StringUtils.hasText(reply)) {
            System.out.println("用法：/validate 回复内容{\"mood\":40,\"emoji\":\"peace\"}");
            return;
        }
        ReplyValidationResult result = replyOutputValidator.validate(reply, DialogueScene.DAILY_CHAT);
        System.out.println("passed=" + result.isPassed());
        System.out.println("violations=" + result.getViolations());
    }

    private List<MemoryContextDTO> historyMemories() {
        List<MemoryContextDTO> memories = new ArrayList<>();
        int index = 1;
        for (String history : dialogueHistory) {
            MemoryContextDTO memory = new MemoryContextDTO();
            memory.setMemoryId((long) index);
            memory.setMemoryType("CLI_DIALOGUE_HISTORY");
            memory.setContent(history);
            memory.setScore(1.0);
            memories.add(memory);
            index++;
        }
        return memories;
    }

    private void remember(String userInput, String answer) {
        dialogueHistory.addLast("用户：" + userInput + "\nEveryCent：" + answer);
        while (dialogueHistory.size() > HISTORY_LIMIT) {
            dialogueHistory.removeFirst();
        }
    }

    private EmotionTag classifyUserEmotion(String userInput) {
        String code = classifyUserEmotionCode(userInput);
        EmotionTag tag = new EmotionTag();
        tag.setCode(code);
        tag.setName(code);
        tag.setValence(switch (code) {
            case "HAPPY", "CALM" -> EmotionValence.POSITIVE;
            case "NONE" -> EmotionValence.NEUTRAL;
            default -> EmotionValence.NEGATIVE;
        });
        tag.setSystemDefault(true);
        return tag;
    }

    private String classifyUserEmotionCode(String userInput) {
        String text = userInput.toLowerCase(Locale.ROOT);
        if (containsAny(text, "生气", "气死", "火大", "愤怒", "烦死", "不爽")) {
            return "ANGRY";
        }
        if (
            containsAny(
                text,
                "没让你",
                "你记什么账",
                "不懂记账",
                "记了也没用",
                "摆设",
                "没用",
                "乱记",
                "别记",
                "不用你记"
            )
        ) {
            return "INVALIDATED";
        }
        if (containsAny(text, "开心", "高兴", "舒服", "顺利", "谢谢", "好一点", "轻松")) {
            return "HAPPY";
        }
        if (containsAny(text, "焦虑", "害怕", "担心", "慌", "不安")) {
            return "ANXIOUS";
        }
        if (containsAny(text, "压力", "累", "崩溃", "撑不住", "超支", "麻烦")) {
            return "STRESSED";
        }
        if (containsAny(text, "后悔", "自责", "不该")) {
            return "REGRET";
        }
        if (containsAny(text, "平静", "冷静", "还好", "没事")) {
            return "CALM";
        }
        return "NONE";
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void printDebugPrompt(String prompt) {
        System.out.println("\n--- DEBUG ---");
        System.out.println("rag=false, memoryPersist=false, mecot=false, postProcess=true");
        System.out.println("prompt:\n" + prompt);
        System.out.println("--- DEBUG END ---");
    }

    private void printDebugPostProcess(DialogueScene scene, String rawAnswer, String finalAnswer) {
        System.out.println("\n--- POST PROCESS DEBUG ---");
        System.out.println("scene=" + scene);
        System.out.println("raw:\n" + rawAnswer);
        System.out.println("final:\n" + finalAnswer);
        System.out.println("rewritten=" + !rawAnswer.equals(finalAnswer));
        System.out.println("--- POST PROCESS DEBUG END ---");
    }

    private static LlmProperties llmProperties(Map<String, String> localEnv) {
        LlmProperties properties = new LlmProperties();
        properties.setProvider(env("APP_LLM_PROVIDER", "openai-compatible", localEnv));
        properties.setBaseUrl(env("APP_LLM_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1", localEnv));
        properties.setApiKey(env("APP_LLM_API_KEY", "", localEnv));
        properties.setModel(env("APP_LLM_MODEL", "qwen3.6-flash", localEnv));
        properties.setTimeoutSeconds(Math.max(60, parseInt(env("APP_LLM_TIMEOUT_SECONDS", "60", localEnv), 60)));
        properties.setMaxTokens(parseInt(env("APP_LLM_MAX_TOKENS", "180", localEnv), 180));
        properties.setTemperature(parseDouble(env("APP_LLM_TEMPERATURE", "0.75", localEnv), 0.75));
        properties.setTopP(parseDouble(env("APP_LLM_TOP_P", "0.85", localEnv), 0.85));
        properties.setRandomizeSampling(Boolean.parseBoolean(env("APP_LLM_RANDOMIZE_SAMPLING", "false", localEnv)));
        properties.setMinTemperature(parseDouble(env("APP_LLM_MIN_TEMPERATURE", "0.75", localEnv), 0.75));
        properties.setMaxTemperature(parseDouble(env("APP_LLM_MAX_TEMPERATURE", "0.75", localEnv), 0.75));
        properties.setMinTopP(parseDouble(env("APP_LLM_MIN_TOP_P", "0.85", localEnv), 0.85));
        properties.setMaxTopP(parseDouble(env("APP_LLM_MAX_TOP_P", "0.85", localEnv), 0.85));
        properties.setEnableThinking(Boolean.parseBoolean(env("APP_LLM_ENABLE_THINKING", "false", localEnv)));
        properties.setEnableSearch(Boolean.parseBoolean(env("APP_LLM_ENABLE_SEARCH", "false", localEnv)));
        return properties;
    }

    private static String env(String name, String defaultValue, Map<String, String> localEnv) {
        String value = System.getenv(name);
        if (StringUtils.hasText(value)) {
            return value;
        }
        value = localEnv.get(name);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private static Map<String, String> loadLocalEnv() {
        Path path = Path.of("src/main/docker/everycent.env");
        Map<String, String> values = new LinkedHashMap<>();
        if (!Files.isRegularFile(path)) {
            return values;
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isBlank() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                values.put(trimmed.substring(0, separator), trimmed.substring(separator + 1));
            }
        } catch (IOException ignored) {
            // Environment variables remain the source of truth when the local env file cannot be read.
        }
        return values;
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double parseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean hasArg(String[] args, String expected) {
        for (String arg : args) {
            if (expected.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExit(String input) {
        String trimmed = input.trim().toLowerCase(Locale.ROOT);
        return "/exit".equals(trimmed) || "/quit".equals(trimmed) || "exit".equals(trimmed) || "quit".equals(trimmed);
    }

    private static void printBanner() {
            System.out.println("EveryCent 财务助手 CLI 对话（功能型提示词模式）");
        printHelp();
    }

    private static void printHelp() {
        System.out.println("命令：/state 查看模式，/debug 切换调试，/validate <回复> 校验回复，/reset 重置，/exit 退出。");
    }

    private AssistantChatCli() {
        throw new IllegalStateException("Utility class");
    }
}
