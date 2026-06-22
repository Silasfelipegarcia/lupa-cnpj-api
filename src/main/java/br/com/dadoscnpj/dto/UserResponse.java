package br.com.dadoscnpj.dto;

import br.com.dadoscnpj.domain.SubscriptionPlan;
import br.com.dadoscnpj.domain.UserRole;

import java.util.UUID;

public class UserResponse {

    private UUID id;
    private String nome;
    private String email;
    private String cpf;
    private UserRole role;
    private SubscriptionPlan plan;
    private String planNome;
    private PlanUsageResponse usage;

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
}
