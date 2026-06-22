package br.com.dadoscnpj.client;

import br.com.dadoscnpj.config.CnpjApiProperties;
import br.com.dadoscnpj.dto.CnpjResponse;
import br.com.dadoscnpj.service.CnpjConsultaPort;
import br.com.dadoscnpj.util.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class CnpjClient implements CnpjConsultaPort {

    private static final Logger log = LoggerFactory.getLogger(CnpjClient.class);
    private static final int MAX_TENTATIVAS_429 = 4;

    private final RestClient restClient;
    private final RateLimiter rateLimiter;
    private final CnpjApiProperties properties;
    private final long minDelayMs;

    public CnpjClient(CnpjApiProperties properties, RateLimiter rateLimiter) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.minDelayMs = properties.getMinDelayBetweenRequestsMs();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.getConsultaBaseUrl())
                .requestFactory(requestFactory)
                .build();

        log.info("Consulta CNPJ configurada: {} ({})",
                properties.getConsultaBaseUrl(),
                properties.isConsultaComercialAtiva() ? "comercial" : "pública");
    }

    public CnpjResponse consultar(String cnpj) throws InterruptedException {
        rateLimiter.acquire();
        return executarConsulta(cnpj, 0);
    }

    private CnpjResponse executarConsulta(String cnpj, int tentativa429) throws InterruptedException {
        String url = "/" + cnpj;
        boolean retry429 = tentativa429 > 0;
        log.info("Consultando CNPJ: {}{}{}", cnpj,
                properties.isConsultaComercialAtiva() ? " [comercial]" : " [pública]",
                retry429 ? " (retry " + tentativa429 + ")" : "");

        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get().uri(url);
            if (properties.isConsultaComercialAtiva()) {
                request = request.header("x_api_token", properties.getToken());
            }
            return request.retrieve().body(CnpjResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("CNPJ não encontrado: {}", cnpj);
            throw new CnpjConsultaException("CNPJ não encontrado na API pública", e);
        } catch (HttpClientErrorException.TooManyRequests e) {
            if (tentativa429 < MAX_TENTATIVAS_429) {
                long esperaMs = minDelayMs * (tentativa429 + 1L);
                log.warn("HTTP 429 para CNPJ {}. Aguardando {} ms antes de tentar novamente.", cnpj, esperaMs);
                Thread.sleep(esperaMs);
                return executarConsulta(cnpj, tentativa429 + 1);
            }
            log.error("HTTP 429 persistente para CNPJ {}", cnpj);
            throw new CnpjConsultaException("Limite de requisições excedido (HTTP 429)", e);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            log.error("Token inválido ao consultar CNPJ {}", cnpj);
            throw new CnpjConsultaException("Token CNPJ.ws inválido ou sem permissão", e);
        } catch (HttpClientErrorException e) {
            log.error("Erro HTTP {} ao consultar CNPJ {}: {}", e.getStatusCode(), cnpj, e.getMessage());
            throw new CnpjConsultaException("Erro HTTP " + e.getStatusCode().value() + " na consulta", e);
        } catch (HttpServerErrorException e) {
            if (tentativa429 < MAX_TENTATIVAS_429) {
                long esperaMs = minDelayMs * (tentativa429 + 1L);
                log.warn("HTTP {} para CNPJ {}. Aguardando {} ms antes de tentar novamente.",
                        e.getStatusCode(), cnpj, esperaMs);
                Thread.sleep(esperaMs);
                return executarConsulta(cnpj, tentativa429 + 1);
            }
            log.error("Erro do servidor (HTTP {}) ao consultar CNPJ {}", e.getStatusCode(), cnpj);
            throw new CnpjConsultaException("Erro interno da API (HTTP " + e.getStatusCode().value() + ")", e);
        } catch (ResourceAccessException e) {
            if (tentativa429 < MAX_TENTATIVAS_429) {
                long esperaMs = minDelayMs * (tentativa429 + 1L);
                log.warn("Falha de conexão ao consultar CNPJ {}. Aguardando {} ms.", cnpj, esperaMs);
                Thread.sleep(esperaMs);
                return executarConsulta(cnpj, tentativa429 + 1);
            }
            log.error("Timeout ou falha de conexão ao consultar CNPJ {}: {}", cnpj, e.getMessage());
            throw new CnpjConsultaException("Timeout ou falha de conexão com a API", e);
        } catch (Exception e) {
            log.error("Erro inesperado ao consultar CNPJ {}: {}", cnpj, e.getMessage());
            throw new CnpjConsultaException("Erro inesperado: " + e.getMessage(), e);
        }
    }

    public static class CnpjConsultaException extends RuntimeException {

        public CnpjConsultaException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
