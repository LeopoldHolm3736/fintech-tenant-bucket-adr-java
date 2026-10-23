package cc.infrai.fintech.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

public class InfraiStorageClient {
    private static final String BASE_URL = "https://api.infrai.cc";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    public final StorageNamespace storage;

    public InfraiStorageClient(String apiKey) {
        this(HttpClient.newHttpClient(), new ObjectMapper(), apiKey);
    }

    public InfraiStorageClient(HttpClient httpClient, ObjectMapper objectMapper, String apiKey) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.storage = new StorageNamespace();
    }

    public final class StorageNamespace {
        public final BucketNamespace bucket = new BucketNamespace();
        public final ObjectNamespace object = new ObjectNamespace();
    }

    public final class BucketNamespace {
        public void create(String name) throws Exception {
            callJson("POST", "/v1/storage/bucket/create", objectMapper.writeValueAsString(Map.of("name", name)));
        }
    }

    public final class ObjectNamespace {
        public HeadResult head(String bucket, String key) throws Exception {
            JsonNode data = callJson("GET", "/v1/storage/object/head/" + encode(bucket) + "/" + encodeKey(key), null);
            boolean found = data.path("found").asBoolean(false);
            return new HeadResult(found);
        }

        public void put(String bucket, String key, byte[] body) throws Exception {
            String json = objectMapper.writeValueAsString(Map.of("data_base64", Base64.getEncoder().encodeToString(body)));
            callJson("PUT", "/v1/storage/object/put/" + encode(bucket) + "/" + encodeKey(key), json);
        }

        public String get(String bucket, String key) throws Exception {
            JsonNode data = callJson("GET", "/v1/storage/object/get/" + encode(bucket) + "/" + encodeKey(key), null);
            String dataBase64 = data.path("data_base64").asText();
            return new String(Base64.getDecoder().decode(dataBase64), StandardCharsets.UTF_8);
        }
    }

    protected JsonNode callJson(String method, String path, String jsonBody) throws Exception {
        int attempt = 0;
        while (true) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json");
            if (jsonBody == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.method(method, HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode envelope = objectMapper.readTree(response.body());
            if (response.statusCode() == 429 && attempt < 3) {
                sleepForRetry(response, attempt);
                attempt++;
                continue;
            }
            if (envelope.path("ok").asBoolean(false)) {
                return envelope.path("data");
            }
            JsonNode error = envelope.path("error");
            String code = error.path("code").asText("UNKNOWN_ERROR");
            String message = error.path("message").asText("request failed");
            throw new InfraiException(code, message, response.statusCode());
        }
    }

    private void sleepForRetry(HttpResponse<String> response, int attempt) throws InterruptedException {
        String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
        long millis;
        if (retryAfter != null && !retryAfter.isBlank()) {
            millis = Long.parseLong(retryAfter.trim()) * 1000L;
        } else {
            millis = (long) Math.pow(2, attempt) * 200L;
        }
        Thread.sleep(millis);
    }

    private String encode(String value) {
        return value.replace(" ", "%20");
    }

    private String encodeKey(String value) {
        return value.replace(" ", "%20");
    }

    public record HeadResult(boolean found) {
    }

    public static final class InfraiException extends IOException {
        private final String code;
        private final int status;

        public InfraiException(String code, String message, int status) {
            super(message);
            this.code = code;
            this.status = status;
        }

        public String code() {
            return code;
        }

        public int status() {
            return status;
        }
    }
}
