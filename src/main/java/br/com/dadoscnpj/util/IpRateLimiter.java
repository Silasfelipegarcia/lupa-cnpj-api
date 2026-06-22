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

    public int obterUso(String key, long windowMs) {
        long now = System.currentTimeMillis();
        Window window = windows.get(key);
        if (window == null || now - window.startMs >= windowMs) {
            return 0;
        }
        return window.count.get();
    }

    public boolean registrarUsoSeAbaixoDoLimite(String key, int maxRequests, long windowMs) {
        long now = System.currentTimeMillis();
        Window[] resultado = new Window[1];
        boolean[] permitido = new boolean[1];

        windows.compute(key, (k, current) -> {
            Window window = current;
            if (window == null || now - window.startMs >= windowMs) {
                window = new Window(now, new AtomicInteger(0));
            }
            if (window.count.get() >= maxRequests) {
                permitido[0] = false;
                resultado[0] = window;
                return window;
            }
            window.count.incrementAndGet();
            permitido[0] = true;
            resultado[0] = window;
            return window;
        });

        return permitido[0];
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
