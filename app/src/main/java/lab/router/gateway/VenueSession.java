package lab.router.gateway;

import lab.router.domain.Order;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One logical session with an exchange gateway. The venue requires every
 * message on a session to carry the next sequence number, with no gaps and
 * no reordering, and rejects anything else.
 */
public class VenueSession {
    private static final Pattern VENUE_ORDER_ID = Pattern.compile("\"venueOrderId\":\"([^\"]+)\"");

    private final HttpClient http;
    private final URI ordersUri;
    private final URI logonUri;
    private long seq;
    private boolean loggedOn;

    public VenueSession(HttpClient http, String baseUrl, String venue, int number) {
        this.http = http;
        String base = baseUrl + "/venues/" + venue + "/sessions/" + number;
        this.ordersUri = URI.create(base + "/orders");
        this.logonUri = URI.create(base + "/logon");
    }

    public synchronized String send(Order order) {
        if (!loggedOn) logon();
        long next = seq + 1;
        String body = "{\"seq\":" + next + ",\"clOrdId\":" + order.getId() + ",\"symbol\":\"" + order.getSymbol()
                + "\",\"side\":\"" + order.getSide() + "\",\"qty\":" + order.getQty()
                + ",\"price\":" + order.getPrice().toPlainString() + "}";
        try {
            HttpResponse<String> response = post(ordersUri, body);
            if (response.statusCode() != 200) {
                loggedOn = false;   // resynchronise on the next message
                throw new GatewayException("venue rejected message: " + response.body());
            }
            seq = next;
            Matcher m = VENUE_ORDER_ID.matcher(response.body());
            if (!m.find()) throw new GatewayException("malformed ack: " + response.body());
            return m.group(1);
        } catch (IOException e) {
            loggedOn = false;
            throw new GatewayException("venue unavailable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            loggedOn = false;
            throw new GatewayException("interrupted", e);
        }
    }

    private void logon() {
        try {
            HttpResponse<String> response = post(logonUri, "{}");
            if (response.statusCode() != 200) throw new GatewayException("logon refused: " + response.body());
            seq = 0;
            loggedOn = true;
        } catch (IOException e) {
            throw new GatewayException("venue unavailable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GatewayException("interrupted", e);
        }
    }

    private HttpResponse<String> post(URI uri, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
