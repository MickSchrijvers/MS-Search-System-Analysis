package sogeti.elasticsearch.application;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkListener;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.TransportException;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.util.BinaryData;
import co.elastic.clients.util.ContentType;
import co.elastic.clients.util.DateTime;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.annotation.PreDestroy;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sogeti.elasticsearch.domain.DatasetName;
import sogeti.elasticsearch.domain.HttpRequest;
import sogeti.elasticsearch.domain.TaxiRequest;
import sogeti.elasticsearch.domain.WikipediaRequest;
import sogeti.elasticsearch.presentation.dto.PostBulkIndexDTO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class GeneralService {
    private final String url;
    private final String apiKey;
    private RestClient restClient;
    private ElasticsearchTransport transport;
    private ElasticsearchClient client;

    public GeneralService(@Value("${elasticsearch.url}") String url,
                          @Value("${elasticsearch.api-key}") String apiKey) {
        this.url = url;
        this.apiKey = apiKey;

        //Elasticsearch Date -> DateTime
        ObjectMapper mapper = new ObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        SimpleModule esModule = new SimpleModule();
        esModule.addDeserializer(DateTime.class, new JsonDeserializer<>() {
            @Override
            public DateTime deserialize(JsonParser p, DeserializationContext ctx)
                    throws IOException {
                return DateTime.of(p.getValueAsString());
            }
        });
        mapper.registerModule(esModule);


        this.restClient = RestClient
            .builder(HttpHost.create(this.url))
            .setDefaultHeaders(new Header[]{
                    new BasicHeader("Authorization", "ApiKey " + this.apiKey)
            })
            .build();

        this.transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper(mapper));

        this.client = new ElasticsearchClient(transport);
    }

    public void bulkIndex(PostBulkIndexDTO postBulkIndexDTO) throws IOException {
        DatasetName datasetName = DatasetName.valueOf(postBulkIndexDTO.type());

        boolean indexExists = client.indices()
                .exists(e -> e.index(datasetName.name().toLowerCase()))
                .value();

        if (indexExists) {
            client.indices().delete(d -> d.index(datasetName.name().toLowerCase()));
            System.out.printf("Bestaande index verwijderd: %s%n", datasetName.name().toLowerCase());
        }

        BulkListener<Void> listener = new BulkListener<>() {

            @Override
            public void beforeBulk(long id, BulkRequest req, List<Void> ctx) {
                System.out.printf("sending bulk %d with %d ops%n", id, req.operations().size());
            }

            @Override
            public void afterBulk(long id, BulkRequest req,
                                  List<Void> ctx, BulkResponse resp) {

                if (resp.errors()) {
                    System.err.printf("bulk %d had errors%n", id);
                    resp.items().stream()
                            .filter(item -> item.error() != null)
                            .forEach(item ->
                                    System.err.println(item.error().reason())
                            );
                } else {
                    System.out.printf("bulk %d indexed %d docs%n", id, resp.items().size());
                }
            }

            @Override
            public void afterBulk(long id, BulkRequest req,
                                  List<Void> ctx, Throwable failure) {
                System.err.printf("bulk %d failed completely: %s%n", id, failure.getMessage());
            }
        };

        BulkIngester<Void> ingester = BulkIngester.of(b -> b
                .client(this.client)
                .maxOperations(5000)
                .flushInterval(2, TimeUnit.SECONDS)
                .listener(listener)
        );

        Path path = switch (datasetName) {
            case TAXI      -> Path.of("Elasticsearch/src/main/resources/taxi.json");
            case HTTP      -> Path.of("Elasticsearch/src/main/resources/http.json");
            case WIKIPEDIA -> Path.of("Elasticsearch/src/main/resources/wikipedia.json");
        };

        BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            BinaryData doc = BinaryData.of(
                    line.getBytes(StandardCharsets.UTF_8),
                    ContentType.APPLICATION_JSON);

            ingester.add(op -> op
                    .index(i -> i
                            .index(datasetName.name().toLowerCase())
                            .document(doc)
                    )
            );
        }

        ingester.close();
    }

    public List<HttpRequest> searchHttpRequests(
            Optional<Long>   timestamp,
            Optional<String> clientIp,
            Optional<String> request,
            Optional<Long>   size,
            Optional<Long> status) throws IOException {
        BoolQuery.Builder bool = new BoolQuery.Builder();

        timestamp.ifPresent(ts ->
                bool.must(mq -> mq
                        .term(t -> t.field("@timestamp")
                                .value(ts))));

        clientIp.ifPresent(ip ->
                bool.must(mq -> mq
                        .match(m -> m.field("clientip")
                                .query(ip))));

        request.ifPresent(url ->
                bool.must(mq -> mq
                        .match(m -> m.field("request")
                                .query(url))));

        size.ifPresent(sz ->
                bool.must(mq -> mq
                        .term(t -> t.field("size")
                                .value(sz))));

        status.ifPresent(st ->
                bool.must(mq -> mq
                        .term(t -> t.field("status")
                                .value(st))));

        BoolQuery boolQuery = bool.build();
        Query      finalQuery = boolQuery.must().isEmpty()
                ? Query.of(q -> q.matchAll(m -> m))
                : Query.of(q -> q.bool(boolQuery));

        SearchResponse<HttpRequest> response = this.client.search(s -> s
                .index("http")
                .query(finalQuery),
                HttpRequest.class);

        return response.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).toList();
    }

    public List<HttpRequest> httpTestRequest() throws IOException {
        this.bulkIndex(new PostBulkIndexDTO("HTTP"));

        return this.searchHttpRequests(Optional.empty(), Optional.empty(), Optional.of("space.gif"), Optional.empty(), Optional.of(404L));
    }

    public List<TaxiRequest> searchTaxiRequests(
            Optional<Integer> vendorId,
            Optional<DateTime> pickupDateTime,
            Optional<DateTime> dropoffDateTime,
            Optional<Integer> passengerCount,
            Optional<Double> tripDistance,
            Optional<Integer> rateCodeId,
            Optional<String> storeAndFwdFlag,
            Optional<String> pickupLocation,
            Optional<String> dropoffLocation,
            Optional<String> paymentType,
            Optional<Double> fareAmount,
            Optional<Double> extra,
            Optional<Double> mtaTax,
            Optional<Double> tipAmount,
            Optional<Double> tollsAmount,
            Optional<Double> improvementSurcharge,
            Optional<Double> totalAmount
    ) throws IOException {
        BoolQuery.Builder bool = new BoolQuery.Builder();

        vendorId.ifPresent(v ->
                bool.must(mq -> mq
                        .term(t -> t.field("vendor_id").value(v))));
        pickupDateTime.ifPresent(dt ->
                bool.must(mq -> mq
                        .term(t -> t.field("pickup_datetime").value(FieldValue.of(dt)))));
        dropoffDateTime.ifPresent(dt ->
                bool.must(mq -> mq
                        .term(t -> t.field("dropoff_datetime").value(FieldValue.of(dt)))));
        passengerCount.ifPresent(pc ->
                bool.must(mq -> mq
                        .term(t -> t.field("passenger_count").value(pc))));
        tripDistance.ifPresent(td ->
                bool.must(mq -> mq
                        .term(t -> t.field("trip_distance").value(td))));
        rateCodeId.ifPresent(rc ->
                bool.must(mq -> mq
                        .term(t -> t.field("rate_code_id").value(rc))));
        storeAndFwdFlag.ifPresent(flag ->
                bool.must(mq -> mq
                        .match(m -> m.field("store_and_fwd_flag").query(flag))));
        pickupLocation.ifPresent(loc ->
                bool.must(mq -> mq
                        .match(m -> m.field("pickup_location").query(loc))));
        dropoffLocation.ifPresent(loc ->
                bool.must(mq -> mq
                        .match(m -> m.field("dropoff_location").query(loc))));
        paymentType.ifPresent(pt ->
                bool.must(mq -> mq
                        .match(m -> m.field("payment_type").query(pt))));
        fareAmount.ifPresent(fa ->
                bool.must(mq -> mq
                        .term(t -> t.field("fare_amount").value(fa))));
        extra.ifPresent(ex ->
                bool.must(mq -> mq
                        .term(t -> t.field("extra").value(ex))));
        mtaTax.ifPresent(mt ->
                bool.must(mq -> mq
                        .term(t -> t.field("mta_tax").value(mt))));
        tipAmount.ifPresent(tip ->
                bool.must(mq -> mq
                        .term(t -> t.field("tip_amount").value(tip))));
        tollsAmount.ifPresent(tl ->
                bool.must(mq -> mq
                        .term(t -> t.field("tolls_amount").value(tl))));
        improvementSurcharge.ifPresent(imp ->
                bool.must(mq -> mq
                        .term(t -> t.field("improvement_surcharge").value(imp))));
        totalAmount.ifPresent(ta ->
                bool.must(mq -> mq
                        .term(t -> t.field("total_amount").value(ta))));

        BoolQuery built = bool.build();
        Query finalQuery = built.must().isEmpty()
                ? Query.of(q -> q.matchAll(m -> m))
                : Query.of(q -> q.bool(built));

        SearchResponse<TaxiRequest> response = this.client.search(s -> s
                        .index("taxi")
                        .query(finalQuery),
                TaxiRequest.class);

        return response.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).toList();
    }

    public List<TaxiRequest> taxiTestRequest() throws IOException {
        this.bulkIndex(new PostBulkIndexDTO("TAXI"));

        return searchTaxiRequests(Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),Optional.of("231"),Optional.empty(),Optional.empty(),Optional.of(17.5),Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty());
    }

    public List<WikipediaRequest> searchWikipediaRequests(
            Optional<String> content,
            Optional<String> id,
            Optional<String> index,
            Optional<String> namespace,
            Optional<String> redirect,
            Optional<String> title
    ) throws IOException {
        BoolQuery.Builder bool = new BoolQuery.Builder();

        content.ifPresent(loc ->
                bool.must(mq -> mq
                        .match(m -> m.field("content").query(loc))));
        id.ifPresent(loc ->
                bool.must(mq -> mq
                        .match(m -> m.field("index._id").query(loc))));
        index.ifPresent(loc ->
                bool.must(mq -> mq
                        .match(m -> m.field("index._index").query(loc))));
        namespace.ifPresent(loc ->
                bool.must(mq -> mq
                        .match(m -> m.field("namespace").query(loc))));
        redirect.ifPresent(loc ->
                bool.must(mq -> mq
                        .match(m -> m.field("redirect").query(loc))));
        title.ifPresent(loc ->
                bool.must(mq -> mq
                        .match(m -> m.field("title").query(loc))));

        BoolQuery built = bool.build();
        Query finalQuery = built.must().isEmpty()
                ? Query.of(q -> q.matchAll(m -> m))
                : Query.of(q -> q.bool(built));

        SearchResponse<WikipediaRequest> response = this.client.search(s -> s
                        .index("wikipedia")
                        .query(finalQuery),
                WikipediaRequest.class);

        return response.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).toList();
    }

    public List<WikipediaRequest> wikipediaTestRequest() throws IOException {
        this.bulkIndex(new PostBulkIndexDTO("WIKIPEDIA"));

        return searchWikipediaRequests(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("Royal Navy"));
    }


    @PreDestroy
    private void cleanup() throws IOException {
        this.client.close();
    }
}
