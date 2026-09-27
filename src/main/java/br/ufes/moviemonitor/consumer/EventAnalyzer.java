package br.ufes.moviemonitor.consumer;

import br.ufes.moviemonitor.config.*;
import br.ufes.moviemonitor.model.*;
import br.ufes.moviemonitor.config.Json;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import java.time.Duration;
import java.util.*;

public final class EventAnalyzer {
    public static void main(String[] args) throws Exception {
        Map<Integer, MovieState> previous = new HashMap<>(); var mapper = Json.mapper();
        double popDelta = Config.decimal("TREND_POPULARITY_INCREASE", 5.0), ratingDelta = Config.decimal("TREND_RATING_INCREASE", 0.5);
        int voteDelta = Config.integer("TREND_VOTE_INCREASE", 100);
        try (var consumer = KafkaSupport.consumer("movie-event-analyzer"); var producer = KafkaSupport.producer()) {
            consumer.subscribe(List.of(Config.MOVIE_TOPIC)); System.out.println("EventAnalyzer consumes " + Config.MOVIE_TOPIC + " and produces " + Config.DERIVED_TOPIC);
            while (true) {
                var records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    MovieEvent now = mapper.readValue(record.value(), MovieEvent.class);
                    MovieState old = previous.put(now.movieId(), MovieState.from(now));
                    if (old == null || !InterestRules.trending(old, now, popDelta, voteDelta, ratingDelta)) continue;
                    DerivedEvent derived = new DerivedEvent(DerivedEvent.TRENDING, now.movieId(), now.title(), old.rating(), now.rating(), old.voteCount(), now.voteCount(), old.popularity(), now.popularity(), now.timestamp());
                    producer.send(new ProducerRecord<>(Config.DERIVED_TOPIC, Integer.toString(now.movieId()), mapper.writeValueAsString(derived))).get();
                    System.out.printf("Derived MOVIE_TRENDING: %s%n", now.title());
                }
                if (!records.isEmpty()) consumer.commitSync();
            }
        }
    }
}
