package com.myCafeteria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ApiDocumentationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    public void apiSpecExposesVersionedRoutesAndBearerScheme() throws Exception {
        String response = mvc.perform(get("/v3/api-docs/cafeteria"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode doc = mapper.readTree(response);
        assertTrue(doc.path("openapi").asText().startsWith("3."));
        assertTrue(doc.path("paths").has("/api/auth/signin"));
        assertTrue(doc.path("paths").has("/api/menu"));
        assertTrue(doc.path("paths").has("/api/orders"));
        assertTrue(doc.path("paths").path("/api/orders").has("post"));
        assertTrue(doc.path("components").path("securitySchemes").has("bearerAuth"));
    }
}
