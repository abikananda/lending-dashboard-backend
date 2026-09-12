package com.techconsulting.lending.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class BorrowerNameResolver {
    private final RestClient.Builder restClientBuilder;
    private final Map<String, Optional<BorrowerIdentity>> cache = new ConcurrentHashMap<>();
    @Value("${app.borrower-lookup-url:}") private String lookupUrl;

    public Optional<BorrowerIdentity> resolve(String loanId) {
        if (loanId == null || lookupUrl == null || lookupUrl.isBlank()) return Optional.empty();
        return cache.computeIfAbsent(loanId, this::fetch);
    }

    private Optional<BorrowerIdentity> fetch(String loanId) {
        try {
            JsonNode response = restClientBuilder.build().get()
                    .uri(lookupUrl.replace("{loanId}", loanId)).retrieve().body(JsonNode.class);
            if (response == null) return Optional.empty();
            String borrowerId = text(response, "borrowerId", "publicId", "borrowerPublicId");
            String name = text(response, "name", "borrowerName", "firstName");
            if (borrowerId != null && name != null) return Optional.of(new BorrowerIdentity(borrowerId, name));
        } catch (RuntimeException ignored) { }
        return Optional.empty();
    }

    private String text(JsonNode response, String... fields) {
        for (String field : fields) {
            JsonNode value = response.get(field);
            if (value != null && !value.asText().isBlank()) return value.asText().trim();
        }
        return null;
    }

    public record BorrowerIdentity(String borrowerId, String name) { }
}
