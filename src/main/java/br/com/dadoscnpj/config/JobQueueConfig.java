package br.com.dadoscnpj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
public class JobQueueConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService importJobExecutor() {
        ThreadFactory factory = r -> {
            Thread thread = new Thread(r);
            thread.setName("cnpj-import-worker");
            thread.setDaemon(false);
            return thread;
        };
        return Executors.newSingleThreadExecutor(factory);
    }
}
