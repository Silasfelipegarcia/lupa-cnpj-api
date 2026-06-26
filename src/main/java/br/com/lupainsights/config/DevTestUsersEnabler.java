package br.com.lupainsights.config;

import br.com.lupainsights.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * V11 desativa contas de teste com senha conhecida (correto em produção).
 * Em dev/local, reabilita automaticamente para não quebrar o fluxo de testes.
 */
@Component
public class DevTestUsersEnabler {

    private static final Logger log = LoggerFactory.getLogger(DevTestUsersEnabler.class);

    private static final List<String> TEST_SEED_EMAILS = List.of(
            "teste.free@lupainsights.com.br",
            "teste.premium@lupainsights.com.br",
            "teste.proplus@lupainsights.com.br",
            "admin@lupainsights.com.br"
    );

    private final Environment environment;
    private final UserRepository userRepository;

    public DevTestUsersEnabler(Environment environment, UserRepository userRepository) {
        this.environment = environment;
        this.userRepository = userRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void reabilitarContasTesteEmDev() {
        if (isProduction()) {
            return;
        }

        for (String email : TEST_SEED_EMAILS) {
            userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                if (!user.isEnabled()) {
                    user.setEnabled(true);
                    userRepository.save(user);
                    log.info("Dev/local: conta de teste reabilitada ({})", email);
                }
            });
        }
    }

    private boolean isProduction() {
        return Arrays.asList(environment.getActiveProfiles()).contains("production");
    }
}
