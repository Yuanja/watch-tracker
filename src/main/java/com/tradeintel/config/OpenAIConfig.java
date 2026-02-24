package com.tradeintel.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * OpenAI API client configuration.
 *
 * <p>Reads all {@code app.openai.*} properties through a dedicated
 * {@link OpenAIProperties} record bound via {@link ConfigurationProperties}.
 * Exposes a pre-configured {@link RestTemplate} bean with the API key already
 * attached as a default {@code Authorization} header so that callers never need
 * to handle the credential themselves.
 *
 * <p>Configuration keys (all under {@code app.openai}):
 * <ul>
 *   <li>{@code api-key}          — OpenAI secret key (required, injected from env)</li>
 *   <li>{@code extraction-model} — model used for listing extraction (default:
 *       {@code gpt-4o-mini})</li>
 *   <li>{@code chat-model}       — model used for the chat AI agent (default:
 *       {@code gpt-4o})</li>
 *   <li>{@code embedding-model}  — model used for embeddings (default:
 *       {@code text-embedding-3-small})</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(OpenAIConfig.OpenAIProperties.class)
public class OpenAIConfig {

    private static final Logger log = LogManager.getLogger(OpenAIConfig.class);

    /** Base URL for the OpenAI REST API v1. */
    public static final String OPENAI_BASE_URL = "https://api.openai.com/v1";

    private final OpenAIProperties properties;

    public OpenAIConfig(OpenAIProperties properties) {
        this.properties = properties;
    }

    // -------------------------------------------------------------------------
    // RestTemplate bean
    // -------------------------------------------------------------------------

    /**
     * Returns a {@link RestTemplate} pre-configured with the OpenAI API key
     * as a default {@code Authorization: Bearer} header.
     *
     * <p>All calls to the OpenAI REST API should use this bean so that
     * authentication is handled uniformly.
     *
     * @return configured RestTemplate for OpenAI calls
     */
    @Bean(name = "openAIRestTemplate")
    public RestTemplate openAIRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        ClientHttpRequestInterceptor authInterceptor = (request, body, execution) -> {
            request.getHeaders().set(HttpHeaders.AUTHORIZATION,
                    "Bearer " + properties.apiKey());
            request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            request.getHeaders().setAccept(List.of(MediaType.APPLICATION_JSON));
            return execution.execute(request, body);
        };

        restTemplate.setInterceptors(List.of(authInterceptor));

        log.info("OpenAI RestTemplate initialised: extractionModel={} chatModel={} embeddingModel={}",
                properties.extractionModel(), properties.chatModel(), properties.embeddingModel());

        return restTemplate;
    }

    // -------------------------------------------------------------------------
    // Properties accessor (package-accessible for injection into services)
    // -------------------------------------------------------------------------

    /**
     * Returns the bound OpenAI configuration properties.
     * Services that need model names or other settings can inject
     * {@link OpenAIProperties} directly via Spring's configuration-properties
     * mechanism.
     */
    public OpenAIProperties getProperties() {
        return properties;
    }

    // -------------------------------------------------------------------------
    // Properties record
    // -------------------------------------------------------------------------

    /**
     * Immutable configuration properties bound to the {@code app.openai} prefix.
     *
     * @param apiKey          OpenAI secret key
     * @param extractionModel model name for LLM extraction (e.g. {@code gpt-4o-mini})
     * @param chatModel       model name for the chat agent (e.g. {@code gpt-4o})
     * @param embeddingModel  model name for embeddings (e.g.
     *                        {@code text-embedding-3-small})
     */
    @ConfigurationProperties(prefix = "app.openai")
    public record OpenAIProperties(
            String apiKey,
            String extractionModel,
            String chatModel,
            String embeddingModel
    ) {
        public OpenAIProperties {
            // Use defaults if not set (should always be set via env vars in production)
            if (extractionModel == null || extractionModel.isBlank()) {
                extractionModel = "gpt-4o-mini";
            }
            if (chatModel == null || chatModel.isBlank()) {
                chatModel = "gpt-4o";
            }
            if (embeddingModel == null || embeddingModel.isBlank()) {
                embeddingModel = "text-embedding-3-small";
            }
        }
    }
}
