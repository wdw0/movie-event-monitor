const test = require('node:test');
const assert = require('node:assert/strict');
const { getRecommendations } = require('../../main/resources/dashboard/recommendations.js');

const movies = [
  { movieId: 1, title: 'Dune', genres: ['Science Fiction', 'Adventure', 'Action'], popularity: 90 },
  { movieId: 2, title: 'Movie B', genres: ['Science Fiction', 'Action'], popularity: 40 },
  { movieId: 3, title: 'Movie C', genres: ['Science Fiction', 'Adventure', 'Action'], popularity: 30 },
  { movieId: 4, title: 'Movie D', genres: ['Comedy', 'Romance'], popularity: 99 },
  { movieId: 5, title: 'Movie E', genres: ['Comedy'], popularity: 70, trending: true },
  { movieId: 6, title: 'Movie F', genres: ['Mystery'], popularity: 50, trending: true }
];

test('recommends movies with shared genres and ranks more overlap higher', () => {
  const result = getRecommendations(movies, [1]);
  assert.equal(result[0].movieId, 3);
  assert.equal(result[0].recommendationScore, 3);
  assert.match(result[0].reason, /Mesmo gênero de Dune/);
  assert.equal(result.find(movie => movie.movieId === 2).recommendationScore, 2);
});

test('adds the trending bonus and explains it', () => {
  const result = getRecommendations(movies, [1]);
  const trending = result.find(movie => movie.movieId === 6);
  assert.equal(trending.recommendationScore, 1);
  assert.equal(trending.reason, 'Em tendência');
});

test('excludes movies already liked', () => {
  assert.ok(!getRecommendations(movies, [1, 2]).some(movie => movie.movieId === 2));
});

test('uses trending movies as fallback when there are no likes', () => {
  const result = getRecommendations(movies, []);
  assert.deepEqual(result.map(movie => movie.movieId), [5, 6]);
  assert.ok(result.every(movie => movie.fallback && movie.reason === 'Em tendência'));
});

test('does not recommend unrelated non-trending movies', () => {
  const result = getRecommendations(movies, [1]);
  assert.ok(!result.some(movie => movie.movieId === 4));
});
