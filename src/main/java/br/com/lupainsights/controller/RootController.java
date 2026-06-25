package br.com.lupainsights.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> raiz() {
        return ResponseEntity.ok(Map.of(
                "service", "Lupa Insights API",
                "status", "ok",
                "docs", Map.of(
                        "health", "/health",
                        "config", "/cnpj/config",
                        "import", "POST /cnpj/import"
                )
        ));
    }
}
