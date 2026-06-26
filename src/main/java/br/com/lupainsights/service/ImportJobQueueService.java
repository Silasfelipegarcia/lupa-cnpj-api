package br.com.lupainsights.service;

import br.com.lupainsights.config.SecurityProperties;
import br.com.lupainsights.dto.ImportJobResponse;
import br.com.lupainsights.dto.ImportJobStatus;
import br.com.lupainsights.dto.ImportJobSummaryResponse;
import br.com.lupainsights.dto.ImportRow;
import br.com.lupainsights.entity.ImportJobEntity;
import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.exception.ForbiddenException;
import br.com.lupainsights.util.ImportLineDedupe;
import br.com.lupainsights.model.ImportJob;
import br.com.lupainsights.plan.UsageTrackingService;
import br.com.lupainsights.config.CnpjApiProperties;
import br.com.lupainsights.plan.PlanLimitsService;
import br.com.lupainsights.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

@Service
public class ImportJobQueueService {

    private static final Logger log = LoggerFactory.getLogger(ImportJobQueueService.class);

    private final ImportJobStore jobStore;
    private final CnpjImportService cnpjImportService;
    private final ExecutorService importJobExecutor;
    private final SecurityProperties securityProperties;
    private final UserRepository userRepository;
    private final UsageTrackingService usageTrackingService;
    private final PlanLimitsService planLimitsService;
    private final CnpjApiProperties cnpjApiProperties;
    private final ImportJobMapper importJobMapper;
    private final ConcurrentLinkedQueue<String> fila = new ConcurrentLinkedQueue<>();
    private final Map<String, String> jobIdPorClientIp = new ConcurrentHashMap<>();

    public ImportJobQueueService(ImportJobStore jobStore,
                                 CnpjImportService cnpjImportService,
                                 ExecutorService importJobExecutor,
                                 SecurityProperties securityProperties,
                                 UserRepository userRepository,
                                 UsageTrackingService usageTrackingService,
                                 PlanLimitsService planLimitsService,
                                 CnpjApiProperties cnpjApiProperties,
                                 ImportJobMapper importJobMapper) {
        this.jobStore = jobStore;
        this.cnpjImportService = cnpjImportService;
        this.importJobExecutor = importJobExecutor;
        this.securityProperties = securityProperties;
        this.userRepository = userRepository;
        this.usageTrackingService = usageTrackingService;
        this.planLimitsService = planLimitsService;
        this.cnpjApiProperties = cnpjApiProperties;
        this.importJobMapper = importJobMapper;
    }

    public ImportJobResponse enfileirar(String nomeArquivo, List<ImportRow> linhas, UUID userId, String clientIp) {
        List<ImportRow> linhasPreparadas = prepararLinhas(linhas, userId);
        validarLimites(nomeArquivo, linhasPreparadas, userId, clientIp);

        ImportJob job = new ImportJob(nomeArquivo, linhasPreparadas, userId);
        jobStore.salvar(job);
        if (clientIp != null && !clientIp.isBlank()) {
            jobIdPorClientIp.put(job.getId(), clientIp);
        }
        fila.offer(job.getId());

        log.info("Job {} enfileirado para usuário {} com {} linha(s). Posição na fila: {}",
                job.getId(), userId, linhas.size(), calcularPosicaoFila(job.getId()));

        importJobExecutor.submit(this::processarProximoDaFila);

        return toResponse(job);
    }

    public ImportJobResponse consultarStatus(String jobId, UUID userId) {
        ImportJob job = buscarJobDoUsuario(jobId, userId);
        return toResponse(job);
    }

    public ImportJobResponse cancelar(String jobId, UUID userId) {
        ImportJob job = buscarJobDoUsuario(jobId, userId);

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
            jobStore.salvar(job);
            log.info("Job {} removido da fila por cancelamento", job.getId());
        } else {
            job.setMensagem("Cancelando consulta...");
            jobStore.salvar(job);
            log.info("Cancelamento solicitado para job {} em processamento", job.getId());
        }

