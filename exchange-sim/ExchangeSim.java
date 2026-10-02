import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Everything the order router talks to that is not the database: the firm's
 * pre-trade risk service and the three exchange gateways. You do not own this
 * code and it is not where the problems are -- treat it as the outside world.
 *
 *   POST /risk/check                          ~30 ms
 *   POST /venues/{venue}/sessions/{n}/logon   resets the session's sequence
 *   POST /venues/{venue}/sessions/{n}/orders  ~3 ms, body must carry "seq"
 *   GET  /stats
 *
 * Venue rules (from the member connectivity spec):
 *   - a member may hold up to 16 sessions per venue, numbered 1..16
 *   - on each session, messages must arrive with seq = previous + 1;
 *     anything else is rejected with 409 and the order is NOT accepted
 */
public class ExchangeSim {
    static final int RISK_MS = Integer.getInteger("risk.ms", 30);
    static final int VENUE_MS = Integer.getInteger("venue.ms", 3);
    static final int MAX_SESSIONS = 16;
    static final Pattern SESSION = Pattern.compile("^/venues/([A-Z]+)/sessions/(\\d+)/(logon|orders)$");
    static final Pattern SEQ = Pattern.compile("\"seq\"\\s*:\\s*(\\d+)");

    static final Map<String, AtomicLong> lastSeq = new ConcurrentHashMap<>();
    static final AtomicLong riskChecks = new AtomicLong();
    static final AtomicLong accepted = new AtomicLong();
    static final AtomicLong seqViolations = new AtomicLong();
    static final AtomicLong venueOrderIds = new AtomicLong(1_000_000);

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(9000), 4096);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/risk/check", ExchangeSim::risk);
        server.createContext("/venues/", ExchangeSim::venue);
        server.createContext("/stats", ExchangeSim::stats);
        server.createContext("/health", x -> reply(x, 200, "{\"status\":\"UP\"}"));
        server.start();
        System.out.printf("exchange-sim up: risk=%dms venue=%dms%n", RISK_MS, VENUE_MS);
    }

    static void risk(HttpExchange x) throws IOException {
        x.getRequestBody().readAllBytes();
        pause(RISK_MS, RISK_MS / 6);
        riskChecks.incrementAndGet();
        reply(x, 200, "{\"approved\":true}");
    }

    static void venue(HttpExchange x) throws IOException {
        String body = new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Matcher m = SESSION.matcher(x.getRequestURI().getPath());
        if (!m.matches()) { reply(x, 404, "{\"error\":\"no such path\"}"); return; }
        int n = Integer.parseInt(m.group(2));
        if (n < 1 || n > MAX_SESSIONS) { reply(x, 403, "{\"error\":\"session not provisioned\"}"); return; }
        String key = m.group(1) + "-" + n;

        if (m.group(3).equals("logon")) {
            lastSeq.computeIfAbsent(key, k -> new AtomicLong()).set(0);
            reply(x, 200, "{\"session\":\"" + key + "\",\"nextSeq\":1}");
            return;
        }

        Matcher s = SEQ.matcher(body);
        AtomicLong last = lastSeq.get(key);
        if (last == null) { reply(x, 409, "{\"error\":\"session not logged on\"}"); return; }
        if (!s.find()) { reply(x, 400, "{\"error\":\"seq missing\"}"); return; }
        long seq = Long.parseLong(s.group(1));
        if (!last.compareAndSet(seq - 1, seq)) {
            seqViolations.incrementAndGet();
            reply(x, 409, "{\"error\":\"sequence gap\",\"expected\":" + (last.get() + 1) + ",\"got\":" + seq + "}");
            return;
        }
        pause(VENUE_MS, 1);
        accepted.incrementAndGet();
        reply(x, 200, "{\"venueOrderId\":\"" + m.group(1) + venueOrderIds.incrementAndGet() + "\"}");
    }

    static void stats(HttpExchange x) throws IOException {
        reply(x, 200, "{\"riskChecks\":" + riskChecks + ",\"accepted\":" + accepted
                + ",\"seqViolations\":" + seqViolations + ",\"sessions\":" + lastSeq.size() + "}");
    }

    static void pause(int ms, int jitter) {
        try {
            Thread.sleep(ms + (jitter > 0 ? ThreadLocalRandom.current().nextInt(jitter + 1) : 0));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static void reply(HttpExchange x, int code, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type", "application/json");
        x.sendResponseHeaders(code, b.length);
        try (var out = x.getResponseBody()) { out.write(b); }
    }
}
