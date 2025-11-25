package sogeti.elasticsearch.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HttpRequest(
        @JsonProperty("@timestamp") Long timestamp,
        @JsonProperty("clientip") String clientip,
        @JsonProperty("request") String request,
        @JsonProperty("status") Long size,
        @JsonProperty("size") Long status) {
}
