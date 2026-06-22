package br.com.dadoscnpj.plan;

import br.com.dadoscnpj.entity.UserDailyUsageEntity;
import br.com.dadoscnpj.entity.UserEntity;
import br.com.dadoscnpj.repository.UserDailyUsageRepository;
import br.com.dadoscnpj.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class UsageTrackingService {

    private final UserDailyUsageRepository usageRepository;
    private final UserRepository userRepository;
    private final PlanLimitsService planLimitsService;

    public UsageTrackingService(UserDailyUsageRepository usageRepository,
                                UserRepository userRepository,
                                PlanLimitsService planLimitsService) {
        this.usageRepository = usageRepository;
        this.userRepository = userRepository;
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
                planLimitsService.isMaster(user)
        );
    }

    @Transactional
    public void validarEIncrementarBatch(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        if (planLimitsService.isMaster(user)) {
            incrementarBatch(userId);
            return;
        }

        PlanLimits limits = planLimitsService.limitesDe(user);
        UserDailyUsageEntity usage = obterOuCriar(userId, hoje());

        if (!limits.isUnlimitedBatch() && usage.getBatchSearches() >= limits.maxBatchSearchesPerDay()) {
            throw new IllegalStateException(String.format(
                    "Limite diário de %d consulta(s) em planilha do plano %s atingido. Faça upgrade ou tente amanhã.",
                    limits.maxBatchSearchesPerDay(),
                    planLimitsService.nomeExibicao(user.getPlan())));
        }

        incrementarBatch(userId);
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
                    "Limite diário de %d consulta(s) avulsas de CNPJ do plano %s atingido. Faça upgrade para consultas ilimitadas.",
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
                    "Seu plano %s permite até %d empresa(s) por planilha. Faça upgrade para enviar mais linhas.",
                    planLimitsService.nomeExibicao(user.getPlan()),
                    limits.maxRowsPerFile()));
        }
    }

    private void incrementarBatch(UUID userId) {
        UserDailyUsageEntity usage = obterOuCriar(userId, hoje());
        usage.setBatchSearches(usage.getBatchSearches() + 1);
        usageRepository.save(usage);
    }

    private void incrementarDirect(UUID userId) {
        UserDailyUsageEntity usage = obterOuCriar(userId, hoje());
        usage.setDirectCnpjLookups(usage.getDirectCnpjLookups() + 1);
        usageRepository.save(usage);
    }

    private UserDailyUsageEntity obterOuCriar(UUID userId, LocalDate date) {
        return usageRepository.findById(new br.com.dadoscnpj.entity.UserDailyUsageId(userId, date))
                .orElseGet(() -> UserDailyUsageEntity.zerado(userId, date));
    }

    private LocalDate hoje() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}
