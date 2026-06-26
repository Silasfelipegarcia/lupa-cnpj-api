package br.com.lupainsights.observability;

import br.com.lupainsights.util.RequestIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = RequestContext.resolverOuGerarRequestId(request);
        String clientIp = RequestIpResolver.resolver(request);

        request.setAttribute(RequestContext.REQUEST_ATTRIBUTE, requestId);
        request.setAttribute("clientIp", clientIp);
        response.setHeader(RequestContext.HEADER_REQUEST_ID, requestId);

        RequestContext.iniciar(requestId, clientIp);
        try {
            filterChain.doFilter(request, response);
        } finally {
            RequestContext.limpar();
        }
    }
}
