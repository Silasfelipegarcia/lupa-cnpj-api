package br.com.dadoscnpj.service;

import br.com.dadoscnpj.dto.CnpjResult;
import br.com.dadoscnpj.dto.ImportJobStatus;
import br.com.dadoscnpj.entity.ImportJobEntity;
import br.com.dadoscnpj.entity.ImportResultEntity;
import br.com.dadoscnpj.model.ImportJob;
import br.com.dadoscnpj.repository.ImportJobRepository;
import br.com.dadoscnpj.repository.ImportResultRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ImportJobStore {

    private final ImportJobRepository jobRepository;
    private final ImportResultRepository resultRepository;
    private final ImportJobMapper mapper;

    public ImportJobStore(ImportJobRepository jobRepository,
                          ImportResultRepository resultRepository,
                          ImportJobMapper mapper) {
        this.jobRepository = jobRepository;
        this.resultRepository = resultRepository;
        this.mapper = mapper;
    }

    @Transactional
    public void salvar(ImportJob job) {
        jobRepository.save(mapper.toEntity(job));
    }

    @Transactional(readOnly = true)
    public Optional<ImportJob> buscar(String jobId) {
        return jobRepository.findById(UUID.fromString(jobId))
                .map(entity -> mapper.toDomain(entity, carregarResultados(entity.getId())));
    }

    @Transactional(readOnly = true)
    public Optional<ImportJob> buscarDoUsuario(String jobId, UUID userId) {
        return jobRepository.findByIdAndUserId(UUID.fromString(jobId), userId)
                .map(entity -> mapper.toDomain(entity, carregarResultados(entity.getId())));
    }

    @Transactional(readOnly = true)
    public List<ImportJobEntity> listarHistorico(UUID userId, int limite) {
        return jobRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limite));
    }

    @Transactional
    public void salvarResultadoLinha(ImportJob job, int linhaNumero, CnpjResult resultado) {
        resultRepository.save(mapper.toResultEntity(UUID.fromString(job.getId()), linhaNumero, resultado));
        salvar(job);
    }

    @Transactional(readOnly = true)
    public int total() {
        return (int) jobRepository.count();
    }

    @Transactional(readOnly = true)
    public int contarAtivosPorUsuario(UUID userId) {
        return (int) jobRepository.countAtivosPorUsuario(userId);
    }

    @Transactional(readOnly = true)
    public int contarAtivos() {
        return (int) jobRepository.countAtivos();
    }

    @Transactional
    public List<String> removerExpirados(Duration ttl) {
        Instant limite = Instant.now().minus(ttl);
        List<String> removidos = new ArrayList<>();

        for (ImportJobEntity entity : jobRepository.findAll()) {
            boolean ativo = ImportJobStatus.NA_FILA.name().equals(entity.getStatus())
                    || ImportJobStatus.PROCESSANDO.name().equals(entity.getStatus());
            Instant referencia = entity.getCompletedAt() != null ? entity.getCompletedAt() : entity.getCreatedAt();
            if (!ativo && referencia.isBefore(limite)) {
                resultRepository.deleteByJobId(entity.getId());
                jobRepository.delete(entity);
                removidos.add(entity.getId().toString());
            }
        }

        return removidos;
    }

    private List<CnpjResult> carregarResultados(UUID jobId) {
        return resultRepository.findByJobIdOrderByLinhaNumeroAsc(jobId).stream()
                .map(mapper::toResultDto)
                .collect(Collectors.toList());
    }
}
