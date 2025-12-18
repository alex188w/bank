package example.bank.config;

import io.micrometer.common.KeyValues;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.http.server.reactive.observation.ServerRequestObservationConvention;

@Configuration
public class EnvObservationConfig {

    @Bean
    public ServerRequestObservationConvention envServerRequestObservationConvention(
            @Value("${app.env:dev}") String env
    ) {
        return new DefaultServerRequestObservationConvention() {
            @Override
            public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
                return super.getLowCardinalityKeyValues(context).and("env", env);
            }
        };
    }
}
