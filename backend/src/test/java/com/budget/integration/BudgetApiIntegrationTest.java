package com.budget.integration;

import com.budget.repository.BudgetItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BudgetApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BudgetItemRepository budgetItemRepository;

    @Test
    void getBudget_createsDefaultSectionsAndItems() throws Exception {
        mockMvc.perform(get("/api/budgets/2031/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2031))
                .andExpect(jsonPath("$.month").value(1))
                .andExpect(jsonPath("$.sections").isNotEmpty())
                .andExpect(jsonPath("$.sections[?(@.name == 'Income')].isIncome").value(true));
    }

    @Test
    void getBudget_invalidMonth_returns400NotServerError() throws Exception {
        mockMvc.perform(get("/api/budgets/2031/13"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/budgets/2031/0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void copyBudget_mergeByNamePreservesTargetItemIds() throws Exception {
        // Create source month and grab an expense item
        JsonNode source = getBudget(2031, 3);
        JsonNode sourceItem = firstExpenseItem(source);
        long sourceItemId = sourceItem.get("id").asLong();
        String itemName = sourceItem.get("name").asText();

        setPlannedAmount(sourceItemId, "123.45");

        // First copy: target month does not exist yet → full structural copy
        mockMvc.perform(post("/api/budgets/2031/4/copy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceYear\":2031,\"sourceMonth\":3}"))
                .andExpect(status().isOk());

        JsonNode target = getBudget(2031, 4);
        JsonNode targetItem = findItemByName(target, itemName);
        long targetItemId = targetItem.get("id").asLong();
        assertThat(targetItem.get("plannedAmount").decimalValue())
                .isEqualByComparingTo(new BigDecimal("123.45"));
        assertThat(targetItemId).isNotEqualTo(sourceItemId);

        // Second copy: target now has sections → merge must keep the target item's id
        setPlannedAmount(sourceItemId, "200.00");
        mockMvc.perform(post("/api/budgets/2031/4/copy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceYear\":2031,\"sourceMonth\":3}"))
                .andExpect(status().isOk());

        JsonNode mergedItem = findItemByName(getBudget(2031, 4), itemName);
        assertThat(mergedItem.get("id").asLong()).isEqualTo(targetItemId);
        assertThat(mergedItem.get("plannedAmount").decimalValue())
                .isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void copyBudget_sameSourceAndTarget_returns400() throws Exception {
        getBudget(2031, 5);
        mockMvc.perform(post("/api/budgets/2031/5/copy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceYear\":2031,\"sourceMonth\":5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void viewingMonth_reportsActualsInDtoButDoesNotPersistThem() throws Exception {
        JsonNode budget = getBudget(2031, 6);
        JsonNode expenseSection = firstExpenseSection(budget);
        long sectionId = expenseSection.get("id").asLong();
        long itemId = expenseSection.get("items").get(0).get("id").asLong();

        // A June-2031 expense transaction against that item
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"EXPENSE\",\"transactionDate\":\"2031-06-10\"," +
                                 "\"merchant\":\"Grocer\",\"amount\":50.00," +
                                 "\"sectionId\":" + sectionId + ",\"budgetItemId\":" + itemId + "}"))
                .andExpect(status().isOk());

        // The API response computes the actual from transactions...
        JsonNode refreshed = getBudget(2031, 6);
        JsonNode item = findItemById(refreshed, itemId);
        assertThat(item.get("actualAmount").decimalValue())
                .isEqualByComparingTo(new BigDecimal("50.00"));

        // ...but the stored entity keeps its own value (used to get clobbered on every view)
        BigDecimal persisted = budgetItemRepository.findById(itemId).orElseThrow().getActualAmount();
        assertThat(persisted).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── helpers ──

    private JsonNode getBudget(int year, int month) throws Exception {
        String body = mockMvc.perform(get("/api/budgets/" + year + "/" + month))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private void setPlannedAmount(long itemId, String amount) throws Exception {
        mockMvc.perform(put("/api/items/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plannedAmount\":" + amount + "}"))
                .andExpect(status().isOk());
    }

    private JsonNode firstExpenseSection(JsonNode budget) {
        for (JsonNode section : budget.get("sections")) {
            if (!section.get("isIncome").asBoolean() && section.get("items").size() > 0) {
                return section;
            }
        }
        throw new AssertionError("No expense section with items found");
    }

    private JsonNode firstExpenseItem(JsonNode budget) {
        return firstExpenseSection(budget).get("items").get(0);
    }

    private JsonNode findItemByName(JsonNode budget, String name) {
        for (JsonNode section : budget.get("sections")) {
            for (JsonNode item : section.get("items")) {
                if (item.get("name").asText().equals(name)) {
                    return item;
                }
            }
        }
        throw new AssertionError("Item not found: " + name);
    }

    private JsonNode findItemById(JsonNode budget, long id) {
        for (JsonNode section : budget.get("sections")) {
            for (JsonNode item : section.get("items")) {
                if (item.get("id").asLong() == id) {
                    return item;
                }
            }
        }
        throw new AssertionError("Item not found: " + id);
    }
}
