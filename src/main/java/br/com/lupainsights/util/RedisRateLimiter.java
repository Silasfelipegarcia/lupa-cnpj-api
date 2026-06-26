package br.com.lupainsights.util;

import br.com.lupainsights.config.SecurityProperties;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RedisRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private static final String INCR_SCRIPT = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """;

    private final SecurityProperties securityProperties;
    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> connection;

    public RedisRateLimiter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public boolean isEnabled() {
        return securityProperties.isRedisEnabled();
    }

    public boolean tryAcquire(String key, int maxRequests, long windowMs) {
        if (!isEnabled()) {
            return true;
        }
        ensureConnected();
        if (connection == null) {
            return true;
        }
        try {
            RedisCommands<String, String> commands = connection.sync();
            Long count = commands.eval(INCR_SCRIPT, io.lettuce.core.ScriptOutputType.INTEGER,
                    new String[]{prefixKey(key)}, String.valueOf(windowMs));
            return count != null && count <= maxRequests;
        } catch (Exception ex) {
            log.warn("Redis rate limit indisponível, permitindo requisição: {}", ex.getMessage());
            return true;
        }
    }

    private synchronized void ensureConnected() {
        if (connection != null || !isEnabled()) {
            return;
        }
        try {
            redisClient = RedisClient.create(RedisURI.create(securityProperties.getRedisUrl()));
            connection = redisClient.connect();
            log.info("Redis rate limiter conectado");
        } catch (Exception ex) {
            log.warn("Falha ao conectar Redis rate limiter: {}", ex.getMessage());
        }
    }

    private String prefixKey(String key) {
        return "ratelimit:" + key;
    }

    @PreDestroy
    public void shutdown() {
        if (connection != null) {
            connection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }
}
