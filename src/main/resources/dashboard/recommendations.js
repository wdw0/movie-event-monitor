(function (root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  if (root) root.MovieRecommendations = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  const LIMIT = 10;
  const TRENDING_BONUS = 1;

  /**
   * Counts distinct candidate genres shared with liked movies. A trending
   * candidate gets a fixed +1 bonus, so the ranking is genreScore + bonus.
   */
  function getRecommendations(movies, likedMovieIds) {
    const likedIds = new Set(Array.from(likedMovieIds || [], Number));
    const likedMovies = (movies || []).filter(movie => likedIds.has(Number(movie.movieId)));
    const hasLikes = likedIds.size > 0;
    const candidates = (movies || []).filter(movie => !likedIds.has(Number(movie.movieId)));

    if (!hasLikes) {
      return candidates.filter(movie => movie.trending === true)
        .map(movie => ({ ...movie, recommendationScore: TRENDING_BONUS, reason: 'Em tendência', fallback: true }))
        .sort(compareRecommendations).slice(0, LIMIT);
    }

    return candidates.map(movie => {
      const candidateGenres = new Set(movie.genres || []);
      const matchingLikedMovies = likedMovies.filter(liked =>
        (liked.genres || []).some(genre => candidateGenres.has(genre)));
      const sharedGenres = new Set((movie.genres || []).filter(genre =>
        likedMovies.some(liked => (liked.genres || []).includes(genre))));
      const genreScore = sharedGenres.size;
      const trendingBonus = movie.trending === true ? TRENDING_BONUS : 0;
      const reasons = [];
      if (matchingLikedMovies.length) {
        reasons.push(`Mesmo gênero de ${matchingLikedMovies.map(liked => liked.title).join(' e ')}`);
      }
      if (trendingBonus) reasons.push('Em tendência');
      return { ...movie, recommendationScore: genreScore + trendingBonus, reason: reasons.join(' • '), fallback: false };
    }).filter(movie => movie.recommendationScore > 0)
      .sort(compareRecommendations).slice(0, LIMIT);
  }

  function compareRecommendations(a, b) {
    return b.recommendationScore - a.recommendationScore
      || Number(b.popularity || 0) - Number(a.popularity || 0)
      || String(a.title).localeCompare(String(b.title))
      || Number(a.movieId) - Number(b.movieId);
  }

  return Object.freeze({ getRecommendations, LIMIT, TRENDING_BONUS });
});
