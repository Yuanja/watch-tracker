package com.tradeintel.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async execution configuration for the WhatsApp Trade Intelligence Platform.
 *
 * <p>Defines a dedicated {@link ThreadPoolTaskExecutor} named
 * {@code processingExecutor} used by the message processing pipeline.
 * Services annotated with {@code @Async("processingExecutor")} (e.g.
 * {@code MessageProcessingService}) will run on this pool, isolating
 * background extraction work from the web request threads.
 *
 * <p>Pool sizing is controlled by {@code app.processing.async-pool-size} in
 * {@code application.yml}. The queue capacity is set to 500 to absorb short
 * bursts; once the queue is full, the caller thread runs the task
 * (CallerRunsPolicy) so that no messages are silently dropped.
 *
 * <p>Note: {@code @EnableAsync} is declared here and also on
 * {@link com.tradeintel.TradeintelApplication} (belt-and-suspenders). Spring
 * de-duplicates duplicate {@code @EnableAsync} declarations.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    private static final Logger log = LogManager.getLogger(AsyncConfig.class);

    @Value("${app.processing.async-pool-size:4}")
    private int asyncPoolSize;

    /**
     * Creates the shared executor used by the message processing pipeline.
     *
     * <p>Configuration summary:
     * <ul>
     *   <li><b>Core / max pool size</b> — both set to {@code asyncPoolSize} so the pool
     *       stays at a fixed size and does not grow under load (predictable resource
     *       usage on a single VPS).</li>
     *   <li><b>Queue capacity</b> — 500 tasks. Sufficient for typical WhatsApp group
     *       message bursts.</li>
     *   <li><b>Thread name prefix</b> — {@code processing-} for easy identification in
     *       thread dumps and Log4j2 output.</li>
     *   <li><b>Wait for tasks on shutdown</b> — {@code true} so that in-flight
     *       extractions complete gracefully when the application stops.</li>
     * </ul>
     *
     * @return the configured executor bean
     */
    @Bean(name = "processingExecutor")
    public Executor processingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncPoolSize);
        executor.setMaxPoolSize(asyncPoolSize);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("processing-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("Processing async executor initialised with pool size={}", asyncPoolSize);
        return executor;
    }
}
