package br.com.lupainsights.plan;

public record PlanLimits(
        int maxRowsPerFile,
        Integer maxBatchSearchesPerDay,
        Integer maxDirectCnpjPerDay,
        boolean pesquisaRazaoSocial,
        boolean exportExcel,
        boolean filtroSomenteAtivos,
        boolean filtrosAvancados,
        boolean dedupeHabilitado,
        boolean dadosLimitados,
        Integer maxImportJobsPerDay
) {
    public boolean isUnlimitedBatch() {
        return maxBatchSearchesPerDay == null;
    }

    public boolean isUnlimitedDirect() {
        return maxDirectCnpjPerDay == null;
    }
}
