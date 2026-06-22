package br.com.dadoscnpj.controller;

import br.com.dadoscnpj.config.CnpjApiProperties;
import br.com.dadoscnpj.csv.CnpjExcelTemplateWriter;
import br.com.dadoscnpj.dto.CnpjConfigResponse;
import br.com.dadoscnpj.dto.ImportJobResponse;
import br.com.dadoscnpj.dto.ImportRow;
import br.com.dadoscnpj.security.RateLimitFilter;
import br.com.dadoscnpj.service.CnpjImportService;
import br.com.dadoscnpj.service.ImportJobQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/cnpj")
public class CnpjImportController {

    private static final Logger log = LoggerFactory.getLogger(CnpjImportController.class);
    private static final long MAX_FILE_BYTES = 5 * 1024 * 1024;

    private final CnpjImportService cnpjImportService;
    private final ImportJobQueueService jobQueueService;
    private final CnpjExcelTemplateWriter templateWriter;
    private final CnpjApiProperties cnpjApiProperties;

    public CnpjImportController(CnpjImportService cnpjImportService,
                                ImportJobQueueService jobQueueService,
                                CnpjExcelTemplateWriter templateWriter,
                                CnpjApiProperties cnpjApiProperties) {
        this.cnpjImportService = cnpjImportService;
        this.jobQueueService = jobQueueService;
        this.templateWriter = templateWriter;
        this.cnpjApiProperties = cnpjApiProperties;
    }

    @GetMapping("/config")
    public ResponseEntity<CnpjConfigResponse> configuracao() {
        return ResponseEntity.ok(new CnpjConfigResponse(cnpjApiProperties.isPesquisaRazaoSocialAtiva()));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportJobResponse> importar(@RequestParam("file") MultipartFile file,
                                                      HttpServletRequest request) throws Exception {
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

        log.info("Recebido arquivo para importação: {} ({} bytes)",
                nomeArquivo, file.getSize());

        List<ImportRow> linhas = cnpjImportService.lerLinhasDoArquivo(file);
        String clientIp = (String) request.getAttribute(RateLimitFilter.CLIENT_IP_ATTRIBUTE);
        ImportJobResponse job = jobQueueService.enfileirar(nomeArquivo, linhas, clientIp);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> baixarModelo() throws Exception {
        byte[] excel = templateWriter.gerarModelo();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"lupa-cnpj-modelo.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new ByteArrayResource(excel));
    }

    @GetMapping("/import/{jobId}/status")
    public ResponseEntity<ImportJobResponse> status(@PathVariable String jobId) {
        return ResponseEntity.ok(jobQueueService.consultarStatus(jobId));
    }

    @GetMapping("/import/{jobId}/download")
    public ResponseEntity<Resource> download(@PathVariable String jobId) {
        byte[] csvResultado = jobQueueService.baixarResultado(jobId);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nomeArquivo = "cnpj_resultado_" + timestamp + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(new ByteArrayResource(csvResultado));
    }

    private boolean extensaoPermitida(String nomeArquivo) {
        if (nomeArquivo == null) {
            return false;
        }
        String nome = nomeArquivo.toLowerCase();
        return nome.endsWith(".csv") || nome.endsWith(".xlsx") || nome.endsWith(".xls");
    }
}
