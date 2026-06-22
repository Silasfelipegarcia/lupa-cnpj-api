package br.com.dadoscnpj.client;

import br.com.dadoscnpj.config.CnpjApiProperties;
import br.com.dadoscnpj.dto.CnpjResponse;
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
public class CnpjClient {

    private static final Logger log = LoggerFactory.getLogger(CnpjClient.class);

    private final RestClient restClient;
    private final RateLimiter rateLimiter;
    private final long minDelayMs;

    public CnpjClient(CnpjApiProperties properties, RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
        this.minDelayMs = properties.getMinDelayBetweenRequestsMs();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public CnpjResponse consultar(String cnpj) throws InterruptedException {
        rateLimiter.acquire();
        return executarConsulta(cnpj, false);
    }

    private CnpjResponse executarConsulta(String cnpj, boolean retry) throws InterruptedException {
        String url = "/" + cnpj;
        log.info("Consultando CNPJ: {}{}", cnpj, retry ? " (retry)" : "");

        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(CnpjResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("CNPJ não encontrado: {}", cnpj);
            throw new CnpjConsultaException("CNPJ não encontrado na API pública", e);
        } catch (HttpClientErrorException.TooManyRequests e) {
            if (!retry) {
                log.warn("HTTP 429 para CNPJ {}. Aguardando {} ms antes de tentar novamente.", cnpj, minDelayMs);
                Thread.sleep(minDelayMs);
                return executarConsulta(cnpj, true);
            }
            log.error("HTTP 429 persistente para CNPJ {}", cnpj);
            throw new CnpjConsultaException("Limite de requisições excedido (HTTP 429)", e);
        } catch (HttpClientErrorException e) {
            log.error("Erro HTTP {} ao consultar CNPJ {}: {}", e.getStatusCode(), cnpj, e.getMessage());
            throw new CnpjConsultaException("Erro HTTP " + e.getStatusCode().value() + " na consulta", e);
        } catch (HttpServerErrorException e) {
            log.error("Erro do servidor (HTTP {}) ao consultar CNPJ {}", e.getStatusCode(), cnpj);
            throw new CnpjConsultaException("Erro interno da API (HTTP " + e.getStatusCode().value() + ")", e);
        } catch (ResourceAccessException e) {
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
