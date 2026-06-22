package br.com.dadoscnpj.dto;

public class CnpjPreviewQuotaResponse {

    private int consultasUsadas;
    private int consultasLimite;
    private int consultasRestantes;
    private boolean limiteAtingido;

    public CnpjPreviewQuotaResponse() {
    }

    public CnpjPreviewQuotaResponse(int consultasUsadas, int consultasLimite) {
        this.consultasUsadas = consultasUsadas;
        this.consultasLimite = consultasLimite;
        this.consultasRestantes = Math.max(0, consultasLimite - consultasUsadas);
        this.limiteAtingido = consultasUsadas >= consultasLimite;
    }

    public int getConsultasUsadas() {
        return consultasUsadas;
    }

    public void setConsultasUsadas(int consultasUsadas) {
        this.consultasUsadas = consultasUsadas;
    }

    public int getConsultasLimite() {
        return consultasLimite;
    }

    public void setConsultasLimite(int consultasLimite) {
        this.consultasLimite = consultasLimite;
    }

    public int getConsultasRestantes() {
        return consultasRestantes;
    }

    public void setConsultasRestantes(int consultasRestantes) {
        this.consultasRestantes = consultasRestantes;
    }

    public boolean isLimiteAtingido() {
        return limiteAtingido;
    }

    public void setLimiteAtingido(boolean limiteAtingido) {
        this.limiteAtingido = limiteAtingido;
    }
}
