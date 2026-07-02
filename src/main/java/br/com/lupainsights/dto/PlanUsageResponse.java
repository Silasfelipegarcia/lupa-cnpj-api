package br.com.lupainsights.dto;

public class PlanUsageResponse {

    private int maxRowsPerFile;
    private Integer maxBatchSearchesPerDay;
    private Integer maxDirectCnpjPerDay;
    private int batchSearchesToday;
    private int directCnpjToday;
    private boolean master;
    private boolean pesquisaRazaoSocial;
    private boolean exportExcel;
    private boolean filtroSomenteAtivos;
    private boolean filtrosAvancados;
    private boolean dedupeHabilitado;
    private boolean trialDisponivel;
    private boolean emTrial;
    private int trialDiasRestantes;
    private boolean conversaoTrialPendente;
    private boolean dadosLimitados;
    private Integer maxImportJobsPerDay;
    private int importJobsToday;

    public int getMaxRowsPerFile() {
        return maxRowsPerFile;
    }

    public void setMaxRowsPerFile(int maxRowsPerFile) {
        this.maxRowsPerFile = maxRowsPerFile;
    }

    public Integer getMaxBatchSearchesPerDay() {
        return maxBatchSearchesPerDay;
    }

    public void setMaxBatchSearchesPerDay(Integer maxBatchSearchesPerDay) {
        this.maxBatchSearchesPerDay = maxBatchSearchesPerDay;
    }

    public Integer getMaxDirectCnpjPerDay() {
        return maxDirectCnpjPerDay;
    }

    public void setMaxDirectCnpjPerDay(Integer maxDirectCnpjPerDay) {
        this.maxDirectCnpjPerDay = maxDirectCnpjPerDay;
    }

    public int getBatchSearchesToday() {
        return batchSearchesToday;
    }

    public void setBatchSearchesToday(int batchSearchesToday) {
        this.batchSearchesToday = batchSearchesToday;
    }

    public int getDirectCnpjToday() {
        return directCnpjToday;
    }

    public void setDirectCnpjToday(int directCnpjToday) {
        this.directCnpjToday = directCnpjToday;
    }

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
    }

    public boolean isPesquisaRazaoSocial() {
        return pesquisaRazaoSocial;
    }

    public void setPesquisaRazaoSocial(boolean pesquisaRazaoSocial) {
        this.pesquisaRazaoSocial = pesquisaRazaoSocial;
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

    public boolean isEmTrial() {
        return emTrial;
    }

    public void setEmTrial(boolean emTrial) {
        this.emTrial = emTrial;
    }

    public int getTrialDiasRestantes() {
        return trialDiasRestantes;
    }

    public void setTrialDiasRestantes(int trialDiasRestantes) {
        this.trialDiasRestantes = trialDiasRestantes;
    }

    public boolean isConversaoTrialPendente() {
        return conversaoTrialPendente;
    }

    public void setConversaoTrialPendente(boolean conversaoTrialPendente) {
        this.conversaoTrialPendente = conversaoTrialPendente;
    }

    public boolean isDadosLimitados() {
        return dadosLimitados;
    }

    public void setDadosLimitados(boolean dadosLimitados) {
        this.dadosLimitados = dadosLimitados;
    }

    public Integer getMaxImportJobsPerDay() {
        return maxImportJobsPerDay;
    }

    public void setMaxImportJobsPerDay(Integer maxImportJobsPerDay) {
        this.maxImportJobsPerDay = maxImportJobsPerDay;
    }

    public int getImportJobsToday() {
        return importJobsToday;
    }

    public void setImportJobsToday(int importJobsToday) {
        this.importJobsToday = importJobsToday;
    }
}
