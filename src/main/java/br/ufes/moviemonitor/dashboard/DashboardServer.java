package br.ufes.moviemonitor.dashboard;

import br.ufes.moviemonitor.config.Config;
import br.ufes.moviemonitor.config.Json;
import br.ufes.moviemonitor.model.DerivedEvent;
import br.ufes.moviemonitor.model.MovieEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Read-only HTTP view backed by an independent Kafka consumer group. */
public final class DashboardServer {
    private static final List<String> EXPECTED_GROUPS = List.of("movie-rating-monitor", "movie-popularity-monitor",
            "movie-release-monitor", "movie-event-analyzer", "movie-action-consumer", "movie-dashboard");

    private DashboardServer() {}

    public static void main(String[] args) throws Exception {
        DashboardState state = new DashboardState();
        Thread consumerThread = new Thread(() -> consume(state), "movie-dashboard-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();

        int port = Config.integer("DASHBOARD_PORT", 8080);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        ObjectMapper mapper = Json.mapper();
        server.createContext("/api/health", exchange -> respond(exchange, mapper, Map.of("status", kafkaOnline(), "consumerGroup", "movie-dashboard")));
        server.createContext("/api/snapshot", exchange -> respond(exchange, mapper, Map.of("data", state.snapshot(), "kafka", kafkaSnapshot())));
        server.createContext("/", DashboardServer::serveIndex);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Dashboard available at http://localhost:" + port);
    }

    private static void consume(DashboardState state) {
        ObjectMapper mapper = Json.mapper();
        try (var consumer = br.ufes.moviemonitor.config.KafkaSupport.consumer("movie-dashboard")) {
            consumer.subscribe(List.of(Config.MOVIE_TOPIC, Config.DERIVED_TOPIC));
            System.out.println("Dashboard consumer subscribed to " + Config.MOVIE_TOPIC + " and " + Config.DERIVED_TOPIC);
            while (!Thread.currentThread().isInterrupted()) {
                var records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    if (record.topic().equals(Config.MOVIE_TOPIC)) state.acceptMovie(mapper.readValue(record.value(), MovieEvent.class));
                    else if (record.topic().equals(Config.DERIVED_TOPIC)) state.acceptDerived(mapper.readValue(record.value(), DerivedEvent.class));
                }
                if (!records.isEmpty()) consumer.commitSync();
            }
        } catch (Exception e) {
            System.err.println("Dashboard Kafka consumer stopped: " + e.getMessage());
        }
    }

    private static Map<String, Object> kafkaSnapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> brokers = new ArrayList<>();
        List<Map<String, Object>> topics = new ArrayList<>();
        Map<String, String> groups = new LinkedHashMap<>();
        try (AdminClient admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, Config.BOOTSTRAP,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 2000,
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 3000))) {
            try {
                var nodes = admin.describeCluster().nodes().get(3, TimeUnit.SECONDS);
                nodes.stream().sorted(Comparator.comparingInt(org.apache.kafka.common.Node::id))
                        .forEach(node -> brokers.add(Map.of("id", node.id(), "host", node.host(), "port", node.port(), "status", "ONLINE")));
                result.put("online", !nodes.isEmpty());
            } catch (Exception e) {
                result.put("online", false);
            }
            if (Boolean.TRUE.equals(result.get("online"))) {
                try {
                    var topicDescriptions = admin.describeTopics(List.of(Config.MOVIE_TOPIC, Config.DERIVED_TOPIC))
                            .allTopicNames().get(3, TimeUnit.SECONDS);
                    for (String topicName : List.of(Config.MOVIE_TOPIC, Config.DERIVED_TOPIC)) {
                        var topic = topicDescriptions.get(topicName);
                        if (topic != null) {
                            int replication = topic.partitions().stream().mapToInt(p -> p.replicas().size()).max().orElse(0);
                            topics.add(Map.of("name", topicName, "partitions", topic.partitions().size(), "replication", replication));
                        }
                    }
                } catch (Exception ignored) {
                    // Cluster connectivity is reported separately from topic metadata availability.
                }
                try {
                    Set<String> activeGroups = new HashSet<>();
                    admin.listConsumerGroups().all().get(3, TimeUnit.SECONDS).forEach(g -> activeGroups.add(g.groupId()));
                    for (String group : EXPECTED_GROUPS) groups.put(group, activeGroups.contains(group) ? "SEEN" : "NOT SEEN");
                } catch (Exception ignored) {
                    for (String group : EXPECTED_GROUPS) groups.put(group, "UNKNOWN");
                }
            }
        } catch (Exception e) {
            result.put("online", false);
        }
        for (String group : EXPECTED_GROUPS) groups.putIfAbsent(group, "UNKNOWN");
        result.put("brokers", brokers);
        result.put("topics", topics);
        result.put("consumerGroups", groups);
        return result;
    }

    private static String kafkaOnline() {
        return Boolean.TRUE.equals(kafkaSnapshot().get("online")) ? "ONLINE" : "OFFLINE";
    }

    private static void respond(HttpExchange exchange, ObjectMapper mapper, Object body) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        byte[] bytes = mapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var out = exchange.getResponseBody()) { out.write(bytes); }
    }

    private static void serveIndex(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        String path = exchange.getRequestURI().getPath();
        if (!path.equals("/") && !path.equals("/index.html")) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        try (var input = DashboardServer.class.getResourceAsStream("/dashboard/index.html")) {
            if (input == null) { exchange.sendResponseHeaders(404, -1); return; }
            byte[] bytes = input.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) { out.write(bytes); }
        }
    }
}
