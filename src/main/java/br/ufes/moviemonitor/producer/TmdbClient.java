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
                    movie.path("popularity").asDouble(), Instant.now()));
        }
        return events;
    }
}
