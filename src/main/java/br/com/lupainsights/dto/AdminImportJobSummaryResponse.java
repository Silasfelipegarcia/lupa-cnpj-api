package br.com.lupainsights.dto;

import java.time.Instant;
import java.util.UUID;

public class AdminImportJobSummaryResponse {

    private UUID id;
    private String arquivo;
    private String status;
    private int total;
    private int processados;
    private int sucesso;
    private int erros;
    private Instant createdAt;
    private Instant completedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getArquivo() {
        return arquivo;
    }

    public void setArquivo(String arquivo) {
        this.arquivo = arquivo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getProcessados() {
        return processados;
    }

    public void setProcessados(int processados) {
        this.processados = processados;
    }

    public int getSucesso() {
        return sucesso;
    }

    public void setSucesso(int sucesso) {
        this.sucesso = sucesso;
    }

    public int getErros() {
        return erros;
    }

    public void setErros(int erros) {
        this.erros = erros;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