        return toResponse(job);
    }

    public byte[] baixarResultado(String jobId, UUID userId) {
        ImportJob job = buscarJobDoUsuario(jobId, userId);

        if (job.getStatus() != ImportJobStatus.CONCLUIDO && job.getStatus() != ImportJobStatus.CANCELADO) {
            throw new IllegalStateException("Job ainda não foi concluído");
        }
        if (job.getResultado() == null) {
            throw new IllegalStateException("Resultado do job não disponível");
        }
        return job.getResultado();
    }

    public List<ImportJobSummaryResponse> listarHistorico(UUID userId, int limite) {
        return jobStore.listarHistorico(userId, limite).stream()
                .map(ImportJobSummaryResponse::from)
                .toList();
    }

    public ImportJobResponse consultarHistoricoDetalhe(String jobId, UUID userId) {
        ImportJob job = buscarJobDoUsuario(jobId, userId);
        return toResponse(job);
    }

    public ImportJobResponse consultarJobAtivo(UUID userId) {
        return jobStore.buscarAtivoDoUsuario(userId)
                .map(this::toResponse)
                .orElse(null);
    }

    public List<br.com.lupainsights.dto.ListaSalvaResponse> listarListasSalvas(UUID userId) {
        return jobStore.listarSalvas(userId, 50).stream()
                .map(entity -> {
                    br.com.lupainsights.dto.ListaSalvaResponse response = new br.com.lupainsights.dto.ListaSalvaResponse();
                    response.setJobId(entity.getId());
                    response.setNomeLista(entity.getNomeLista());
                    response.setArquivo(entity.getArquivo());
                    response.setTotal(entity.getTotal());
                    response.setCreatedAt(entity.getCreatedAt());
                    return response;
                })
                .toList();
    }

    public void salvarLista(String jobId, UUID userId, String nomeLista) {
        if (nomeLista == null || nomeLista.isBlank()) {
            throw new IllegalArgumentException("Informe um nome para a lista.");
        }
        jobStore.salvarComoLista(jobId, userId, nomeLista.trim());
    }

    public ImportJobResponse reprocessar(String jobId, UUID userId) {
        ImportJobEntity entity = jobStore.buscarEntidadeDoUsuario(jobId, userId)
                .orElseThrow(() -> new ForbiddenException("Você não tem permissão para acessar esta consulta"));
        List<ImportRow> linhas = importJobMapper.deserializarLinhasPublico(entity.getLinhasJson());
        String nome = "reprocesso-" + entity.getArquivo();
        return enfileirar(nome, linhas, userId, null);
    }

    public int recuperarJobsPendentes() {
        int recuperados = 0;
        for (ImportJobEntity entity : jobStore.listarAtivos()) {
            String jobId = entity.getId().toString();
            if (jaEstaNaFila(jobId)) {
                continue;
            }
            if (ImportJobStatus.PROCESSANDO.name().equals(entity.getStatus())) {
                ImportJob job = jobStore.buscar(jobId).orElse(null);
                if (job != null) {
                    job.setStatus(ImportJobStatus.NA_FILA);
                    if (job.getProcessados() > 0) {
                        job.setMensagem(String.format(
                                "Retomando consulta de onde parou (%d de %d)",
                                job.getProcessados(), job.getTotal()));
                    } else {
                        job.setMensagem("Aguardando retomada na fila");
                    }
                    jobStore.salvar(job);
                }
            }
            fila.offer(jobId);
            recuperados++;
            log.info("Job {} reenfileirado para retomada (status anterior: {})", jobId, entity.getStatus());
        }

        if (recuperados > 0) {
            importJobExecutor.submit(this::processarProximoDaFila);
        }
        return recuperados;
    }

    private ImportJob buscarJobDoUsuario(String jobId, UUID userId) {
        return jobStore.buscarDoUsuario(jobId, userId)
                .orElseThrow(() -> new ForbiddenException("Você não tem permissão para acessar esta consulta"));
    }

    private List<ImportRow> prepararLinhas(List<ImportRow> linhas, UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        if (planLimitsService.limitesDe(user).dedupeHabilitado()) {
            return ImportLineDedupe.deduplicar(linhas);
        }
        return linhas;
    }

    private void validarLimites(String nomeArquivo, List<ImportRow> linhas, UUID userId, String clientIp) {
        if (linhas.isEmpty()) {
            throw new IllegalArgumentException("O arquivo não contém linhas válidas para importação.");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        usageTrackingService.validarLinhasPorPlano(user, linhas.size());
        usageTrackingService.validarEIncrementarBatch(userId, linhas.size());

        int tetoGlobal = securityProperties.getMaxRowsPerFile();
        if (linhas.size() > tetoGlobal) {
            throw new IllegalArgumentException(String.format(
                    "O arquivo excede o limite global de %d linhas.", tetoGlobal));
        }

        if (fila.size() >= securityProperties.getMaxQueueSize()) {
            throw new IllegalStateException(
                    "A fila de processamento está cheia. Tente novamente em alguns minutos.");
        }

        if (jobStore.total() >= securityProperties.getMaxJobsInMemory()) {
            throw new IllegalStateException(
                    "O servidor está com muitas consultas em andamento. Tente novamente mais tarde.");
        }

        if (jobStore.contarAtivosPorUsuario(userId) >= securityProperties.getMaxActiveJobsPerUser()) {
            throw new IllegalStateException(
                    "Você já possui uma importação em andamento. Aguarde a conclusão antes de enviar outra.");
        }

        if (clientIp != null && !clientIp.isBlank()
                && contarAtivosPorIp(clientIp) >= securityProperties.getMaxActiveJobsPerIp()) {
            throw new IllegalStateException(
                    "Já existe uma importação em andamento para este endereço. Aguarde a conclusão.");
        }
    }

    private int contarAtivosPorIp(String clientIp) {
        return (int) jobStore.listarAtivos().stream()
                .map(entity -> entity.getId().toString())
                .filter(jobId -> clientIp.equals(jobIdPorClientIp.get(jobId)))
                .count();
    }

    private void liberarClientIp(String jobId) {
        jobIdPorClientIp.remove(jobId);
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
            boolean retomada = job.getProcessados() > 0;
            job.setStatus(ImportJobStatus.PROCESSANDO);
            job.setMensagem(retomada
                    ? String.format("Retomando consulta (%d de %d)", job.getProcessados(), job.getTotal())
                    : "Processamento iniciado");
            jobStore.salvar(job);

            log.info("Iniciando job {} ({} CNPJs, {} já processado(s))",
                    job.getId(), job.getTotal(), job.getProcessados());

            List<ImportRow> linhasPendentes = job.getLinhas().subList(job.getProcessados(), job.getTotal());
            if (linhasPendentes.isEmpty()) {
                finalizarJob(job);
                return;
            }

            CnpjImportService.CachesConsulta caches = cnpjImportService.construirCaches(job.getResultados());

            boolean pesquisaRazaoSocial = pesquisaRazaoSocialPermitida(job.getUserId());

            cnpjImportService.processarLinhas(
                    linhasPendentes,
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
                        jobStore.salvarResultadoLinha(job, job.getProcessados(), progresso.resultado());
                    },
                    () -> !job.isCancelamentoSolicitado(),
                    job.getProcessados() + 1,
                    caches.porCnpj(),
                    caches.porRazaoSocial(),
                    pesquisaRazaoSocial);

            if (job.isCancelamentoSolicitado()) {
                if (!job.getResultados().isEmpty()) {
                    job.setResultado(cnpjImportService.gerarCsv(job.getResultados()));
                }
                job.setStatus(ImportJobStatus.CANCELADO);
                job.setMensagem(String.format(
                        "Consulta cancelada após %d de %d linha(s)",
                        job.getProcessados(), job.getTotal()));
                job.setConcluidoEm(Instant.now());
                log.info("Job {} cancelado pelo usuário", job.getId());
            } else {
                job.setResultado(cnpjImportService.gerarCsv(job.getResultados()));
                job.setStatus(ImportJobStatus.CONCLUIDO);
                job.setMensagem(String.format("Concluído: %d sucesso(s), %d erro(s)",
                        job.getSucesso(), job.getErros()));
                job.setConcluidoEm(Instant.now());
                log.info("Job {} concluído com sucesso", job.getId());
            }
            jobStore.salvar(job);
        } catch (Exception e) {
            log.error("Falha no job {}: {}", job.getId(), e.getMessage(), e);
            job.setStatus(ImportJobStatus.ERRO);
            job.setMensagem("Erro no processamento: " + e.getMessage());
            job.setConcluidoEm(Instant.now());
            jobStore.salvar(job);
        } finally {
            liberarClientIp(job.getId());
            if (!fila.isEmpty()) {
                importJobExecutor.submit(this::processarProximoDaFila);
            }
        }
    }

    private void removerDaFila(String jobId) {
        fila.removeIf(id -> id.equals(jobId));
    }

    private boolean jaEstaNaFila(String jobId) {
        return fila.contains(jobId);
    }

    private void finalizarJob(ImportJob job) throws IOException {
        if (!job.getResultados().isEmpty()) {
            job.setResultado(cnpjImportService.gerarCsv(job.getResultados()));
        }
        job.setStatus(ImportJobStatus.CONCLUIDO);
        job.setMensagem(String.format("Concluído: %d sucesso(s), %d erro(s)",
                job.getSucesso(), job.getErros()));
        job.setConcluidoEm(Instant.now());
        jobStore.salvar(job);
        log.info("Job {} finalizado após recuperação (todas as linhas já estavam processadas)", job.getId());
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

    private boolean pesquisaRazaoSocialPermitida(UUID userId) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        return cnpjApiProperties.isPesquisaRazaoSocialAtiva()
                && planLimitsService.limitesDe(user).pesquisaRazaoSocial();
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
        response.setCreatedAt(job.getCriadoEm());
        response.setCompletedAt(job.getConcluidoEm());
        return response;
    }
}
