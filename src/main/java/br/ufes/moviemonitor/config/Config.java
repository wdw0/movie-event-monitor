package br.ufes.moviemonitor.config;

public final class Config {
    private Config() {}
    public static String get(String name, String fallback) { return System.getenv().getOrDefault(name, fallback); }
    public static int integer(String name, int fallback) { return Integer.parseInt(get(name, Integer.toString(fallback))); }
    public static double decimal(String name, double fallback) { return Double.parseDouble(get(name, Double.toString(fallback))); }
    public static final String BOOTSTRAP = get("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092,localhost:9094,localhost:9096");
    public static final String MOVIE_TOPIC = get("MOVIE_EVENTS_TOPIC", "movie-events");
    public static final String DERIVED_TOPIC = get("MOVIE_DERIVED_EVENTS_TOPIC", "movie-derived-events");
}
