package br.com.lupainsights.controller;

import br.com.lupainsights.config.CnpjApiProperties;
import br.com.lupainsights.csv.CnpjExcelTemplateWriter;
import br.com.lupainsights.dto.CnpjConfigResponse;
import br.com.lupainsights.dto.CnpjResult;
import br.com.lupainsights.dto.ImportJobResponse;
import br.com.lupainsights.dto.ImportJobSummaryResponse;
import br.com.lupainsights.dto.ImportRow;
import br.com.lupainsights.dto.ListaSalvaResponse;
import br.com.lupainsights.dto.SalvarListaRequest;
import br.com.lupainsights.entity.UserEntity;
import br.com.lupainsights.exception.ForbiddenException;
import br.com.lupainsights.plan.PlanLimits;
import br.com.lupainsights.plan.PlanLimitsService;
import br.com.lupainsights.repository.UserRepository;
import br.com.lupainsights.security.SecurityUtils;
import br.com.lupainsights.service.CnpjDirectConsultaService;
import br.com.lupainsights.service.CnpjImportService;
import br.com.lupainsights.service.ImportJobQueueService;
import br.com.lupainsights.service.TrialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/cnpj")
public class CnpjImportController {

    private static final Logger log = LoggerFactory.getLogger(CnpjImportController.class);
    private static final long MAX_FILE_BYTES = 5 * 1024 * 1024;

    private final CnpjImportService cnpjImportService;
    private final ImportJobQueueService jobQueueService;
    private final CnpjExcelTemplateWriter templateWriter;
    private final CnpjApiProperties cnpjApiProperties;
    private final CnpjDirectConsultaService directConsultaService;
    private final UserRepository userRepository;
    private final PlanLimitsService planLimitsService;
    private final TrialService trialService;

    public CnpjImportController(CnpjImportService cnpjImportService,
                                ImportJobQueueService jobQueueService,
                                CnpjExcelTemplateWriter templateWriter,
                                CnpjApiProperties cnpjApiProperties,
                                CnpjDirectConsultaService directConsultaService,
                                UserRepository userRepository,
                                PlanLimitsService planLimitsService,
                                TrialService trialService) {
        this.cnpjImportService = cnpjImportService;
        this.jobQueueService = jobQueueService;
        this.templateWriter = templateWriter;
        this.cnpjApiProperties = cnpjApiProperties;
        this.directConsultaService = directConsultaService;
        this.userRepository = userRepository;
        this.planLimitsService = planLimitsService;
        this.trialService = trialService;
    }

