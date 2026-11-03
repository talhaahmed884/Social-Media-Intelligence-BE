package com.media.intelligence.social_connector.connector.instagram;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Low-level HTTP access to Instagram: builds URIs, executes requests, and turns failures into
 * {@link SocialConnectorException}s. Knows nothing about our DTOs (see {@link InstagramResponseParser}).
 * <p>
 * The access token travels as the {@code access_token} query parameter (the documented method).
 * Request URIs are therefore never logged, and Spring exceptions that embed the URI are not
 * attached as causes.
 */
@Component
public class InstagramApiClient {

    private final RestClient restClient;
    private final InstagramProperties properties;
    private final ObjectMapper objectMapper;
    private final InstagramErrorMapper errorMapper;

    public InstagramApiClient(@Qualifier(InstagramConfig.REST_CLIENT) RestClient restClient,
                              InstagramProperties properties,
                              ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.errorMapper = new InstagramErrorMapper(objectMapper);
    }

    /**
     * GET a versioned Graph endpoint, e.g. segments {@code [postId, "comments"]}.
     *
     * @param operation    short description for error messages (no secrets)
     * @param pathSegments path segments, each URL-encoded
     * @param query        query parameters (the token is added automatically)
     * @param accessToken  access token
     */
    public JsonNode getGraph(String operation, List<String> pathSegments, Map<String, String> query,
                             String accessToken) {
        Map<String, String> params = new LinkedHashMap<>(query);
        params.put("access_token", accessToken);
        return execute(operation, restClient.get().uri(buildUri(properties.versionedBaseUrl(), pathSegments, params)));
    }

    /**
     * GET an unversioned Graph endpoint (long-lived token exchange and refresh).
     */
    public JsonNode getUnversioned(String operation, String path, Map<String, String> query) {
        return execute(operation, restClient.get().uri(buildUri(properties.graphBaseUrl(), List.of(path), query)));
    }

    /**
     * POST a form to an absolute URL (authorization-code exchange).
     */
    public JsonNode postForm(String operation, String url, MultiValueMap<String, String> form) {
        return execute(operation, restClient.post()
                .uri(URI.create(url))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form));
    }

    private JsonNode execute(String operation, RestClient.RequestHeadersSpec<?> request) {
        String body;
        try {
            body = request.exchange((clientRequest, response) -> {
                String responseBody = readBody(response);
                if (response.getStatusCode().is2xxSuccessful()) {
                    return responseBody;
                }
                throw errorMapper.toException(operation, response.getStatusCode().value(), responseBody);
            });
        } catch (ResourceAccessException e) {
            // e's message contains the request URI (and so the token); keep only the I/O cause
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.NETWORK_ERROR,
                    "Network error during Instagram " + operation,
                    e.getCause()
            );
        } catch (RestClientException e) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.API_CALL_FAILED,
                    "Instagram " + operation + " failed: " + e.getClass().getSimpleName()
            );
        }
        return readJson(operation, body);
    }

    private String readBody(ClientHttpResponse response) throws IOException {
        return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private JsonNode readJson(String operation, String body) {
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.PARSE_ERROR,
                    "Failed to parse Instagram " + operation + " response",
                    e
            );
        }
    }

    /**
     * Build a URI with every path segment and query value strictly encoded (including '+').
     */
    private URI buildUri(String baseUrl, List<String> pathSegments, Map<String, String> query) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
        Map<String, Object> variables = new HashMap<>();

        for (int i = 0; i < pathSegments.size(); i++) {
            builder.pathSegment("{p" + i + "}");
            variables.put("p" + i, pathSegments.get(i));
        }

        int index = 0;
        for (Map.Entry<String, String> param : query.entrySet()) {
            builder.queryParam(param.getKey(), "{q" + index + "}");
            variables.put("q" + index, param.getValue());
            index++;
        }

        return builder.encode().buildAndExpand(variables).toUri();
    }
}
