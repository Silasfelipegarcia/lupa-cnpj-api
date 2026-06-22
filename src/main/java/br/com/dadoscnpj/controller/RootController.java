package br.com.dadoscnpj.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> raiz() {
        return ResponseEntity.ok(Map.of(
                "service", "LupaCNPJ API",
                "status", "ok",
                "docs", Map.of(
                        "health", "/actuator/health",
                        "config", "/cnpj/config",
                        "import", "POST /cnpj/import"
                )
        ));
    }
}
