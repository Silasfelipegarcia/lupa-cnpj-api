package br.com.dadoscnpj.service;

import br.com.dadoscnpj.model.ImportJob;
import org.springframework.stereotype.Component;

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
}
