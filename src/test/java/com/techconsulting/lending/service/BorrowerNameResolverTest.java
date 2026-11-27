package com.techconsulting.lending.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BorrowerNameResolverTest {

    @Test
    void sendsConfiguredApiKeyToRiskEngine() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BorrowerNameResolver resolver = new BorrowerNameResolver(builder);
        ReflectionTestUtils.setField(resolver, "lookupUrl", "http://localhost:8081/api/borrowers/{loanId}");
        ReflectionTestUtils.setField(resolver, "apiKey", "test-risk-engine-key");
        ReflectionTestUtils.setField(resolver, "authHeader", "X-API-Key");

        server.expect(once(), requestTo("http://localhost:8081/api/borrowers/LOAN-123"))
                .andExpect(header("X-API-Key", "test-risk-engine-key"))
                .andRespond(withSuccess("""
                        {"borrowerId":"BRW-123","borrowerName":"Jane Doe"}
                        """, APPLICATION_JSON));

        var result = resolver.resolve("LOAN-123");

        assertTrue(result.isPresent());
        assertEquals("BRW-123", result.orElseThrow().borrowerId());
        assertEquals("Jane Doe", result.orElseThrow().name());
        server.verify();
    }
}
