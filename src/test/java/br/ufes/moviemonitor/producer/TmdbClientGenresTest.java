package br.ufes.moviemonitor.producer;

import br.ufes.moviemonitor.config.Json;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TmdbClientGenresTest {
    @Test void mapsTmdbGenreIdsToReadableNames() throws Exception {
        var ids = Json.mapper().readTree("[28,878,12,999999]");
        assertEquals(java.util.List.of("Action", "Science Fiction", "Adventure"), TmdbClient.genreNames(ids));
    }
}
