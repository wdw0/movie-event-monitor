package br.ufes.moviemonitor.consumer;

import br.ufes.moviemonitor.model.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

class InterestRulesTest {
    private MovieEvent event(int id, double rating, int votes, double popularity) {
        return new MovieEvent(MovieEvent.TYPE, id, "Film", "2026-01-01", rating, votes, popularity, Instant.EPOCH);
    }
    @Test void detectsHighRatingAtConfiguredThreshold() { assertTrue(InterestRules.highRating(event(1, 8, 10, 20), 8)); assertFalse(InterestRules.highRating(event(1, 7.9, 10, 20), 8)); }
    @Test void detectsHighPopularityAtConfiguredThreshold() { assertTrue(InterestRules.highPopularity(event(1, 5, 10, 80), 80)); assertFalse(InterestRules.highPopularity(event(1, 5, 10, 79.9), 80)); }
    @Test void detectsFirstObservationOnly() { var seen = new HashMap<Integer, MovieState>(); var e = event(1, 5, 2, 3); assertTrue(InterestRules.newMovie(e, seen)); assertFalse(InterestRules.newMovie(e, seen)); }
    @Test void trendingWhenRatingPopularityAndVotesIncrease() { var old = new MovieState(7, 100, 20); assertTrue(InterestRules.trending(old, event(1, 7.6, 200, 26), 5, 100, .5)); }
    @Test void trendingWhenRatingAndPopularityIncreaseEvenIfVotesDoNot() { var old = new MovieState(7, 100, 20); assertTrue(InterestRules.trending(old, event(1, 7.6, 100, 25), 5, 100, .5)); }
    @Test void doesNotTrendWhenOnlyRatingIncreases() { var old = new MovieState(7, 100, 20); assertFalse(InterestRules.trending(old, event(1, 7.6, 100, 20), 5, 100, .5)); }
}
