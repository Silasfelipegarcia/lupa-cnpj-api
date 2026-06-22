package br.com.dadoscnpj.dto;

import java.util.List;

public class CnpjPreviewResponse {

    private String cnpj;
    private String razaoSocial;
    private int consultasUsadas;
    private int consultasLimite;
    private int consultasRestantes;
    private List<String> camposComLogin;

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
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

    public List<String> getCamposComLogin() {
        return camposComLogin;
    }

    public void setCamposComLogin(List<String> camposComLogin) {
        this.camposComLogin = camposComLogin;
    }
}
