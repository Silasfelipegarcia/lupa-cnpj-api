package br.com.lupainsights.plan;

public record UsageSnapshot(
        PlanLimits limits,
        int batchSearchesToday,
        int directCnpjToday,
        boolean master
) {
}
