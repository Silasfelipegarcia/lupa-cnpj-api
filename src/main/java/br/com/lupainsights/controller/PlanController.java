package br.com.lupainsights.controller;

import br.com.lupainsights.dto.PlanCatalogItemResponse;
import br.com.lupainsights.plan.PlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public ResponseEntity<List<PlanCatalogItemResponse>> catalogo() {
        return ResponseEntity.ok(planService.catalogo());
    }
}
