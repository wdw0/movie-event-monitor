package br.ufes.moviemonitor.producer;

import br.ufes.moviemonitor.config.*;
import br.ufes.moviemonitor.model.MovieEvent;
import br.ufes.moviemonitor.config.Json;
import org.apache.kafka.clients.producer.ProducerRecord;

public final class MovieProducer {
    public static void main(String[] args) throws Exception {
        String key = Config.get("TMDB_API_KEY", "");
        if (key.isBlank()) throw new IllegalStateException("Set TMDB_API_KEY before starting MovieProducer");
        int interval = Config.integer("POLL_INTERVAL_MS", 30_000), pages = Config.integer("TMDB_PAGES", 1);
        TmdbClient tmdb = new TmdbClient(key); var mapper = Json.mapper();
        try (var producer = KafkaSupport.producer()) {
            System.out.println("Polling TMDB every " + interval + " ms; topic=" + Config.MOVIE_TOPIC);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    for (MovieEvent event : tmdb.popularMovies(pages)) {
                        producer.send(new ProducerRecord<>(Config.MOVIE_TOPIC, Integer.toString(event.movieId()), mapper.writeValueAsString(event))).get();
                        System.out.printf("Published %s: %s%n", event.eventType(), event.title());
                    }
                } catch (Exception e) { System.err.println("Poll failed: " + e.getMessage()); }
                Thread.sleep(interval);
            }
        }
    }
}
