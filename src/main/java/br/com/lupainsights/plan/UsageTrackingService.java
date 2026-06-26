package br.com.lupainsights.plan;

import br.com.lupainsights.entity.UserDailyUsageEntity;
import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.repository.UserDailyUsageRepository;
import br.com.lupainsights.repository.ImportJobRepository;
import br.com.lupainsights.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class UsageTrackingService {

    private final UserDailyUsageRepository usageRepository;
    private final UserRepository userRepository;
    private final ImportJobRepository importJobRepository;
    private final PlanLimitsService planLimitsService;

    public UsageTrackingService(UserDailyUsageRepository usageRepository,
                                UserRepository userRepository,
                                ImportJobRepository importJobRepository,
                                PlanLimitsService planLimitsService) {
        this.usageRepository = usageRepository;
        this.userRepository = userRepository;
        this.importJobRepository = importJobRepository;
        this.planLimitsService = planLimitsService;
    }

    public UsageSnapshot snapshot(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        PlanLimits limits = planLimitsService.limitesDe(user);
        UserDailyUsageEntity usage = obterOuCriar(userId, hoje());

        return new UsageSnapshot(
                limits,
                usage.getBatchSearches(),
                usage.getDirectCnpjLookups(),
                planLimitsService.isMaster(user),
                contarImportacoesHoje(userId)
        );
    }

    public int contarImportacoesHoje(UUID userId) {
        Instant inicioDia = hoje().atStartOfDay(ZoneOffset.UTC).toInstant();
        return (int) importJobRepository.countByUserIdSince(userId, inicioDia);
    }

    public void validarImportacaoDiaria(UserEntity user) {
        if (planLimitsService.isMaster(user)) {
            return;
        }
        PlanLimits limits = planLimitsService.limitesDe(user);
        if (limits.maxImportJobsPerDay() == null) {
            return;
        }
        int usadas = contarImportacoesHoje(user.getId());
        if (usadas >= limits.maxImportJobsPerDay()) {
            throw new IllegalStateException(String.format(
                    "Seu plano %s permite %d importação(ões) por dia. Faça upgrade para importar mais planilhas.",
                    planLimitsService.nomeExibicao(user.getPlan()),
                    limits.maxImportJobsPerDay()));
        }
    }

    @Transactional
    public void validarEIncrementarBatch(UUID userId, int linhasNoArquivo) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        if (planLimitsService.isMaster(user)) {
            incrementarBatch(userId, linhasNoArquivo);
            return;
        }

        PlanLimits limits = planLimitsService.limitesDe(user);
        UserDailyUsageEntity usage = obterOuCriar(userId, hoje());

        if (!limits.isUnlimitedBatch()) {
            int limiteDiario = limits.maxBatchSearchesPerDay();
            int usadas = usage.getBatchSearches();
            if (usadas + linhasNoArquivo > limiteDiario) {
                int restantes = Math.max(0, limiteDiario - usadas);
                throw new IllegalStateException(String.format(
                        "Seu plano %s permite até %d empresa(s) em planilha por dia. "
                                + "Você já consultou %d hoje — restam %d.",
                        planLimitsService.nomeExibicao(user.getPlan()),
                        limiteDiario, usadas, restantes));
            }
        }

        incrementarBatch(userId, linhasNoArquivo);
    }

    @Transactional
    public void validarEIncrementarDirectCnpj(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        if (planLimitsService.isMaster(user)) {
            incrementarDirect(userId);
            return;
        }

        PlanLimits limits = planLimitsService.limitesDe(user);
        if (limits.isUnlimitedDirect()) {
            incrementarDirect(userId);
            return;
        }

        UserDailyUsageEntity usage = obterOuCriar(userId, hoje());
        if (usage.getDirectCnpjLookups() >= limits.maxDirectCnpjPerDay()) {
            throw new IllegalStateException(String.format(
                    "Limite diário de %d CNPJ(s) únicos do plano %s atingido. Faça upgrade para continuar.",
                    limits.maxDirectCnpjPerDay(),
                    planLimitsService.nomeExibicao(user.getPlan())));
        }

        incrementarDirect(userId);
    }

    public void validarLinhasPorPlano(UserEntity user, int linhas) {
        if (planLimitsService.isMaster(user)) {
            return;
        }
        PlanLimits limits = planLimitsService.limitesDe(user);
        if (linhas > limits.maxRowsPerFile()) {
            throw new IllegalArgumentException(String.format(
                    "Seu plano %s permite até %d empresa(s) por arquivo. Faça upgrade para enviar mais linhas.",
                    planLimitsService.nomeExibicao(user.getPlan()),
                    limits.maxRowsPerFile()));
        }

        if (!limits.isUnlimitedBatch()) {
            UserDailyUsageEntity usage = obterOuCriar(user.getId(), hoje());
            int limiteDiario = limits.maxBatchSearchesPerDay();
            int restantes = limiteDiario - usage.getBatchSearches();
            if (linhas > restantes) {
                throw new IllegalArgumentException(String.format(
                        "Seu plano %s permite até %d empresa(s) em planilha por dia. "
                                + "Você já consultou %d hoje — restam %d para importar.",
                        planLimitsService.nomeExibicao(user.getPlan()),
                        limiteDiario, usage.getBatchSearches(), Math.max(0, restantes)));
            }
        }
    }

    private void incrementarBatch(UUID userId, int linhasNoArquivo) {
        UserDailyUsageEntity usage = obterOuCriar(userId, hoje());
        usage.setBatchSearches(usage.getBatchSearches() + linhasNoArquivo);
        usageRepository.save(usage);
    }

    private void incrementarDirect(UUID userId) {
        UserDailyUsageEntity usage = obterOuCriar(userId, hoje());
        usage.setDirectCnpjLookups(usage.getDirectCnpjLookups() + 1);
        usageRepository.save(usage);
    }

    private UserDailyUsageEntity obterOuCriar(UUID userId, LocalDate date) {
        return usageRepository.findById(new br.com.lupainsights.entity.UserDailyUsageId(userId, date))
                .orElseGet(() -> UserDailyUsageEntity.zerado(userId, date));
    }

    private LocalDate hoje() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}
