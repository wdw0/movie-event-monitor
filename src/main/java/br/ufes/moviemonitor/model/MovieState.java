package br.ufes.moviemonitor.model;

public record MovieState(double rating, int voteCount, double popularity) {
    public static MovieState from(MovieEvent event) { return new MovieState(event.rating(), event.voteCount(), event.popularity()); }
}
