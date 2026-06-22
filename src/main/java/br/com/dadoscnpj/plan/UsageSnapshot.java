package br.com.dadoscnpj.plan;

public record UsageSnapshot(
        PlanLimits limits,
        int batchSearchesToday,
        int directCnpjToday,
        boolean master
) {
}
