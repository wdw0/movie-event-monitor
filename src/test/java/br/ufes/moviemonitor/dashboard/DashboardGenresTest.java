package br.ufes.moviemonitor.dashboard;

import br.ufes.moviemonitor.config.Json;
import br.ufes.moviemonitor.model.MovieEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardGenresTest {
    @Test void keepsGenresInDashboardProjection() {
        var state = new DashboardState();
        state.acceptMovie(new MovieEvent(MovieEvent.TYPE, 7, "Film", "2026-01-01", 8.1, 10,
                20.0, Instant.EPOCH, List.of("Action", "Science Fiction")));
        assertEquals(List.of("Action", "Science Fiction"), state.snapshot().movies().get(0).genres());
    }

    @Test void oldKafkaPayloadWithoutGenresRemainsCompatible() throws Exception {
        MovieEvent oldPayload = Json.mapper().readValue("""
                {"eventType":"MOVIE_UPDATE","movieId":8,"title":"Old Film","releaseDate":"2020-01-01",
                 "rating":7.0,"voteCount":3,"popularity":4.0,"timestamp":"2026-01-01T00:00:00Z"}
                """, MovieEvent.class);
        var state = new DashboardState();
        state.acceptMovie(oldPayload);
        assertEquals(List.of(), state.snapshot().movies().get(0).genres());
    }
}
