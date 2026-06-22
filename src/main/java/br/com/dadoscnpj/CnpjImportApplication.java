package br.com.dadoscnpj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CnpjImportApplication {

    public static void main(String[] args) {
        SpringApplication.run(CnpjImportApplication.class, args);
    }
}
