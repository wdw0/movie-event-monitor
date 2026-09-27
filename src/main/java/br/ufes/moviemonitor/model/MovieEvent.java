package br.ufes.moviemonitor.model;

import java.time.Instant;
import java.util.List;

public record MovieEvent(String eventType, int movieId, String title, String releaseDate,
                         double rating, int voteCount, double popularity, Instant timestamp,
                         List<String> genres) {
    public static final String TYPE = "MOVIE_UPDATE";

    /** Keeps source compatibility with events created before genres were added. */
    public MovieEvent(String eventType, int movieId, String title, String releaseDate,
                      double rating, int voteCount, double popularity, Instant timestamp) {
        this(eventType, movieId, title, releaseDate, rating, voteCount, popularity, timestamp, List.of());
    }
}
