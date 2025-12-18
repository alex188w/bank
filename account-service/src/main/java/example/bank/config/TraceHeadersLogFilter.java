package example.bank.config;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class TraceHeadersLogFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var h = exchange.getRequest().getHeaders();
        System.out.println("B3 incoming: traceId=" + h.getFirst("X-B3-TraceId")
                + " spanId=" + h.getFirst("X-B3-SpanId")
                + " parent=" + h.getFirst("X-B3-ParentSpanId")
                + " b3=" + h.getFirst("b3"));
        return chain.filter(exchange);
    }
}
