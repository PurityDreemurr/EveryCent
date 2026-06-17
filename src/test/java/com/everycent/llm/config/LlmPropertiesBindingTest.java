package com.everycent.llm.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;

class LlmPropertiesBindingTest {

    @Test
    void shouldBindLlmPropertiesFromApplicationYaml() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        loader.load("application", new FileSystemResource("src/main/resources/config/application.yml")).forEach(propertySources::addFirst);

        LlmProperties properties = Binder.get(environment).bind("app.llm", Bindable.of(LlmProperties.class)).get();

        assertThat(properties.getEnabled()).isTrue();
        assertThat(properties.getProvider()).isEqualTo("openai-compatible");
        assertThat(properties.getModel()).isEqualTo("qwen3.6-flash");
        assertThat(properties.getBaseUrl()).isNotBlank();
        assertThat(properties.getBaseUrl()).doesNotContain("example.com");
        assertThat(properties.getApiKey()).doesNotContain("your_api_key_here");
        assertThat(properties.getTimeoutSeconds()).isEqualTo(20);
        assertThat(properties.getMinConfidence()).isEqualTo(0.70);
        assertThat(properties.getMaxInputLength()).isEqualTo(500);
    }
}
