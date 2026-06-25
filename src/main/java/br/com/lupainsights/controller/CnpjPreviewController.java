package br.com.lupainsights.controller;

import br.com.lupainsights.dto.CnpjPreviewQuotaResponse;
import br.com.lupainsights.dto.CnpjPreviewResponse;
import br.com.lupainsights.security.RateLimitFilter;
import br.com.lupainsights.service.GuestCnpjPreviewService;
import br.com.lupainsights.util.RequestIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cnpj/preview")
public class CnpjPreviewController {

    private final GuestCnpjPreviewService guestCnpjPreviewService;

    public CnpjPreviewController(GuestCnpjPreviewService guestCnpjPreviewService) {
        this.guestCnpjPreviewService = guestCnpjPreviewService;
    }

    @GetMapping("/quota")
    public ResponseEntity<CnpjPreviewQuotaResponse> quota(HttpServletRequest request) {
        String clientIp = ipDoCliente(request);
        return ResponseEntity.ok(guestCnpjPreviewService.obterQuota(clientIp));
    }

    @GetMapping
    public ResponseEntity<CnpjPreviewResponse> consultar(@RequestParam("cnpj") String cnpj,
                                                       HttpServletRequest request) throws InterruptedException {
        String clientIp = ipDoCliente(request);
        return ResponseEntity.ok(guestCnpjPreviewService.consultar(clientIp, cnpj));
    }

    private String ipDoCliente(HttpServletRequest request) {
        Object atributo = request.getAttribute(RateLimitFilter.CLIENT_IP_ATTRIBUTE);
        if (atributo instanceof String ip && !ip.isBlank()) {
            return ip;
        }
        return RequestIpResolver.resolver(request);
    }
}
