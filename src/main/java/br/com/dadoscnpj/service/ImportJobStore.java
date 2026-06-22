package br.com.dadoscnpj.service;

import br.com.dadoscnpj.dto.ImportJobStatus;
import br.com.dadoscnpj.model.ImportJob;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ImportJobStore {

    private final Map<String, ImportJob> jobs = new ConcurrentHashMap<>();

    public void salvar(ImportJob job) {
        jobs.put(job.getId(), job);
    }

    public Optional<ImportJob> buscar(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    public void remover(String jobId) {
        jobs.remove(jobId);
    }

    public int total() {
        return jobs.size();
    }

    public int contarAtivosPorIp(String clientIp) {
        return (int) jobs.values().stream()
                .filter(job -> clientIp.equals(job.getClientIp()))
                .filter(job -> job.getStatus() == ImportJobStatus.NA_FILA
                        || job.getStatus() == ImportJobStatus.PROCESSANDO)
                .count();
    }

    public List<String> removerExpirados(Duration ttl) {
        Instant limite = Instant.now().minus(ttl);
        List<String> removidos = new ArrayList<>();

        for (Map.Entry<String, ImportJob> entry : jobs.entrySet()) {
            ImportJob job = entry.getValue();
            Instant referencia = job.getConcluidoEm() != null ? job.getConcluidoEm() : job.getCriadoEm();
            boolean expirado = referencia.isBefore(limite);
            boolean ativo = job.getStatus() == ImportJobStatus.NA_FILA
                    || job.getStatus() == ImportJobStatus.PROCESSANDO;

            if (expirado && !ativo) {
                jobs.remove(entry.getKey());
                removidos.add(entry.getKey());
            }
        }

        return removidos;
    }
}
