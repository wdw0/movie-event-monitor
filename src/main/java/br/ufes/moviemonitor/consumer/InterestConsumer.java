package br.ufes.moviemonitor.consumer;

import br.ufes.moviemonitor.config.*;
import br.ufes.moviemonitor.model.*;
import br.ufes.moviemonitor.config.Json;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import java.time.Duration;
import java.util.*;

public final class InterestConsumer {
    public enum Mode { RATING, POPULARITY, RELEASE }
    public static void run(Mode mode) throws Exception {
        String group = switch (mode) { case RATING -> "movie-rating-monitor"; case POPULARITY -> "movie-popularity-monitor"; case RELEASE -> "movie-release-monitor"; };
        double ratingThreshold = Config.decimal("HIGH_RATING_THRESHOLD", 8.0), popularityThreshold = Config.decimal("HIGH_POPULARITY_THRESHOLD", 80.0);
        Map<Integer, MovieState> seen = new HashMap<>(); var mapper = Json.mapper();
        try (var consumer = KafkaSupport.consumer(group)) {
            consumer.subscribe(List.of(Config.MOVIE_TOPIC)); System.out.println("Group " + group + " subscribed to " + Config.MOVIE_TOPIC);
            while (true) for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                MovieEvent e = mapper.readValue(record.value(), MovieEvent.class);
                boolean match = switch (mode) {
                    case RATING -> InterestRules.highRating(e, ratingThreshold);
                    case POPULARITY -> InterestRules.highPopularity(e, popularityThreshold);
                    case RELEASE -> InterestRules.newMovie(e, seen);
                };
                if (match) {
                    String situation = switch (mode) { case RATING -> "HIGH_RATING"; case POPULARITY -> "HIGH_POPULARITY"; case RELEASE -> "NEW_MOVIE"; };
                    System.out.printf("[%s] %s | rating %.1f | popularity %.1f | votes %d | release %s (partition=%d offset=%d)%n", situation, e.title(), e.rating(), e.popularity(), e.voteCount(), e.releaseDate(), record.partition(), record.offset());
                }
            }
        }
    }
}
