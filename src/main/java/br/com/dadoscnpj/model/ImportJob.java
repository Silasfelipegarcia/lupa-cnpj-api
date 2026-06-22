package br.com.dadoscnpj.model;

import br.com.dadoscnpj.dto.ImportJobStatus;
import br.com.dadoscnpj.dto.ImportRow;
import br.com.dadoscnpj.dto.CnpjResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ImportJob {

    private final String id;
    private final String arquivo;
    private final String clientIp;
    private final List<ImportRow> linhas;
    private final Instant criadoEm;
    private final List<CnpjResult> resultados = Collections.synchronizedList(new ArrayList<>());

    private ImportJobStatus status = ImportJobStatus.NA_FILA;
    private volatile boolean cancelamentoSolicitado;
    private int processados;
    private int sucesso;
    private int erros;
    private String mensagem = "Aguardando na fila de processamento";
    private byte[] resultado;
    private Instant concluidoEm;

    public ImportJob(String arquivo, List<ImportRow> linhas, String clientIp) {
        this.id = UUID.randomUUID().toString();
        this.arquivo = arquivo;
        this.clientIp = clientIp != null ? clientIp : "desconhecido";
        this.linhas = new ArrayList<>(linhas);
        this.criadoEm = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getArquivo() {
        return arquivo;
    }

    public String getClientIp() {
        return clientIp;
    }

    public List<ImportRow> getLinhas() {
        return linhas;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public ImportJobStatus getStatus() {
        return status;
    }

    public void setStatus(ImportJobStatus status) {
        this.status = status;
    }

    public void solicitarCancelamento() {
        this.cancelamentoSolicitado = true;
    }

    public boolean isCancelamentoSolicitado() {
        return cancelamentoSolicitado;
    }

    public int getTotal() {
        return linhas.size();
    }

    public int getProcessados() {
        return processados;
    }

    public void incrementarProcessados() {
        this.processados++;
    }

    public int getSucesso() {
        return sucesso;
    }

    public void incrementarSucesso() {
        this.sucesso++;
    }

    public int getErros() {
        return erros;
    }

    public void incrementarErros() {
        this.erros++;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public byte[] getResultado() {
        return resultado;
    }

    public void setResultado(byte[] resultado) {
        this.resultado = resultado;
    }

    public Instant getConcluidoEm() {
        return concluidoEm;
    }

    public void setConcluidoEm(Instant concluidoEm) {
        this.concluidoEm = concluidoEm;
    }

    public void adicionarResultado(CnpjResult resultado) {
        resultados.add(resultado);
    }

    public List<CnpjResult> getResultados() {
        synchronized (resultados) {
            return new ArrayList<>(resultados);
        }
    }

    public int getPercentual() {
        if (linhas.isEmpty()) {
            return 0;
        }
        return (int) Math.round((processados * 100.0) / linhas.size());
    }
}
