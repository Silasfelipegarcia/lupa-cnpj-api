package br.com.dadoscnpj.plan;

public record PlanLimits(
        int maxRowsPerFile,
        Integer maxBatchSearchesPerDay,
        Integer maxDirectCnpjPerDay,
        boolean pesquisaRazaoSocial,
        boolean exportExcel,
        boolean filtroSomenteAtivos,
        boolean filtrosAvancados,
        boolean dedupeHabilitado
) {
    public boolean isUnlimitedBatch() {
        return maxBatchSearchesPerDay == null;
    }

    public boolean isUnlimitedDirect() {
        return maxDirectCnpjPerDay == null;
    }
}
