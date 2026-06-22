package br.com.dadoscnpj.dto;

import java.util.ArrayList;
import java.util.List;

public class CnpjConfigResponse {

    private boolean pesquisaRazaoSocialHabilitada;
    private boolean exportExcel;
    private boolean filtroSomenteAtivos;
    private boolean filtrosAvancados;
    private boolean dedupeHabilitado;
    private boolean trialDisponivel;

    public CnpjConfigResponse() {
    }

    public boolean isPesquisaRazaoSocialHabilitada() {
        return pesquisaRazaoSocialHabilitada;
    }

    public void setPesquisaRazaoSocialHabilitada(boolean pesquisaRazaoSocialHabilitada) {
        this.pesquisaRazaoSocialHabilitada = pesquisaRazaoSocialHabilitada;
    }

    public boolean isExportExcel() {
        return exportExcel;
    }

    public void setExportExcel(boolean exportExcel) {
        this.exportExcel = exportExcel;
    }

    public boolean isFiltroSomenteAtivos() {
        return filtroSomenteAtivos;
    }

    public void setFiltroSomenteAtivos(boolean filtroSomenteAtivos) {
        this.filtroSomenteAtivos = filtroSomenteAtivos;
    }

    public boolean isFiltrosAvancados() {
        return filtrosAvancados;
    }

    public void setFiltrosAvancados(boolean filtrosAvancados) {
        this.filtrosAvancados = filtrosAvancados;
    }

    public boolean isDedupeHabilitado() {
        return dedupeHabilitado;
    }

    public void setDedupeHabilitado(boolean dedupeHabilitado) {
        this.dedupeHabilitado = dedupeHabilitado;
    }

    public boolean isTrialDisponivel() {
        return trialDisponivel;
    }

    public void setTrialDisponivel(boolean trialDisponivel) {
        this.trialDisponivel = trialDisponivel;
    }
}
