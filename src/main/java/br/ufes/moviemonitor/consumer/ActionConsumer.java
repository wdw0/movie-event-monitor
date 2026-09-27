package br.ufes.moviemonitor.consumer;

import br.ufes.moviemonitor.config.*;
import br.ufes.moviemonitor.model.DerivedEvent;
import br.ufes.moviemonitor.config.Json;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import java.time.Duration;
import java.util.List;

public final class ActionConsumer {
    public static void main(String[] args) throws Exception {
        var mapper = Json.mapper();
        try (var consumer = KafkaSupport.consumer("movie-action-consumer")) {
            consumer.subscribe(List.of(Config.DERIVED_TOPIC)); System.out.println("ActionConsumer subscribed to " + Config.DERIVED_TOPIC);
            while (true) {
                var records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    DerivedEvent e = mapper.readValue(record.value(), DerivedEvent.class);
                    System.out.printf("========================================%nMOVIE TRENDING DETECTED%nMovie: %s%nRating: %.1f -> %.1f%nPopularity: %.1f -> %.1f%nVote Count: %d -> %d%n========================================%n", e.title(), e.previousRating(), e.currentRating(), e.previousPopularity(), e.currentPopularity(), e.previousVoteCount(), e.currentVoteCount());
                }
                if (!records.isEmpty()) consumer.commitSync();
            }
        }
    }
}
