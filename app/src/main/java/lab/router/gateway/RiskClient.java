package lab.router.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Client of the firm-wide pre-trade risk service (another team's system). */
@Component
public class RiskClient {
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final URI uri;

    public RiskClient(@Value("${router.risk-url}") String baseUrl) {
        this.uri = URI.create(baseUrl + "/risk/check");
    }

    public boolean approve(long accountId, String symbol, String side, int qty, BigDecimal price) {
        String body = "{\"accountId\":" + accountId + ",\"symbol\":\"" + symbol + "\",\"side\":\"" + side
                + "\",\"qty\":" + qty + ",\"price\":" + price.toPlainString() + "}";
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body().contains("\"approved\":true");
        } catch (IOException e) {
            throw new GatewayException("risk service unavailable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GatewayException("interrupted", e);
        }
    }
}
