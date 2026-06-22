package br.com.dadoscnpj.controller;

import br.com.dadoscnpj.dto.ImportJobResponse;
import br.com.dadoscnpj.dto.ImportRow;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/cnpj")
public class CnpjImportController {

    private static final Logger log = LoggerFactory.getLogger(CnpjImportController.class);

    private final CnpjImportService cnpjImportService;
    private final ImportJobQueueService jobQueueService;

    public CnpjImportController(CnpjImportService cnpjImportService,
                                ImportJobQueueService jobQueueService) {
        this.cnpjImportService = cnpjImportService;
        this.jobQueueService = jobQueueService;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportJobResponse> importar(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Recebido arquivo para importação: {} ({} bytes)",
                file.getOriginalFilename(), file.getSize());

        List<ImportRow> linhas = cnpjImportService.lerLinhasDoArquivo(file);
        ImportJobResponse job = jobQueueService.enfileirar(file.getOriginalFilename(), linhas);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
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
}
