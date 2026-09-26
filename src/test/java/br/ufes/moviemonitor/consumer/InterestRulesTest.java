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
    @Test void detectsThresholds() { var e = event(1, 8, 10, 80); assertTrue(InterestRules.highRating(e, 8)); assertTrue(InterestRules.highPopularity(e, 80)); }
    @Test void detectsFirstObservationOnly() { var seen = new HashMap<Integer, MovieState>(); var e = event(1, 5, 2, 3); assertTrue(InterestRules.newMovie(e, seen)); assertFalse(InterestRules.newMovie(e, seen)); }
    @Test void detectsGrowthInAtLeastTwoMetrics() { var old = new MovieState(7, 100, 20); assertTrue(InterestRules.trending(old, event(1, 7.6, 150, 26), 5, 100, .5)); assertFalse(InterestRules.trending(old, event(1, 7.6, 120, 22), 5, 100, .5)); }
}
