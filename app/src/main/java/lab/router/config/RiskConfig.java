package lab.router.config;

import lab.router.risk.ExposureBook;
import lab.router.risk.RoutingStats;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RiskConfig {
    @Bean
    public ExposureBook exposureBook() { return new ExposureBook(); }

    @Bean
    public RoutingStats routingStats() { return new RoutingStats(); }
}
