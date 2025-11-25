package sogeti.elasticsearch.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WikipediaIndex(
        @JsonProperty("_id") String id,
        @JsonProperty("_index") String index
) {
}
