package sogeti.elasticsearch.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WikipediaRequest(
        @JsonProperty("content") String content,
        @JsonProperty("index") WikipediaIndex index,
        @JsonProperty("namespace") String namespace,
        @JsonProperty("redirect") String redirect,
        @JsonProperty("title") String title) {
}