    @GetMapping("/config")
    public ResponseEntity<CnpjConfigResponse> configuracao() {
        UUID userId = SecurityUtils.currentUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        trialService.expirarTrialSeNecessario(user);

        PlanLimits limits = planLimitsService.limitesDe(user);
        CnpjConfigResponse response = new CnpjConfigResponse();
        response.setPesquisaRazaoSocialHabilitada(
                cnpjApiProperties.isPesquisaRazaoSocialAtiva() && limits.pesquisaRazaoSocial());
        response.setExportExcel(limits.exportExcel());
        response.setFiltroSomenteAtivos(limits.filtroSomenteAtivos());
        response.setFiltrosAvancados(limits.filtrosAvancados());
        response.setDedupeHabilitado(limits.dedupeHabilitado());
        response.setTrialDisponivel(trialService.trialDisponivel(user));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/consulta")
    public ResponseEntity<CnpjResult> consultarCnpj(@RequestParam("cnpj") String cnpj) throws Exception {
        UUID userId = SecurityUtils.currentUserId();
        return ResponseEntity.ok(directConsultaService.consultar(userId, cnpj));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportJobResponse> importar(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("O arquivo excede o limite de 5 MB.");
        }

        String nomeArquivo = file.getOriginalFilename();
        if (!extensaoPermitida(nomeArquivo)) {
            throw new IllegalArgumentException("Formato não suportado. Use CSV ou Excel (.xlsx).");
        }

        UUID userId = SecurityUtils.currentUserId();
        log.info("Recebido arquivo para importação: {} ({} bytes) do usuário {}",
                nomeArquivo, file.getSize(), userId);

        List<ImportRow> linhas = cnpjImportService.lerLinhasDoArquivo(file);
        ImportJobResponse job = jobQueueService.enfileirar(nomeArquivo, linhas, userId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> baixarModelo() throws Exception {
        byte[] excel = templateWriter.gerarModelo();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"lupa-insights-modelo.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new ByteArrayResource(excel));
    }

    @GetMapping("/import/ativo")
    public ResponseEntity<ImportJobResponse> jobAtivo() {
        UUID userId = SecurityUtils.currentUserId();
        ImportJobResponse job = jobQueueService.consultarJobAtivo(userId);
        if (job == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(job);
    }

    @GetMapping("/import/historico")
    public ResponseEntity<List<ImportJobSummaryResponse>> historico() {
        UUID userId = SecurityUtils.currentUserId();
        return ResponseEntity.ok(jobQueueService.listarHistorico(userId, 50));
    }

    @GetMapping("/import/historico/{jobId}")
    public ResponseEntity<ImportJobResponse> historicoDetalhe(@PathVariable String jobId) {
        UUID userId = SecurityUtils.currentUserId();
        return ResponseEntity.ok(jobQueueService.consultarHistoricoDetalhe(jobId, userId));
    }

    @GetMapping("/import/listas-salvas")
    public ResponseEntity<List<ListaSalvaResponse>> listasSalvas() {
        UUID userId = SecurityUtils.currentUserId();
        return ResponseEntity.ok(jobQueueService.listarListasSalvas(userId));
    }

    @PostMapping("/import/{jobId}/salvar-lista")
    public ResponseEntity<Void> salvarLista(@PathVariable String jobId,
                                            @RequestBody SalvarListaRequest request) {
        UUID userId = SecurityUtils.currentUserId();
        jobQueueService.salvarLista(jobId, userId, request.getNomeLista());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/import/{jobId}/reprocessar")
    public ResponseEntity<ImportJobResponse> reprocessar(@PathVariable String jobId) {
        UUID userId = SecurityUtils.currentUserId();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(jobQueueService.reprocessar(jobId, userId));
    }

    @GetMapping("/import/{jobId}/status")
    public ResponseEntity<ImportJobResponse> status(@PathVariable String jobId) {
        UUID userId = SecurityUtils.currentUserId();
        return ResponseEntity.ok(jobQueueService.consultarStatus(jobId, userId));
    }

    @DeleteMapping("/import/{jobId}")
    public ResponseEntity<ImportJobResponse> cancelar(@PathVariable String jobId) {
        UUID userId = SecurityUtils.currentUserId();
        return ResponseEntity.ok(jobQueueService.cancelar(jobId, userId));
    }

    @GetMapping("/import/{jobId}/download")
    public ResponseEntity<Resource> download(@PathVariable String jobId,
                                             @RequestParam(value = "format", defaultValue = "csv") String format,
                                             @RequestParam(value = "somenteAtivos", defaultValue = "false") boolean somenteAtivos,
                                             @RequestParam(value = "uf", required = false) String uf,
                                             @RequestParam(value = "cnae", required = false) String cnae,
                                             @RequestParam(value = "comTelefone", defaultValue = "false") boolean comTelefone,
                                             @RequestParam(value = "comEmail", defaultValue = "false") boolean comEmail)
            throws Exception {
        UUID userId = SecurityUtils.currentUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        PlanLimits limits = planLimitsService.limitesDe(user);

        ImportJobResponse job = jobQueueService.consultarHistoricoDetalhe(jobId, userId);
        List<CnpjResult> resultados = job.getResultados() != null ? job.getResultados() : List.of();

        if (somenteAtivos && !limits.filtroSomenteAtivos()) {
            throw new ForbiddenException("Filtro de empresas ativas disponível no plano Prospecção ou superior.");
        }
        if ((comTelefone || comEmail || (uf != null && !uf.isBlank()) || (cnae != null && !cnae.isBlank()))
                && !limits.filtrosAvancados()) {
            throw new ForbiddenException("Filtros avançados disponíveis no plano Growth.");
        }

        List<CnpjResult> filtrados = cnpjImportService.filtrarResultados(
                resultados, somenteAtivos, uf, cnae, comTelefone, comEmail);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        boolean excel = "xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format);
        if (excel && !limits.exportExcel()) {
            throw new ForbiddenException("Exportação Excel disponível no plano Prospecção ou superior.");
        }

        byte[] conteudo;
        String nomeArquivo;
        MediaType mediaType;
        if (excel) {
            conteudo = cnpjImportService.gerarExcel(filtrados);
            nomeArquivo = "lupa_insights_prospeccao_" + timestamp + ".xlsx";
            mediaType = MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else {
            conteudo = cnpjImportService.gerarCsv(filtrados);
            nomeArquivo = "lupa_insights_prospeccao_" + timestamp + ".csv";
            mediaType = MediaType.parseMediaType("text/csv; charset=UTF-8");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .contentType(mediaType)
                .body(new ByteArrayResource(conteudo));
    }

    private boolean extensaoPermitida(String nomeArquivo) {
        if (nomeArquivo == null) {
            return false;
        }
        String nome = nomeArquivo.toLowerCase();
        return nome.endsWith(".csv") || nome.endsWith(".xlsx") || nome.endsWith(".xls");
    }
}
