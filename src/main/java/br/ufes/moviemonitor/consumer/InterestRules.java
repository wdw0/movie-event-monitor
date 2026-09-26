package br.ufes.moviemonitor.consumer;

import br.ufes.moviemonitor.model.*;
import java.util.Map;

public final class InterestRules {
    private InterestRules() {}
    public static boolean highRating(MovieEvent e, double threshold) { return e.rating() >= threshold; }
    public static boolean highPopularity(MovieEvent e, double threshold) { return e.popularity() >= threshold; }
    public static boolean newMovie(MovieEvent e, Map<Integer, MovieState> seen) { return seen.putIfAbsent(e.movieId(), MovieState.from(e)) == null; }
    public static boolean trending(MovieState old, MovieEvent now, double popularityDelta, int voteDelta, double ratingDelta) {
        int increased = (now.popularity() - old.popularity() >= popularityDelta ? 1 : 0)
                + (now.voteCount() - old.voteCount() >= voteDelta ? 1 : 0)
                + (now.rating() - old.rating() >= ratingDelta ? 1 : 0);
        return increased >= 2;
    }
}
