package br.ufes.moviemonitor.model;

import java.time.Instant;

public record DerivedEvent(String eventType, int movieId, String title,
                           double previousRating, double currentRating,
                           int previousVoteCount, int currentVoteCount,
                           double previousPopularity, double currentPopularity, Instant timestamp) {
    public static final String TRENDING = "MOVIE_TRENDING";
}
