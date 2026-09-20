package com.budget.integration;

import com.budget.repository.BudgetItemRepository;
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

    @Test
    void updatingItemMetadata_doesNotResetActualAmountToZero() throws Exception {
        JsonNode budget = getBudget(2031, 7);
        JsonNode expenseSection = firstExpenseSection(budget);
        long sectionId = expenseSection.get("id").asLong();
        long itemId = expenseSection.get("items").get(0).get("id").asLong();

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"EXPENSE\",\"transactionDate\":\"2031-07-10\"," +
                                 "\"merchant\":\"Grocer\",\"amount\":75.00," +
                                 "\"sectionId\":" + sectionId + ",\"budgetItemId\":" + itemId + "}"))
                .andExpect(status().isOk());

        // Sanity check: the item now shows a non-zero actual before we touch its flags
        JsonNode beforeToggle = findItemById(getBudget(2031, 7), itemId);
        assertThat(beforeToggle.get("actualAmount").decimalValue())
                .isEqualByComparingTo(new BigDecimal("75.00"));

        // Tagging as a key item is metadata-only — it must not clobber the computed actual
        mockMvc.perform(put("/api/items/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isKeyItem\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualAmount").value(75.00))
                .andExpect(jsonPath("$.isKeyItem").value(true));

        // Same for excluding it from budget totals
        mockMvc.perform(put("/api/items/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isExcludedFromBudget\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualAmount").value(75.00))
                .andExpect(jsonPath("$.isExcludedFromBudget").value(true));
    }

    @Test
    void getItemInsight_matchesYearlySummaryTotalsAndAggregatesAcrossMonths() throws Exception {
        JsonNode augustBudget = getBudget(2034, 8);
        JsonNode expenseSection = firstExpenseSection(augustBudget);
        String sectionName = expenseSection.get("name").asText();
        String itemName = expenseSection.get("items").get(0).get("name").asText();
        long augustItemId = expenseSection.get("items").get(0).get("id").asLong();

        // September's default budget carries the same section/item names (by design — every
        // month is seeded from the same DEFAULT_SECTIONS), so it is the cross-month match.
        getBudget(2034, 9);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"EXPENSE\",\"transactionDate\":\"2034-08-15\"," +
                                 "\"merchant\":\"Grocer\",\"amount\":60.00," +
                                 "\"sectionId\":" + expenseSection.get("id").asLong() +
                                 ",\"budgetItemId\":" + augustItemId + "}"))
                .andExpect(status().isOk());

        // Section/item names sent upper/lower-cased on purpose to exercise case-insensitive matching.
        String insightBody = mockMvc.perform(get("/api/budgets/2034/item-insight")
                        .param("section", sectionName.toUpperCase())
                        .param("item", itemName.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2034))
                .andExpect(jsonPath("$.isIncome").value(false))
                .andExpect(jsonPath("$.months.length()").value(2))
                .andExpect(jsonPath("$.months[0].month").value(8))
                .andExpect(jsonPath("$.months[0].actual").value(60.00))
                .andExpect(jsonPath("$.months[1].month").value(9))
                .andExpect(jsonPath("$.months[1].actual").value(0))
                .andExpect(jsonPath("$.annualActual").value(60.00))
                .andExpect(jsonPath("$.ytdActual").value(60.00))
                .andExpect(jsonPath("$.monthlyAverageActual").value(5.00))
                .andReturn().getResponse().getContentAsString();
        JsonNode insight = objectMapper.readTree(insightBody);

        // The item-insight's per-month total must equal the Yearly summary's actualExpenses for
        // that same month — both derive from the same populateActualAmounts path.
        BigDecimal insightAugustMonthTotal = insight.get("months").get(0).get("monthTotal").decimalValue();
        mockMvc.perform(get("/api/budgets/2034"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months[?(@.month == 8)].actualExpenses").value(
                        insightAugustMonthTotal.doubleValue()));
    }

    @Test
    void getItemInsight_unknownItem_returnsEmptySeriesNotAnError() throws Exception {
        getBudget(2035, 1);

        // "Daily Living" is a real default section (it seeds "Groceries"/"Restaurants"/etc.),
        // but this item name does not exist in it — the item, not the section, is the miss.
        mockMvc.perform(get("/api/budgets/2035/item-insight")
                        .param("section", "Daily Living")
                        .param("item", "Definitely Not A Real Item"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months.length()").value(0))
                .andExpect(jsonPath("$.annualActual").value(0))
                .andExpect(jsonPath("$.currentMonth").isEmpty());
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
