package sogeti.elasticsearch.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PostBulkIndexDTO(
        @NotNull @NotBlank String type
) {
}
