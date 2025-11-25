package sogeti.elasticsearch.domain;

import co.elastic.clients.util.DateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TaxiRequest(
        @JsonProperty("vendor_id") Integer vendorId,
        @JsonProperty("pickup_datetime") DateTime pickupDateTime,
        @JsonProperty("dropoff_datetime") DateTime dropoffDateTime,
        @JsonProperty("passenger_count") Integer passengerCount,
        @JsonProperty("trip_distance") Double tripDistance,
        @JsonProperty("rate_code_id") Integer rateCodeId,
        @JsonProperty("store_and_fwd_flag") String storeAndFwdFlag,
        @JsonProperty("pickup_location") String pickupLocation,
        @JsonProperty("dropoff_location") String dropoffLocation,
        @JsonProperty("payment_type") String paymentType,
        @JsonProperty("fare_amount") Double fareAmount,
        @JsonProperty("extra") Double extra,
        @JsonProperty("mta_tax") Double mtaTax,
        @JsonProperty("tip_amount") Double tipAmount,
        @JsonProperty("tolls_amount") Double tollsAmount,
        @JsonProperty("improvement_surcharge") Double improvementSurcharge,
        @JsonProperty("total_amount") Double totalAmount
) {}
