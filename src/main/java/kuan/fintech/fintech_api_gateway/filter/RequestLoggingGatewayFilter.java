package kuan.fintech.fintech_api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class RequestLoggingGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingGatewayFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(CorrelationIdGatewayFilter.CORRELATION_ID_HEADER);
        long start = System.currentTimeMillis();

        log.info(
                "Incoming request method={} path={} query={}",
                request.getMethod(),
                request.getURI().getPath(),
                request.getURI().getQuery()
        );

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    if (correlationId != null && !correlationId.isBlank()) {
                        MDC.put(CorrelationIdGatewayFilter.MDC_KEY, correlationId);
                    }

                    try {
                        var response = exchange.getResponse();
                        long duration = System.currentTimeMillis() - start;

                        log.info(
                                "Completed request method={} path={} status={} durationMs={}",
                                request.getMethod(),
                                request.getURI().getPath(),
                                response.getStatusCode(),
                                duration
                        );
                    }
                    finally {
                        MDC.remove(CorrelationIdGatewayFilter.MDC_KEY);
                    }
                });
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
