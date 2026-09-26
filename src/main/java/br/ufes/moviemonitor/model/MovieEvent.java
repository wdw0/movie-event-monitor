package br.ufes.moviemonitor.model;

import java.time.Instant;

public record MovieEvent(String eventType, int movieId, String title, String releaseDate,
                         double rating, int voteCount, double popularity, Instant timestamp) {
    public static final String TYPE = "MOVIE_UPDATE";
}
