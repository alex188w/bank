package example.bank.config;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class TraceContextFilter implements WebFilter {

  public static final String ATTR_TRACE_ID = "trace.id";
  public static final String ATTR_SPAN_ID  = "span.id";

  private static final Pattern TRACEPARENT =
      Pattern.compile("^[\\da-f]{2}-[\\da-f]{32}-[\\da-f]{16}-[\\da-f]{2}$", Pattern.CASE_INSENSITIVE);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String tp = exchange.getRequest().getHeaders().getFirst("traceparent");
    if (tp != null) {
      tp = tp.trim();
      if (TRACEPARENT.matcher(tp).matches()) {
        String[] parts = tp.split("-");
        exchange.getAttributes().put(ATTR_TRACE_ID, parts[1]);
        exchange.getAttributes().put(ATTR_SPAN_ID,  parts[2]);
      }
    }
    return chain.filter(exchange);
  }
}
