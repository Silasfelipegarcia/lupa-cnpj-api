package br.com.lupainsights.audit;

import br.com.lupainsights.entity.AuditLogEntity;
import br.com.lupainsights.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditLogService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(AuditAction action,
                          String method,
                          String path,
                          String ip,
                          int statusCode,
                          UUID userId,
                          String details,
                          int durationMs) {
        String userLabel = userId != null ? userId.toString() : "anonymous";
        AUDIT.info("action={} method={} path={} status={} user={} ip={} durationMs={} details={}",
                action, method, path, statusCode, userLabel, ip, durationMs,
                details != null ? details : "");

        AuditLogEntity entry = new AuditLogEntity();
        entry.setOccurredAt(Instant.now());
        entry.setUserId(userId);
        entry.setAction(action.name());
        entry.setHttpMethod(method);
        entry.setPath(truncar(path, 500));
        entry.setIp(truncar(ip, 45));
        entry.setStatusCode(statusCode);
        entry.setDetails(truncar(details, 1000));
        entry.setDurationMs(durationMs);
        repository.save(entry);
    }

    private static String truncar(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
