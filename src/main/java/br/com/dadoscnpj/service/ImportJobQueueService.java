package br.com.dadoscnpj.service;

import br.com.dadoscnpj.config.SecurityProperties;
import br.com.dadoscnpj.csv.CnpjCsvWriter;
import br.com.dadoscnpj.dto.CnpjResult;
import br.com.dadoscnpj.dto.ImportJobResponse;
import br.com.dadoscnpj.dto.ImportJobStatus;
import br.com.dadoscnpj.dto.ImportRow;
import br.com.dadoscnpj.model.ImportJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

@Service
public class ImportJobQueueService {

    private static final Logger log = LoggerFactory.getLogger(ImportJobQueueService.class);

    private final ImportJobStore jobStore;
    private final CnpjImportService cnpjImportService;
    private final ExecutorService importJobExecutor;
    private final SecurityProperties securityProperties;
    private final ConcurrentLinkedQueue<String> fila = new ConcurrentLinkedQueue<>();

    public ImportJobQueueService(ImportJobStore jobStore,
                                 CnpjImportService cnpjImportService,
                                 ExecutorService importJobExecutor,
                                 SecurityProperties securityProperties) {
        this.jobStore = jobStore;
        this.cnpjImportService = cnpjImportService;
        this.importJobExecutor = importJobExecutor;
        this.securityProperties = securityProperties;
    }

    public ImportJobResponse enfileirar(String nomeArquivo, List<ImportRow> linhas, String clientIp) {
        validarLimites(nomeArquivo, linhas, clientIp);

        ImportJob job = new ImportJob(nomeArquivo, linhas, clientIp);
        jobStore.salvar(job);
        fila.offer(job.getId());

        log.info("Job {} enfileirado com {} linha(s). Posição na fila: {}",
                job.getId(), linhas.size(), calcularPosicaoFila(job.getId()));

        importJobExecutor.submit(this::processarProximoDaFila);

        return toResponse(job);
    }

    private void validarLimites(String nomeArquivo, List<ImportRow> linhas, String clientIp) {
        if (linhas.isEmpty()) {
            throw new IllegalArgumentException("O arquivo não contém linhas válidas para importação.");
        }

        if (linhas.size() > securityProperties.getMaxRowsPerFile()) {
            throw new IllegalArgumentException(String.format(
                    "O arquivo excede o limite de %d linhas. Divida em arquivos menores.",
                    securityProperties.getMaxRowsPerFile()));
        }

        if (fila.size() >= securityProperties.getMaxQueueSize()) {
            throw new IllegalStateException(
                    "A fila de processamento está cheia. Tente novamente em alguns minutos.");
        }

        if (jobStore.total() >= securityProperties.getMaxJobsInMemory()) {
            throw new IllegalStateException(
                    "O servidor está com muitas consultas em andamento. Tente novamente mais tarde.");
        }

        if (jobStore.contarAtivosPorIp(clientIp) >= securityProperties.getMaxActiveJobsPerIp()) {
            throw new IllegalStateException(
                    "Você já possui uma importação em andamento. Aguarde a conclusão antes de enviar outra.");
        }
    }

