package br.com.lupainsights.dto;

import br.com.lupainsights.entity.ImportJobEntity;

import java.time.Instant;
import java.util.UUID;

public class ImportJobSummaryResponse {

    private UUID jobId;
    private String status;
    private String arquivo;
    private int total;
    private int processados;
    private int sucesso;
    private int erros;
    private int percentual;
    private String mensagem;
    private Instant createdAt;
    private Instant completedAt;

    public static ImportJobSummaryResponse from(ImportJobEntity entity) {
        ImportJobSummaryResponse response = new ImportJobSummaryResponse();
        response.setJobId(entity.getId());
        response.setStatus(entity.getStatus());
        response.setArquivo(entity.getArquivo());
        response.setTotal(entity.getTotal());
        response.setProcessados(entity.getProcessados());
        response.setSucesso(entity.getSucesso());
        response.setErros(entity.getErros());
        response.setPercentual(entity.getTotal() == 0
                ? 0
                : (int) Math.round((entity.getProcessados() * 100.0) / entity.getTotal()));
        response.setMensagem(entity.getMensagem());
        response.setCreatedAt(entity.getCreatedAt());
        response.setCompletedAt(entity.getCompletedAt());
        return response;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getArquivo() {
        return arquivo;
    }

    public void setArquivo(String arquivo) {
        this.arquivo = arquivo;
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

    public int getPercentual() {
        return percentual;
    }

    public void setPercentual(int percentual) {
        this.percentual = percentual;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
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
