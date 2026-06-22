package br.com.dadoscnpj.dto;

public class CnpjConfigResponse {

    private boolean pesquisaRazaoSocialHabilitada;

    public CnpjConfigResponse() {
    }

    public CnpjConfigResponse(boolean pesquisaRazaoSocialHabilitada) {
        this.pesquisaRazaoSocialHabilitada = pesquisaRazaoSocialHabilitada;
    }

    public boolean isPesquisaRazaoSocialHabilitada() {
        return pesquisaRazaoSocialHabilitada;
    }

    public void setPesquisaRazaoSocialHabilitada(boolean pesquisaRazaoSocialHabilitada) {
        this.pesquisaRazaoSocialHabilitada = pesquisaRazaoSocialHabilitada;
    }
}
