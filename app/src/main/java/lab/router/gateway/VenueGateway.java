package lab.router.gateway;

import lab.router.domain.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/** Routes an order to the exchange it is listed on. */
@Component
public class VenueGateway {
    private final Map<String, VenueSession> sessions;

    public VenueGateway(@Value("${router.venue-url}") String baseUrl) {
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        this.sessions = Map.of(
                "HOSE", new VenueSession(http, baseUrl, "HOSE", 1),
                "HNX", new VenueSession(http, baseUrl, "HNX", 1),
                "UPCOM", new VenueSession(http, baseUrl, "UPCOM", 1));
    }

    /** @return the order id assigned by the venue */
    public String send(Order order) {
        VenueSession session = sessions.get(order.getVenue());
        if (session == null) throw new GatewayException("no route to " + order.getVenue());
        return session.send(order);
    }
}
