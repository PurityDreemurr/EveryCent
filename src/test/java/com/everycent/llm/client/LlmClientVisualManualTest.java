package com.everycent.llm.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.everycent.llm.config.LlmProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;

class LlmClientVisualManualTest {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void generateVisualReportForRealAliyunConfigAndExceptionCases() throws Exception {
        LlmProperties realProperties = loadRealLlmProperties();
        List<TestCaseResult> results = new ArrayList<>();

        results.add(
            runCase(
                "正常输入：真实阿里云 LLM 调用",
                "使用 application.yml 中的 baseUrl、apiKey、model，发送简单测试提示词。",
                "应返回非空模型文本；如果网络、额度、认证或模型配置异常，应显示 LlmClientException。",
                () -> createClient(realProperties).complete("请用一句话回复：King是天才。")
            )
        );
        results.add(
            runCase(
                "异常输入：prompt 为空",
                "传入空白 prompt，不发起外部 API 调用。",
                "应抛出 LlmClientException，提示 prompt 不能为空。",
                () -> createClient(realProperties).complete(" ")
            )
        );
        results.add(
            runCase(
                "异常配置：API Key 缺失",
                "构造缺失 apiKey 的 LlmProperties，不发起外部 API 调用。",
                "应抛出 LlmClientException，提示 API Key 未配置。",
                () -> createClient(copyWith(realProperties, realProperties.getBaseUrl(), "", realProperties.getModel(), 20)).complete("测试")
            )
        );
        results.add(
            runCase(
                "异常配置：baseUrl 缺失",
                "构造缺失 baseUrl 的 LlmProperties，不发起外部 API 调用。",
                "应抛出 LlmClientException，提示 baseUrl 未配置。",
                () -> createClient(copyWith(realProperties, "", "test-key", realProperties.getModel(), 20)).complete("测试")
            )
        );
        results.add(
            runCase(
                "异常配置：model 缺失",
                "构造缺失 model 的 LlmProperties，不发起外部 API 调用。",
                "应抛出 LlmClientException，提示 model 未配置。",
                () -> createClient(copyWith(realProperties, realProperties.getBaseUrl(), "test-key", "", 20)).complete("测试")
            )
        );
        results.add(
            runCase(
                "异常配置：timeout 非法",
                "构造 timeoutSeconds=0 的 LlmProperties，不发起外部 API 调用。",
                "应抛出 LlmClientException，提示 timeout 配置无效。",
                () -> createClient(copyWith(realProperties, realProperties.getBaseUrl(), "test-key", realProperties.getModel(), 0)).complete("测试")
            )
        );
        results.add(runHttpCase("异常响应：HTTP 401", 401, "{\"error\":{\"message\":\"unauthorized\"}}", "应记录认证失败并抛出 LlmClientException。"));
        results.add(runHttpCase("异常响应：HTTP 429", 429, "{\"error\":{\"message\":\"rate limit\"}}", "应识别限流并抛出 LlmClientException。"));
        results.add(runHttpCase("异常响应：HTTP 500", 500, "{\"error\":{\"message\":\"server error\"}}", "应识别云端服务异常并抛出 LlmClientException。"));
        results.add(runHttpCase("异常响应：模型返回空内容", 200, "{\"choices\":[{\"message\":{\"content\":\"\"}}]}", "应抛出 LlmClientException，提示返回内容为空。"));
        results.add(runHttpCase("异常响应：JSON 格式错误", 200, "{bad-json", "应抛出 LlmClientException，提示响应解析失败。"));

        printTerminalReport(realProperties, results);

        assertThat(results).hasSize(11);
    }

    private static TestCaseResult runHttpCase(String name, int status, String body, String expected) {
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> writeResponse(exchange, status, body));
            server.start();

            LlmProperties properties = copyWith(
                new LlmProperties(),
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "test-key",
                "test-model",
                5
            );
            return runCase(name, "使用本地临时 HTTP 服务模拟模型响应，不调用真实阿里云 API。", expected, () -> createClient(properties).complete("测试"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (server != null) {
                server.stop(0);
            }
        }
    }

