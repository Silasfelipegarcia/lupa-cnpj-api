package br.com.dadoscnpj.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "import_jobs")
public class ImportJobEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String arquivo;

    @Column(nullable = false, length = 20)
    private String status;

    private int total;
    private int processados;
    private int sucesso;
    private int erros;

    @Column(columnDefinition = "TEXT")
    private String mensagem;

    @Column(name = "linhas_json", nullable = false, columnDefinition = "TEXT")
    private String linhasJson;

    @Column(name = "resultado_csv")
    private byte[] resultadoCsv;

    @Column(name = "cancelamento_solicitado", nullable = false)
    private boolean cancelamentoSolicitado;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "nome_lista")
    private String nomeLista;

    @Column(name = "lista_salva", nullable = false)
    private boolean listaSalva = false;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getLinhasJson() {
        return linhasJson;
    }

    public void setLinhasJson(String linhasJson) {
        this.linhasJson = linhasJson;
    }

    public byte[] getResultadoCsv() {
        return resultadoCsv;
    }

    public void setResultadoCsv(byte[] resultadoCsv) {
        this.resultadoCsv = resultadoCsv;
    }

    public boolean isCancelamentoSolicitado() {
        return cancelamentoSolicitado;
    }

    public void setCancelamentoSolicitado(boolean cancelamentoSolicitado) {
        this.cancelamentoSolicitado = cancelamentoSolicitado;
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

    public String getNomeLista() {
        return nomeLista;
    }

    public void setNomeLista(String nomeLista) {
        this.nomeLista = nomeLista;
    }

    public boolean isListaSalva() {
        return listaSalva;
    }

    public void setListaSalva(boolean listaSalva) {
        this.listaSalva = listaSalva;
    }
}
