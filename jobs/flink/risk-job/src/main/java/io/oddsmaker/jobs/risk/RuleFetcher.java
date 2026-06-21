package io.oddsmaker.jobs.risk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

public class RuleFetcher implements Runnable {
    private static final AtomicReference<RuleFetcher> instance = new AtomicReference<>();

    public static void startOnce(String controlUrl, String gameId, String adminToken, long intervalMs) {
        if (instance.get() == null) {
            RuleFetcher fetcher = new RuleFetcher(controlUrl, gameId, adminToken, intervalMs);
            if (instance.compareAndSet(null, fetcher)) {
                fetcher.start();
            }
        }
    }

    private final String controlUrl;
    private final String gameId;
    private final String adminToken;
    private final long intervalMs;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile boolean running = true;
    private Thread thread;

    public RuleFetcher(String controlUrl, String gameId, String adminToken, long intervalMs) {
        this.controlUrl = controlUrl.replaceAll("/$", "");
        this.gameId = gameId;
        this.adminToken = adminToken;
        this.intervalMs = intervalMs;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void start() {
        thread = new Thread(this, "oddsmaker-rule-fetcher");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        if (thread != null) thread.interrupt();
    }

    @Override
    public void run() {
        while (running) {
            try {
                fetchAndApply();
            } catch (Exception e) {
                System.err.println("[rule-fetcher] failed: " + e.getMessage());
            }
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void fetchAndApply() throws Exception {
        String url = controlUrl + "/api/risk-dashboard/rules/" + gameId;
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET();
        if (adminToken != null && !adminToken.isEmpty()) {
            reqBuilder.header("x-admin-token", adminToken);
        }
        HttpResponse<String> resp = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            System.err.println("[rule-fetcher] HTTP " + resp.statusCode() + ", keeping existing rules");
            return;
        }

        JsonNode arr = mapper.readTree(resp.body());
        if (!arr.isArray() || arr.isEmpty()) return;

        RuleConfig base = RuleConfig.current();
        BigDecimal amountThreshold = base.amountThreshold;
        long freqMaxEvents = base.freqMaxEvents;
        BigDecimal velocityMax = base.velocityMax;
        BigDecimal ratioMax = base.ratioMax;

        for (JsonNode rule : arr) {
            String type = rule.path("ruleType").asText("");
            int threshold = rule.path("triggerThreshold").asInt(0);
            if (threshold <= 0) continue;
            switch (type) {
                case "THRESHOLD":
                    amountThreshold = BigDecimal.valueOf(threshold);
                    break;
                case "FREQUENCY":
                    freqMaxEvents = threshold;
                    break;
                case "VELOCITY":
                    velocityMax = BigDecimal.valueOf(threshold);
                    break;
                case "RATIO":
                    ratioMax = BigDecimal.valueOf(threshold);
                    break;
                default:
                    break;
            }
        }

        RuleConfig.update(new RuleConfig(amountThreshold, freqMaxEvents, velocityMax, ratioMax));
        System.out.println("[rule-fetcher] rules refreshed from " + url + " (" + arr.size() + " rules)");
    }
}