    private static TestCaseResult runCase(String name, String input, String expected, Supplier<String> action) {
        long startedAt = System.nanoTime();
        try {
            String output = action.get();
            return new TestCaseResult(name, input, expected, "成功", abbreviate(output), null, elapsedMillis(startedAt));
        } catch (Exception e) {
            return new TestCaseResult(
                name,
                input,
                expected,
                "异常",
                abbreviate(e.getMessage()),
                e.getClass().getName(),
                elapsedMillis(startedAt)
            );
        }
    }

    private static OpenAiCompatibleLlmClient createClient(LlmProperties properties) {
        return new OpenAiCompatibleLlmClient(properties, OBJECT_MAPPER);
    }

    private static LlmProperties loadRealLlmProperties() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        loader.load("application", new FileSystemResource("src/main/resources/config/application.yml")).forEach(propertySources::addFirst);
        return Binder.get(environment).bind("app.llm", Bindable.of(LlmProperties.class)).orElse(new LlmProperties());
    }

    private static LlmProperties copyWith(LlmProperties source, String baseUrl, String apiKey, String model, Integer timeoutSeconds) {
        LlmProperties copy = new LlmProperties();
        copy.setEnabled(source.getEnabled());
        copy.setProvider(source.getProvider());
        copy.setBaseUrl(baseUrl);
        copy.setApiKey(apiKey);
        copy.setModel(model);
        copy.setTimeoutSeconds(timeoutSeconds);
        copy.setMinConfidence(source.getMinConfidence());
        copy.setMaxInputLength(source.getMaxInputLength());
        return copy;
    }

    private static void writeResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private static void printTerminalReport(LlmProperties properties, List<TestCaseResult> results) {
        StringBuilder report = new StringBuilder();
        report.append("\n");
        report.append("============================================================\n");
        report.append("EveryCent LLM Client 终端测试报告\n");
        report.append("生成时间: ").append(LocalDateTime.now().format(DATE_TIME_FORMATTER)).append("\n");
        report.append("============================================================\n");
        report.append("配置摘要\n");
        report.append("- Provider: ").append(nullToBlank(properties.getProvider())).append("\n");
        report.append("- Base URL: ").append(nullToBlank(properties.getBaseUrl())).append("\n");
        report.append("- Model: ").append(nullToBlank(properties.getModel())).append("\n");
        report.append("- API Key: ").append(hasText(properties.getApiKey()) ? "已配置，终端不展示" : "未配置").append("\n");
        report.append("- Timeout Seconds: ").append(properties.getTimeoutSeconds()).append("\n");
        report.append("------------------------------------------------------------\n");
        report.append("测试结果\n");
        for (TestCaseResult result : results) {
            report.append("\n");
            report.append("测试点: ").append(result.name()).append("\n");
            report.append("输入/场景:\n").append(indent(result.input())).append("\n");
            report.append("预计输出:\n").append(indent(result.expected())).append("\n");
            report.append("实际状态: ").append(result.status()).append("\n");
            report.append("实际输出:\n").append(indent(result.output())).append("\n");
            report.append("异常类型: ").append(result.exceptionType() == null ? "-" : result.exceptionType()).append("\n");
            report.append("耗时: ").append(result.elapsedMillis()).append(" ms\n");
            report.append("------------------------------------------------------------\n");
        }
        report.append("汇总: 共 ").append(results.size()).append(" 个测试点，请重点审查“正常输入”是否成功返回模型文本。\n");
        report.append("============================================================\n");
        System.out.println(report);
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private static String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 1200 ? value.substring(0, 1200) + "\n...[已截断]" : value;
    }

    private static String indent(String value) {
        if (value == null) {
            return "";
        }
        return "  " + value.replace("\n", "\n  ");
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record TestCaseResult(
        String name,
        String input,
        String expected,
        String status,
        String output,
        String exceptionType,
        long elapsedMillis
    ) {}
}
