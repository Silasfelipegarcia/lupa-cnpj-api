package br.com.lupainsights;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LupaInsightsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LupaInsightsApplication.class, args);
    }
}
