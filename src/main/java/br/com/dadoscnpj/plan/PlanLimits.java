package br.com.dadoscnpj.plan;

public record PlanLimits(
        int maxRowsPerFile,
        Integer maxBatchSearchesPerDay,
        Integer maxDirectCnpjPerDay
) {
    public boolean isUnlimitedBatch() {
        return maxBatchSearchesPerDay == null;
    }

    public boolean isUnlimitedDirect() {
        return maxDirectCnpjPerDay == null;
    }
}
