package br.com.lupainsights.dto;

import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.domain.UserRole;
import br.com.lupainsights.dto.SubscriptionStatusResponse;

import java.time.Instant;
import java.util.UUID;

public class UserResponse {

    private UUID id;
    private String nome;
    private String email;
    private String cpf;
    private Instant createdAt;
    private UserRole role;
    private SubscriptionPlan plan;
    private String planNome;
    private PlanUsageResponse usage;
    private SubscriptionStatusResponse subscription;

    public UserResponse() {
    }

    public UserResponse(UUID id, String nome, String email, String cpf) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.cpf = cpf;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }

    public String getPlanNome() {
        return planNome;
    }

    public void setPlanNome(String planNome) {
        this.planNome = planNome;
    }

    public PlanUsageResponse getUsage() {
        return usage;
    }

    public void setUsage(PlanUsageResponse usage) {
        this.usage = usage;
    }

    public SubscriptionStatusResponse getSubscription() {
        return subscription;
    }

    public void setSubscription(SubscriptionStatusResponse subscription) {
        this.subscription = subscription;
    }
}
