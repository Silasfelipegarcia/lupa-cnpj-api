package br.com.lupainsights.service;

import br.com.lupainsights.domain.SubscriptionPlan;
import br.com.lupainsights.domain.UserRole;
import br.com.lupainsights.dto.AdminDailyUsageResponse;
import br.com.lupainsights.dto.AdminImportJobSummaryResponse;
import br.com.lupainsights.dto.AdminOverviewResponse;
import br.com.lupainsights.dto.AdminPaymentSummaryResponse;
import br.com.lupainsights.dto.AdminPlanCountResponse;
import br.com.lupainsights.dto.AdminUsageSnapshotResponse;
import br.com.lupainsights.dto.AdminUserDetailResponse;
import br.com.lupainsights.dto.AdminUserSummaryResponse;
import br.com.lupainsights.dto.AdminUsersPageResponse;
import br.com.lupainsights.entity.ImportJobEntity;
import br.com.lupainsights.entity.PaymentOrderEntity;
import br.com.lupainsights.entity.UserDailyUsageEntity;
import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.exception.ForbiddenException;
import br.com.lupainsights.repository.ImportJobRepository;
import br.com.lupainsights.repository.PaymentOrderRepository;
import br.com.lupainsights.repository.UserDailyUsageRepository;
import br.com.lupainsights.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminMetricsService {

    private static final ZoneId BRAZIL = ZoneId.of("America/Sao_Paulo");
    private static final int DETAIL_USAGE_DAYS = 30;

    private final UserRepository userRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final ImportJobRepository importJobRepository;
    private final UserDailyUsageRepository userDailyUsageRepository;

    public AdminMetricsService(UserRepository userRepository,
                               PaymentOrderRepository paymentOrderRepository,
                               ImportJobRepository importJobRepository,
                               UserDailyUsageRepository userDailyUsageRepository) {
        this.userRepository = userRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.importJobRepository = importJobRepository;
        this.userDailyUsageRepository = userDailyUsageRepository;
    }

    @Transactional(readOnly = true)
    public AdminOverviewResponse overview(int days) {
        int periodDays = clampDays(days);
        Instant since = Instant.now().minus(periodDays, ChronoUnit.DAYS);
        LocalDate sinceDate = LocalDate.now(BRAZIL).minusDays(periodDays);
        LocalDate today = LocalDate.now(BRAZIL);
        Instant now = Instant.now();

        AdminOverviewResponse response = new AdminOverviewResponse();
        response.setPeriodDays(periodDays);
        response.setTotalUsers(userRepository.countExcludingRole(UserRole.ADMIN));
        response.setNewUsersInPeriod(userRepository.countCreatedSinceExcludingRole(UserRole.ADMIN, since));
        response.setUsersByPlan(buildPlanCounts());
        response.setActiveTrials(userRepository.countActiveTrialsExcludingRole(UserRole.ADMIN, now));
        response.setActivePaidSubscriptions(
                userRepository.countActivePaidSubscriptionsExcludingRole(UserRole.ADMIN, now));
        response.setTotalRevenueCents(paymentOrderRepository.sumApprovedAmountAllTime());
        response.setRevenueInPeriodCents(paymentOrderRepository.sumApprovedAmountSince(since));
        response.setApprovedPaymentsInPeriod(paymentOrderRepository.countApprovedSince(since));
        response.setPendingPayments(paymentOrderRepository.countPendingPayments());
        response.setTotalImportJobs(importJobRepository.countAllJobs());
        response.setActiveImportJobs(importJobRepository.countAtivos());
        response.setTotalRowsProcessed(importJobRepository.sumProcessadosAll());
        response.setTotalRowsSuccess(importJobRepository.sumSucessoAll());
        response.setTotalRowsErrors(importJobRepository.sumErrosAll());
        response.setUsageInPeriod(toUsageSnapshot(userDailyUsageRepository.sumUsageSince(sinceDate)));
        response.setUsageToday(toUsageSnapshot(userDailyUsageRepository.sumUsageOnDate(today)));
        return response;
    }

    @Transactional(readOnly = true)
    public AdminUsersPageResponse listUsers(int page, int size, SubscriptionPlan plan, String q) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String query = q != null ? q.trim() : null;
        if (query != null && query.isEmpty()) {
            query = null;
        }

        Page<UserEntity> users = userRepository.searchForAdmin(plan, query, PageRequest.of(safePage, safeSize));
        List<UUID> userIds = users.getContent().stream().map(UserEntity::getId).toList();

        Map<UUID, long[]> importMetrics = loadImportMetrics(userIds);
        Map<UUID, Long> revenueByUser = loadRevenueByUser(userIds);
        Map<UUID, AdminUsageSnapshotResponse> usageToday = loadUsageToday(userIds);

        List<AdminUserSummaryResponse> content = users.getContent().stream()
                .map(user -> toUserSummary(user, importMetrics, revenueByUser, usageToday))
                .toList();

        AdminUsersPageResponse response = new AdminUsersPageResponse();
        response.setContent(content);
        response.setPage(users.getNumber());
        response.setSize(users.getSize());
        response.setTotalElements(users.getTotalElements());
        response.setTotalPages(users.getTotalPages());
        return response;
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse userDetail(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException("Usuário não encontrado."));

        LocalDate sinceDate = LocalDate.now(BRAZIL).minusDays(DETAIL_USAGE_DAYS);

        AdminUserDetailResponse response = new AdminUserDetailResponse();
        response.setId(user.getId());
        response.setNome(user.getNome());
        response.setEmail(user.getEmail());
        response.setPlan(user.getPlan());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        response.setEnabled(user.isEnabled());
        response.setTrialUtilizado(user.isTrialUtilizado());
        response.setTrialAte(user.getTrialAte());
        response.setPlanValidUntil(user.getPlanValidUntil());
        response.setPlanCancelledAt(user.getPlanCancelledAt());
        response.setAutoRenew(user.isAutoRenew());
        response.setImportJobsCount(importJobRepository.countByUserId(userId));
        response.setRowsProcessed(importJobRepository.sumProcessadosByUserId(userId));
        response.setRowsSuccess(importJobRepository.sumSucessoByUserId(userId));
        response.setRowsErrors(importJobRepository.sumErrosByUserId(userId));
        response.setRevenueCents(paymentOrderRepository.sumApprovedAmountByUserId(userId));
        response.setUsageLifetime(toUsageSnapshot(userDailyUsageRepository.sumUsageByUserId(userId)));
        response.setUsageLast30Days(toUsageSnapshot(userDailyUsageRepository.sumUsageByUserIdSince(userId, sinceDate)));
        response.setRecentImports(loadRecentImports(userId));
        response.setPayments(loadPayments(userId));
        response.setDailyUsage(loadDailyUsage(userId, sinceDate));
        return response;
    }

    private List<AdminPlanCountResponse> buildPlanCounts() {
        Map<SubscriptionPlan, Long> counts = new EnumMap<>(SubscriptionPlan.class);
        for (SubscriptionPlan plan : SubscriptionPlan.values()) {
            counts.put(plan, 0L);
        }
        for (Object[] row : userRepository.countByPlanExcludingRole(UserRole.ADMIN)) {
            counts.put((SubscriptionPlan) row[0], (Long) row[1]);
        }
        return counts.entrySet().stream()
                .map(entry -> new AdminPlanCountResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<UUID, long[]> loadImportMetrics(List<UUID> userIds) {
        Map<UUID, long[]> result = new HashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }
        for (Object[] row : importJobRepository.metricsByUserIds(userIds)) {
            UUID userId = (UUID) row[0];
            long jobCount = (Long) row[1];
            long rowsProcessed = (Long) row[2];
            result.put(userId, new long[]{jobCount, rowsProcessed});
        }
        return result;
    }

    private Map<UUID, Long> loadRevenueByUser(List<UUID> userIds) {
        Map<UUID, Long> result = new HashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }
        for (Object[] row : paymentOrderRepository.sumApprovedAmountByUserIds(userIds)) {
            result.put((UUID) row[0], (Long) row[1]);
        }
        return result;
    }

    private Map<UUID, AdminUsageSnapshotResponse> loadUsageToday(List<UUID> userIds) {
        Map<UUID, AdminUsageSnapshotResponse> result = new HashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }
        LocalDate today = LocalDate.now(BRAZIL);
        for (UserDailyUsageEntity usage : userDailyUsageRepository.findByUserIdsAndDate(userIds, today)) {
            result.put(usage.getUserId(), new AdminUsageSnapshotResponse(
                    usage.getBatchSearches(), usage.getDirectCnpjLookups()));
        }
        return result;
    }

    private AdminUserSummaryResponse toUserSummary(UserEntity user,
                                                   Map<UUID, long[]> importMetrics,
                                                   Map<UUID, Long> revenueByUser,
                                                   Map<UUID, AdminUsageSnapshotResponse> usageToday) {
        long[] metrics = importMetrics.getOrDefault(user.getId(), new long[]{0L, 0L});
        AdminUserSummaryResponse summary = new AdminUserSummaryResponse();
        summary.setId(user.getId());
        summary.setNome(user.getNome());
        summary.setEmail(user.getEmail());
        summary.setPlan(user.getPlan());
        summary.setRole(user.getRole());
        summary.setCreatedAt(user.getCreatedAt());
        summary.setEnabled(user.isEnabled());
        summary.setImportJobsCount(metrics[0]);
        summary.setRowsProcessed(metrics[1]);
        summary.setRevenueCents(revenueByUser.getOrDefault(user.getId(), 0L));
        summary.setUsageToday(usageToday.getOrDefault(user.getId(), new AdminUsageSnapshotResponse(0, 0)));
        return summary;
    }

    private List<AdminImportJobSummaryResponse> loadRecentImports(UUID userId) {
        return importJobRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10))
                .stream()
                .map(this::toImportSummary)
                .toList();
    }

    private List<AdminPaymentSummaryResponse> loadPayments(UUID userId) {
        return paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toPaymentSummary)
                .toList();
    }

    private List<AdminDailyUsageResponse> loadDailyUsage(UUID userId, LocalDate since) {
        return userDailyUsageRepository.findByUserIdSince(userId, since).stream()
                .map(usage -> new AdminDailyUsageResponse(
                        usage.getUsageDate(),
                        usage.getBatchSearches(),
                        usage.getDirectCnpjLookups()))
                .toList();
    }

    private AdminImportJobSummaryResponse toImportSummary(ImportJobEntity job) {
        AdminImportJobSummaryResponse item = new AdminImportJobSummaryResponse();
        item.setId(job.getId());
        item.setArquivo(job.getArquivo());
        item.setStatus(job.getStatus());
        item.setTotal(job.getTotal());
        item.setProcessados(job.getProcessados());
        item.setSucesso(job.getSucesso());
        item.setErros(job.getErros());
        item.setCreatedAt(job.getCreatedAt());
        item.setCompletedAt(job.getCompletedAt());
        return item;
    }

    private AdminPaymentSummaryResponse toPaymentSummary(PaymentOrderEntity payment) {
        AdminPaymentSummaryResponse item = new AdminPaymentSummaryResponse();
        item.setId(payment.getId());
        item.setPlan(payment.getPlan());
        item.setStatus(payment.getStatus());
        item.setAmountCents(payment.getAmountCents());
        item.setCreatedAt(payment.getCreatedAt());
        item.setPaidAt(payment.getPaidAt());
        item.setRenewal(payment.isRenewal());
        return item;
    }

    private AdminUsageSnapshotResponse toUsageSnapshot(Object[] row) {
        if (row == null || row.length < 2) {
            return new AdminUsageSnapshotResponse(0, 0);
        }
        long batch = row[0] instanceof Number number ? number.longValue() : 0L;
        long direct = row[1] instanceof Number number ? number.longValue() : 0L;
        return new AdminUsageSnapshotResponse(batch, direct);
    }

    private int clampDays(int days) {
        if (days <= 7) {
            return 7;
        }
        if (days <= 30) {
            return 30;
        }
        return 90;
    }
}
