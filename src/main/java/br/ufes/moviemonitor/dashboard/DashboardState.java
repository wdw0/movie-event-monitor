package br.ufes.moviemonitor.dashboard;

import br.ufes.moviemonitor.config.Config;
import br.ufes.moviemonitor.consumer.InterestRules;
import br.ufes.moviemonitor.model.DerivedEvent;
import br.ufes.moviemonitor.model.MovieEvent;
import br.ufes.moviemonitor.model.MovieState;

import java.time.Instant;
import java.util.*;

/** Volatile projection for the dashboard. Kafka remains the event source of truth. */
public final class DashboardState {
    private static final int MAX_EVENTS = 100;
    private static final int MAX_POINTS = 40;
    private final Map<Integer, MovieRow> movies = new HashMap<>();
    private final Deque<EventRow> events = new ArrayDeque<>();
    private final Deque<PopularityPoint> popularity = new ArrayDeque<>();
    private final Map<String, Long> eventCounts = new HashMap<>();
    private final Set<Integer> firstSeen = new HashSet<>();
    private long primitiveEvents;
    private long derivedEvents;
    private long newMovies;
    private long trendingMovies;

    public synchronized void acceptMovie(MovieEvent event) {
        primitiveEvents++;
        eventCounts.merge(event.eventType(), 1L, Long::sum);
        boolean isNew = firstSeen.add(event.movieId());
        if (isNew) newMovies++;
        double ratingThreshold = Config.decimal("HIGH_RATING_THRESHOLD", 8.0);
        double popularityThreshold = Config.decimal("HIGH_POPULARITY_THRESHOLD", 80.0);
        MovieRow previous = movies.get(event.movieId());
        boolean trending = previous != null && InterestRules.trending(
                new MovieState(previous.rating(), previous.votes(), previous.popularity()), event,
                Config.decimal("TREND_POPULARITY_INCREASE", 5.0),
                Config.integer("TREND_VOTE_INCREASE", 100),
                Config.decimal("TREND_RATING_INCREASE", 0.5));
        movies.put(event.movieId(), new MovieRow(event.movieId(), event.title(), event.rating(), event.voteCount(),
                event.popularity(), event.timestamp(), isNew, event.rating() >= ratingThreshold,
                event.popularity() >= popularityThreshold, trending));
        appendEvent(event.eventType(), event.movieId(), event.title(), event.timestamp());
        if (isNew) recordSignal("NEW_MOVIE", event);
        if (event.rating() >= ratingThreshold) recordSignal("HIGH_RATING", event);
        if (event.popularity() >= popularityThreshold) recordSignal("HIGH_POPULARITY", event);
        double average = movies.values().stream().mapToDouble(MovieRow::popularity).average().orElse(0);
        popularity.addLast(new PopularityPoint(event.timestamp(), average));
        while (popularity.size() > MAX_POINTS) popularity.removeFirst();
    }

    private void recordSignal(String type, MovieEvent event) {
        eventCounts.merge(type, 1L, Long::sum);
        appendEvent(type, event.movieId(), event.title(), event.timestamp());
    }

    public synchronized void acceptDerived(DerivedEvent event) {
        derivedEvents++;
        eventCounts.merge(event.eventType(), 1L, Long::sum);
        if (DerivedEvent.TRENDING.equals(event.eventType())) {
            trendingMovies++;
            MovieRow movie = movies.get(event.movieId());
            if (movie != null) movies.put(event.movieId(), movie.withTrending(true));
        }
        appendEvent(event.eventType(), event.movieId(), event.title(), event.timestamp());
    }

    private void appendEvent(String type, int movieId, String title, Instant timestamp) {
        events.addFirst(new EventRow(type, movieId, title, timestamp));
        while (events.size() > MAX_EVENTS) events.removeLast();
    }

    public synchronized DashboardSnapshot snapshot() {
        List<MovieRow> sortedMovies = movies.values().stream()
                .sorted(Comparator.comparingDouble(MovieRow::popularity).reversed()).toList();
        return new DashboardSnapshot(primitiveEvents + derivedEvents, movies.size(), trendingMovies, newMovies,
                sortedMovies, List.copyOf(events), Map.copyOf(eventCounts), List.copyOf(popularity));
    }

    public record MovieRow(int movieId, String title, double rating, int votes, double popularity,
                           Instant lastUpdate, boolean newMovie, boolean highRating,
                           boolean highPopularity, boolean trending) {
        MovieRow withTrending(boolean value) {
            return new MovieRow(movieId, title, rating, votes, popularity, lastUpdate, newMovie,
                    highRating, highPopularity, value);
        }
    }
    public record EventRow(String type, int movieId, String title, Instant timestamp) {}
    public record PopularityPoint(Instant timestamp, double averagePopularity) {}
    public record DashboardSnapshot(long events, int moviesTracked, long trending, long newMovies,
                                    List<MovieRow> movies, List<EventRow> recentEvents,
                                    Map<String, Long> eventCounts, List<PopularityPoint> popularityHistory) {}
}
