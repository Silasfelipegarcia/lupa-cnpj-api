package br.com.dadoscnpj.client;

import br.com.dadoscnpj.config.CnpjApiProperties;
import br.com.dadoscnpj.dto.CnpjPesquisaResponse;
import br.com.dadoscnpj.util.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Component
public class CnpjPesquisaClient {

    private static final Logger log = LoggerFactory.getLogger(CnpjPesquisaClient.class);

    private final RestClient restClient;
    private final RateLimiter rateLimiter;
    private final CnpjApiProperties properties;
    private final long minDelayMs;

    public CnpjPesquisaClient(CnpjApiProperties properties, RateLimiter rateLimiter) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.minDelayMs = properties.getMinDelayBetweenRequestsMs();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));

        this.restClient = RestClient.builder()
                .baseUrl(extrairBaseUrl(properties.getPesquisaUrl()))
                .requestFactory(requestFactory)
                .build();
    }

    private String extrairBaseUrl(String pesquisaUrl) {
        int idx = pesquisaUrl.indexOf("/v2/");
        if (idx > 0) {
            return pesquisaUrl.substring(0, idx);
        }
        return "https://comercial.cnpj.ws";
    }

    public List<String> buscarPorRazaoSocial(String razaoSocial) throws InterruptedException {
        if (!properties.isPesquisaHabilitada()) {
            throw new CnpjPesquisaException(
                    "Busca por razão social não está configurada no servidor. "
                            + "Informe um CNPJ válido na planilha ou configure CNPJ_WS_TOKEN no Railway.");
        }

        if (razaoSocial == null || razaoSocial.trim().length() < 3) {
            throw new CnpjPesquisaException("Razão social deve ter pelo menos 3 caracteres");
        }

        rateLimiter.acquire();
        return executarPesquisa(razaoSocial.trim(), false);
    }

    private List<String> executarPesquisa(String razaoSocial, boolean retry) throws InterruptedException {
        String uri = UriComponentsBuilder.fromPath("/v2/pesquisa")
                .queryParam("razao_social", razaoSocial)
                .queryParam("limite", 5)
                .build()
                .toUriString();

        log.info("Pesquisando razão social: {}{}", razaoSocial, retry ? " (retry)" : "");

        try {
            CnpjPesquisaResponse response = restClient.get()
                    .uri(uri)
                    .header("x_api_token", properties.getToken())
                    .retrieve()
                    .body(CnpjPesquisaResponse.class);

            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }
            return response.getData();
        } catch (HttpClientErrorException.TooManyRequests e) {
            if (!retry) {
                log.warn("HTTP 429 na pesquisa. Aguardando {} ms.", minDelayMs);
                Thread.sleep(minDelayMs);
                return executarPesquisa(razaoSocial, true);
            }
            throw new CnpjPesquisaException("Limite de requisições excedido (HTTP 429)", e);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw new CnpjPesquisaException("Token inválido ou plano sem permissão para pesquisa por razão social", e);
        } catch (HttpClientErrorException.NotFound e) {
            return Collections.emptyList();
        } catch (HttpClientErrorException e) {
            throw new CnpjPesquisaException("Erro HTTP " + e.getStatusCode().value() + " na pesquisa", e);
        } catch (HttpServerErrorException e) {
            throw new CnpjPesquisaException("Erro interno da API na pesquisa (HTTP " + e.getStatusCode().value() + ")", e);
        } catch (ResourceAccessException e) {
            throw new CnpjPesquisaException("Timeout ou falha de conexão na pesquisa", e);
        } catch (Exception e) {
            throw new CnpjPesquisaException("Erro inesperado na pesquisa: " + e.getMessage(), e);
        }
    }

    public static class CnpjPesquisaException extends RuntimeException {

        public CnpjPesquisaException(String message) {
            super(message);
        }

        public CnpjPesquisaException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
