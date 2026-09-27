package br.ufes.moviemonitor.producer;

import br.ufes.moviemonitor.model.MovieEvent;
import br.ufes.moviemonitor.config.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

public final class TmdbClient {
    private static final Map<Integer, String> GENRES = Map.ofEntries(
            Map.entry(28, "Action"), Map.entry(12, "Adventure"), Map.entry(16, "Animation"),
            Map.entry(35, "Comedy"), Map.entry(80, "Crime"), Map.entry(99, "Documentary"),
            Map.entry(18, "Drama"), Map.entry(10751, "Family"), Map.entry(14, "Fantasy"),
            Map.entry(36, "History"), Map.entry(27, "Horror"), Map.entry(10402, "Music"),
            Map.entry(9648, "Mystery"), Map.entry(10749, "Romance"), Map.entry(878, "Science Fiction"),
            Map.entry(10770, "TV Movie"), Map.entry(53, "Thriller"), Map.entry(10752, "War"),
            Map.entry(37, "Western"));
    private final String apiKey;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = Json.mapper();
    public TmdbClient(String apiKey) { this.apiKey = Objects.requireNonNull(apiKey); }
    public List<MovieEvent> popularMovies(int pages) throws Exception {
        List<MovieEvent> events = new ArrayList<>();
        for (int page = 1; page <= pages; page++) {
            String url = "https://api.themoviedb.org/3/discover/movie?api_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                    + "&include_adult=false&include_video=false&language=en-US&page=" + page + "&sort_by=popularity.desc";
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("accept", "application/json").GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) throw new IllegalStateException("TMDB returned HTTP " + response.statusCode());
            JsonNode results = mapper.readTree(response.body()).path("results");
            for (JsonNode movie : results) events.add(new MovieEvent(MovieEvent.TYPE, movie.path("id").asInt(),
                    movie.path("title").asText("Unknown"), movie.path("release_date").asText(""),
                    movie.path("vote_average").asDouble(), movie.path("vote_count").asInt(),
                    movie.path("popularity").asDouble(), Instant.now(), genreNames(movie.path("genre_ids"))));
        }
        return events;
    }

    static List<String> genreNames(JsonNode genreIds) {
        List<String> names = new ArrayList<>();
        if (genreIds.isArray()) for (JsonNode id : genreIds) {
            String name = GENRES.get(id.asInt());
            if (name != null) names.add(name);
        }
        return List.copyOf(names);
    }
}
