package lab.router;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OrderRouterApplication {
    static void main(String[] args) {
        SpringApplication.run(OrderRouterApplication.class, args);
    }
}
