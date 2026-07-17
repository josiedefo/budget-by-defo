package com.budget.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the transaction endpoints end-to-end, including the native filter query
 * (transactionId / date range / type / merchant substring / section+item name / uncategorized).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TransactionApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private long createTransaction(String type, String date, String merchant, String amount,
                                   Long sectionId, Long budgetItemId) throws Exception {
        StringBuilder json = new StringBuilder("{\"type\":\"").append(type)
                .append("\",\"transactionDate\":\"").append(date)
                .append("\",\"merchant\":\"").append(merchant)
                .append("\",\"amount\":").append(amount);
        if (sectionId != null) json.append(",\"sectionId\":").append(sectionId);
        if (budgetItemId != null) json.append(",\"budgetItemId\":").append(budgetItemId);
        json.append("}");

        String body = mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    void getTransactions_appliesFilters() throws Exception {
        // Budget month gives us a real section + item for the categorized transaction
        String budgetBody = mockMvc.perform(get("/api/budgets/2032/1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode budget = objectMapper.readTree(budgetBody);
        JsonNode section = null;
        for (JsonNode s : budget.get("sections")) {
            if (!s.get("isIncome").asBoolean() && s.get("items").size() > 0) { section = s; break; }
        }
        assertThat(section).isNotNull();
        long sectionId = section.get("id").asLong();
        String sectionName = section.get("name").asText();
        long itemId = section.get("items").get(0).get("id").asLong();
        String itemName = section.get("items").get(0).get("name").asText();

        long categorized = createTransaction("EXPENSE", "2032-01-05", "Walmart", "45.00", sectionId, itemId);
        createTransaction("EXPENSE", "2032-01-10", "Target", "30.00", null, null);
        createTransaction("INCOME", "2032-01-15", "Employer", "1000.00", null, null);
        createTransaction("EXPENSE", "2032-02-01", "Walmart", "20.00", null, null); // outside range

        String range = "startDate=2032-01-01&endDate=2032-01-31";

        mockMvc.perform(get("/api/transactions?" + range))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/transactions?" + range + "&type=INCOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].merchant").value("Employer"));

        // Merchant filter is a case-insensitive substring match
        mockMvc.perform(get("/api/transactions?" + range + "&merchant=walm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].merchant").value("Walmart"));

        // Name-based navigation filter (used by budget item → transactions links)
        mockMvc.perform(get("/api/transactions?" + range +
                        "&sectionName=" + sectionName + "&budgetItemName=" + itemName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(categorized));

        mockMvc.perform(get("/api/transactions?" + range + "&uncategorized=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/transactions?transactionId=" + categorized))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void matchingIdsAndBulkDelete_removeAllMatchingTransactions() throws Exception {
        long a = createTransaction("EXPENSE", "2032-03-05", "Costco", "45.00", null, null);
        long b = createTransaction("EXPENSE", "2032-03-10", "Costco", "30.00", null, null);
        createTransaction("EXPENSE", "2032-03-11", "Other", "10.00", null, null);

        String idsBody = mockMvc.perform(get("/api/transactions/matching-ids?merchant=costco" +
                        "&startDate=2032-03-01&endDate=2032-03-31"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode ids = objectMapper.readTree(idsBody);
        assertThat(ids).hasSize(2);

        mockMvc.perform(delete("/api/transactions/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[" + a + "," + b + "]}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/transactions?startDate=2032-03-01&endDate=2032-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].merchant").value("Other"));
    }

    @Test
    void importTransactions_importsValidRowsOnly() throws Exception {
        String payload = """
                {
                  "columnMapping": {"date": 0, "merchant": 1, "amount": 2},
                  "skipFirstRow": true,
                  "rows": [
                    ["Date", "Merchant", "Amount"],
                    ["2032-04-01", "Grocer", "-45.50"],
                    ["2032-04-02", "Employer", "1000.00"],
                    ["bad-date", "Broken", "10.00"],
                    ["2032-04-03", "Zero Corp", "0.00"]
                  ]
                }
                """;

        mockMvc.perform(post("/api/transactions/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("EXPENSE"))
                .andExpect(jsonPath("$[1].type").value("INCOME"));
    }
}
