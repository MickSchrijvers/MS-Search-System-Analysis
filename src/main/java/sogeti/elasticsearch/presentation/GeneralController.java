package sogeti.elasticsearch.presentation;

import co.elastic.clients.util.DateTime;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import sogeti.elasticsearch.application.GeneralService;
import sogeti.elasticsearch.domain.HttpRequest;
import sogeti.elasticsearch.domain.TaxiRequest;
import sogeti.elasticsearch.domain.WikipediaRequest;
import sogeti.elasticsearch.presentation.dto.PostBulkIndexDTO;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/elasticsearch")
@RequiredArgsConstructor
@Validated
@Slf4j
public class GeneralController {
    private final GeneralService service;

    @PostMapping
    @Operation(summary = "Post request for specific dataset to be bulk indexed, options being: TAXI, WIKIPEDIA and HTTP.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Request successfully received."),
            @ApiResponse(responseCode = "400", description = "Request failed, check typing.")
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void bulkIndex(@Valid @RequestBody PostBulkIndexDTO postBulkIndexDTO) throws IOException {
        this.service.bulkIndex(postBulkIndexDTO);
    }

    @GetMapping("/http")
    @Operation(summary = "Get request for searching specific Http request documents")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request successfully received and processed.",
             content = {@Content(mediaType = "application/json",
             array = @ArraySchema(schema = @Schema(implementation = HttpRequest.class)))}),
            @ApiResponse(responseCode = "400", description = "Request failed, check request parameters.")
    })
    public ResponseEntity<List<HttpRequest>> searchHttpRequests(
            @Parameter(description = "Http request timestamp")
            @RequestParam(required = false) Optional<Long> timestamp,

            @Parameter(description = "Http request clientip")
            @RequestParam(required = false) Optional<String> clientip,

            @Parameter(description = "Http request url ")
            @RequestParam(required = false) Optional<String> request,

            @Parameter(description = "Http request size")
            @RequestParam(required = false) Optional<Long> size,

            @Parameter(description = "Http request status")
            @RequestParam(required = false) Optional<Long> status
    ) throws IOException {
        return ResponseEntity.ok(this.service.searchHttpRequests(
                timestamp,
                clientip,
                request,
                size,
                status)
        );
    }

    @GetMapping("/http/test")
    @Operation(summary = "Get request for the http test use case: this use case indexes the http.json file to a newly created Elasticsearch http index and returns results with a request url containing 'space.gif' and a status code of '404'.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request successfully received and processed.",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = HttpRequest.class))
                    )}
            ),
            @ApiResponse(responseCode = "400", description = "Request failed, check request parameters.")
    })
    public ResponseEntity<List<HttpRequest>> httpTestRequest() throws IOException {
        return ResponseEntity.ok(service.httpTestRequest());
    }

    @GetMapping("/taxi")
    @Operation(summary = "Get request for searching specific Taxi request documents")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request successfully received and processed.",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TaxiRequest.class))
                    )}
            ),
            @ApiResponse(responseCode = "400", description = "Request failed, check request parameters.")
    })
    public ResponseEntity<List<TaxiRequest>> searchTaxiRequests(
            @Parameter(description = "Vendor identifier")
            @RequestParam(required = false) Optional<Integer> vendorId,

            @Parameter(description = "Pickup date-time")
            @RequestParam(required = false) Optional<DateTime> pickupDateTime,

            @Parameter(description = "Drop-off date-time")
            @RequestParam(required = false) Optional<DateTime> dropoffDateTime,

            @Parameter(description = "Number of passengers")
            @RequestParam(required = false) Optional<Integer> passengerCount,

            @Parameter(description = "Trip distance in miles")
            @RequestParam(required = false) Optional<Double> tripDistance,

            @Parameter(description = "Rate code identifier")
            @RequestParam(required = false) Optional<Integer> rateCodeId,

            @Parameter(description = "Store and forward flag (Y/N)")
            @RequestParam(required = false) Optional<String> storeAndFwdFlag,

            @Parameter(description = "Pickup location code")
            @RequestParam(required = false) Optional<String> pickupLocation,

            @Parameter(description = "Drop-off location code")
            @RequestParam(required = false) Optional<String> dropoffLocation,

            @Parameter(description = "Payment type code")
            @RequestParam(required = false) Optional<String> paymentType,

            @Parameter(description = "Fare amount")
            @RequestParam(required = false) Optional<Double> fareAmount,

            @Parameter(description = "Extra charges")
            @RequestParam(required = false) Optional<Double> extra,

            @Parameter(description = "MTA tax amount")
            @RequestParam(required = false) Optional<Double> mtaTax,

            @Parameter(description = "Tip amount")
            @RequestParam(required = false) Optional<Double> tipAmount,

            @Parameter(description = "Tolls amount")
            @RequestParam(required = false) Optional<Double> tollsAmount,

            @Parameter(description = "Improvement surcharge")
            @RequestParam(required = false) Optional<Double> improvementSurcharge,

            @Parameter(description = "Total trip amount")
            @RequestParam(required = false) Optional<Double> totalAmount
    ) throws IOException {
        return ResponseEntity.ok(service.searchTaxiRequests(
                        vendorId,
                        pickupDateTime,
                        dropoffDateTime,
                        passengerCount,
                        tripDistance,
                        rateCodeId,
                        storeAndFwdFlag,
                        pickupLocation,
                        dropoffLocation,
                        paymentType,
                        fareAmount,
                        extra,
                        mtaTax,
                        tipAmount,
                        tollsAmount,
                        improvementSurcharge,
                        totalAmount)
        );
    }

    @GetMapping("/taxi/test")
    @Operation(summary = "Get request for the taxi test use case: this use case indexes the taxi.json file to a newly created Elasticsearch taxi index and returns results with a pickup location equal to '231' and a fare amount of '17.5'.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request successfully received and processed.",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TaxiRequest.class))
                    )}
            ),
            @ApiResponse(responseCode = "400", description = "Request failed, check request parameters.")
    })
    public ResponseEntity<List<TaxiRequest>> taxiTestRequest() throws IOException {
        return ResponseEntity.ok(service.taxiTestRequest());
    }

    @GetMapping("/wikipedia")
    @Operation(summary = "Get request for searching specific Wikipedia request documents")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request successfully received and processed.",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = WikipediaRequest.class))
                    )}
            ),
            @ApiResponse(responseCode = "400", description = "Request failed, check request parameters.")
    })
    public ResponseEntity<List<WikipediaRequest>> searchWikipediaRequests(
            @Parameter(description = "Content text")
            @RequestParam(required = false) Optional<String> content,

            @Parameter(description = "Index id")
            @RequestParam(required = false) Optional<String> id,

            @Parameter(description = "Index title")
            @RequestParam(required = false) Optional<String> index,

            @Parameter(description = "Namespace")
            @RequestParam(required = false) Optional<String> namespace,

            @Parameter(description = "Redirect URL")
            @RequestParam(required = false) Optional<String> redirect,

            @Parameter(description = "Title")
            @RequestParam(required = false) Optional<String> title
    ) throws IOException {
        return ResponseEntity.ok(service.searchWikipediaRequests(
                content,
                id,
                index,
                namespace,
                redirect,
                title)
        );
    }

    @GetMapping("/wikipedia/test")
    @Operation(summary = "Get request for the wikipedia test use case: this use case indexes the wikipedia.json file to a newly created Elasticsearch wikipedia index and returns results with a title containing 'Royal Navy'.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request successfully received and processed.",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = WikipediaRequest.class))
                    )}
            ),
            @ApiResponse(responseCode = "400", description = "Request failed, check request parameters.")
    })
    public ResponseEntity<List<WikipediaRequest>> wikipediaTestRequest() throws IOException {
        return ResponseEntity.ok(service.wikipediaTestRequest());
    }

}
