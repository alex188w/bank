package example.bank.config;

// import org.springframework.cloud.client.loadbalancer.LoadBalanced;
// import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
// import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
// import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
// import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
// import org.springframework.web.reactive.function.client.WebClient;

// @Configuration
// public class WebClientConfig {

//         @Bean
//         public WebClient exchangeWebClient(ReactiveClientRegistrationRepository clients,
//                         ReactiveOAuth2AuthorizedClientService authService) {
//                 ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2 = new ServerOAuth2AuthorizedClientExchangeFilterFunction(
//                                 new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clients, authService));
//                 oauth2.setDefaultClientRegistrationId("gateway-client");

//                 return WebClient.builder()
//                                 .baseUrl("http://localhost:8084")
//                                 .filter(oauth2)
//                                 .build();
//         }
// }