    public ImportJobResponse consultarStatus(String jobId) {
        ImportJob job = jobStore.buscar(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job não encontrado: " + jobId));
        return toResponse(job);
    }

    public ImportJobResponse cancelar(String jobId) {
        ImportJob job = jobStore.buscar(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job não encontrado: " + jobId));

        if (job.getStatus() == ImportJobStatus.CONCLUIDO
                || job.getStatus() == ImportJobStatus.ERRO
                || job.getStatus() == ImportJobStatus.CANCELADO) {
            return toResponse(job);
        }

        job.solicitarCancelamento();

        if (job.getStatus() == ImportJobStatus.NA_FILA) {
            removerDaFila(job.getId());
            job.setStatus(ImportJobStatus.CANCELADO);
            job.setMensagem("Consulta cancelada pelo usuário");
            job.setConcluidoEm(Instant.now());
            log.info("Job {} removido da fila por cancelamento", job.getId());
        } else {
            job.setMensagem("Cancelando consulta...");
            log.info("Cancelamento solicitado para job {} em processamento", job.getId());
        }

        return toResponse(job);
    }

    public byte[] baixarResultado(String jobId) {
        ImportJob job = jobStore.buscar(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job não encontrado: " + jobId));

        if (job.getStatus() != ImportJobStatus.CONCLUIDO && job.getStatus() != ImportJobStatus.CANCELADO) {
            throw new IllegalStateException("Job ainda não foi concluído");
        }
        if (job.getResultado() == null) {
            throw new IllegalStateException("Resultado do job não disponível");
        }
        return job.getResultado();
    }

    private void processarProximoDaFila() {
        String jobId = fila.poll();
        if (jobId == null) {
            return;
        }

        ImportJob job = jobStore.buscar(jobId).orElse(null);
        if (job == null) {
            processarProximoDaFila();
            return;
        }

        try {
            job.setStatus(ImportJobStatus.PROCESSANDO);
            job.setMensagem("Processamento iniciado");

            log.info("Iniciando job {} ({} CNPJs)", job.getId(), job.getTotal());

            List<CnpjResult> resultados = cnpjImportService.processarLinhas(
                    job.getLinhas(),
                    progresso -> {
                        job.adicionarResultado(progresso.resultado());
                        job.incrementarProcessados();
                        if ("SUCESSO".equals(progresso.statusConsulta())) {
                            job.incrementarSucesso();
                        } else {
                            job.incrementarErros();
                        }
                        job.setMensagem(String.format("Consultando %d de %d (%d%%)",
                                job.getProcessados(), job.getTotal(), job.getPercentual()));
                    },
                    () -> !job.isCancelamentoSolicitado());

            if (job.isCancelamentoSolicitado()) {
                if (!resultados.isEmpty()) {
                    job.setResultado(cnpjImportService.gerarCsv(resultados));
                }
                job.setStatus(ImportJobStatus.CANCELADO);
                job.setMensagem(String.format(
                        "Consulta cancelada após %d de %d linha(s)",
                        job.getProcessados(), job.getTotal()));
                job.setConcluidoEm(Instant.now());
                log.info("Job {} cancelado pelo usuário", job.getId());
            } else {
                job.setResultado(cnpjImportService.gerarCsv(resultados));
                job.setStatus(ImportJobStatus.CONCLUIDO);
                job.setMensagem(String.format("Concluído: %d sucesso(s), %d erro(s)",
                        job.getSucesso(), job.getErros()));
                job.setConcluidoEm(Instant.now());
                log.info("Job {} concluído com sucesso", job.getId());
            }
        } catch (Exception e) {
            log.error("Falha no job {}: {}", job.getId(), e.getMessage(), e);
            job.setStatus(ImportJobStatus.ERRO);
            job.setMensagem("Erro no processamento: " + e.getMessage());
            job.setConcluidoEm(Instant.now());
        } finally {
            if (!fila.isEmpty()) {
                importJobExecutor.submit(this::processarProximoDaFila);
            }
        }
    }

    private void removerDaFila(String jobId) {
        fila.removeIf(id -> id.equals(jobId));
    }

    private int calcularPosicaoFila(String jobId) {
        int posicao = 1;
        for (String id : fila) {
            if (id.equals(jobId)) {
                return posicao;
            }
            posicao++;
        }
        return 0;
    }

    private ImportJobResponse toResponse(ImportJob job) {
        ImportJobResponse response = new ImportJobResponse();
        response.setJobId(job.getId());
        response.setStatus(job.getStatus());
        response.setArquivo(job.getArquivo());
        response.setTotal(job.getTotal());
        response.setProcessados(job.getProcessados());
        response.setSucesso(job.getSucesso());
        response.setErros(job.getErros());
        response.setPercentual(job.getPercentual());
        response.setPosicaoFila(job.getStatus() == ImportJobStatus.NA_FILA
                ? calcularPosicaoFila(job.getId()) : 0);
        response.setMensagem(job.getMensagem());
        response.setResultados(job.getResultados());
        return response;
    }
}
