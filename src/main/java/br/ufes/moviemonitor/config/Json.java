package br.ufes.moviemonitor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class Json {
    private Json() {}
    public static ObjectMapper mapper() { return new ObjectMapper().registerModule(new JavaTimeModule()); }
}
