package br.com.dadoscnpj.util;

import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class IpRateLimiter {

    private record Window(long startMs, AtomicInteger count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, int maxRequests, long windowMs) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, current) -> {
            if (current == null || now - current.startMs >= windowMs) {
                return new Window(now, new AtomicInteger(0));
            }
            return current;
        });
        return window.count.incrementAndGet() <= maxRequests;
    }

    public void limparExpirados(long windowMs) {
        long cutoff = System.currentTimeMillis() - windowMs;
        Iterator<Map.Entry<String, Window>> iterator = windows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Window> entry = iterator.next();
            if (entry.getValue().startMs < cutoff) {
                iterator.remove();
            }
        }
    }
}
