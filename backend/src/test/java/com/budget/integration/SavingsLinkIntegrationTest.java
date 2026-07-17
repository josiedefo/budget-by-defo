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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of the budget ↔ savings link lifecycle, including the
 * regressions fixed in this change set:
 *  - editing a linked transaction now syncs the savings event and balances
 *  - deleting a linked transaction reverses balances (and refuses to go negative)
 *  - single-transaction fund deposits enforce the pool ceiling
 *  - lowering a fund deposit below spent funds is rejected
 *  - the batch link-status endpoint binds comma-separated ids (the axios default
 *    "ids[]" form does NOT bind, which is why the frontend joins with commas)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SavingsLinkIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private long createAccount(String name, String balance) throws Exception {
        String body = mockMvc.perform(post("/api/savings/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"balance\":" + balance +
                                 ",\"asOfDate\":\"2033-01-01\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long createFund(String name) throws Exception {
        String body = mockMvc.perform(post("/api/savings/funds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"goalType\":\"NO_GOAL\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long createTransaction(String date, String merchant, String amount) throws Exception {
        String body = mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"EXPENSE\",\"transactionDate\":\"" + date +
                                 "\",\"merchant\":\"" + merchant + "\",\"amount\":" + amount + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private BigDecimal accountBalance(long accountId) throws Exception {
        String body = mockMvc.perform(get("/api/savings/accounts/" + accountId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("balance").decimalValue();
    }

    @Test
    void linkedTransaction_updateAndDelete_keepAccountInSync() throws Exception {
        long accountId = createAccount("Sync Account", "1000.00");
        long txId = createTransaction("2033-01-10", "Vanguard", "100.00");

        // Link as a deposit → balance 1100
        mockMvc.perform(post("/api/savings/accounts/" + accountId + "/link-transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionId\":" + txId + ",\"eventType\":\"DEPOSIT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAfter").value(1100.00));
        assertThat(accountBalance(accountId)).isEqualByComparingTo("1100.00");

        // Editing the transaction amount must flow through to the event and balance
        mockMvc.perform(put("/api/transactions/" + txId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":150.00}"))
                .andExpect(status().isOk());
        assertThat(accountBalance(accountId)).isEqualByComparingTo("1150.00");

        String events = mockMvc.perform(get("/api/savings/accounts/" + accountId + "/events"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode linkedEvent = null;
        for (JsonNode e : objectMapper.readTree(events)) {
            if (e.hasNonNull("transactionId") && e.get("transactionId").asLong() == txId) {
                linkedEvent = e;
            }
        }
        assertThat(linkedEvent).as("linked account event").isNotNull();
        assertThat(linkedEvent.get("amount").decimalValue()).isEqualByComparingTo("150.00");

        // Deleting the transaction reverses the deposit
        mockMvc.perform(delete("/api/transactions/" + txId))
                .andExpect(status().isNoContent());
        assertThat(accountBalance(accountId)).isEqualByComparingTo("1000.00");
    }

    @Test
    void deletingTransaction_whoseDepositWasSpent_returns400() throws Exception {
        long accountId = createAccount("Spent Account", "0.00");
        long txId = createTransaction("2033-02-10", "Payroll", "100.00");

        mockMvc.perform(post("/api/savings/accounts/" + accountId + "/link-transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionId\":" + txId + ",\"eventType\":\"DEPOSIT\"}"))
                .andExpect(status().isOk());

        // Spend most of it directly from the account
        mockMvc.perform(post("/api/savings/accounts/" + accountId + "/withdrawal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":80.00,\"eventDate\":\"2033-02-15\"}"))
                .andExpect(status().isOk());

        // Reversing the 100 deposit would leave the account at -80
        mockMvc.perform(delete("/api/transactions/" + txId))
                .andExpect(status().isBadRequest());
        assertThat(accountBalance(accountId)).isEqualByComparingTo("20.00");
    }

    @Test
    void fundDepositLink_enforcesPoolCeiling() throws Exception {
        createAccount("Pool Account", "500.00");
        long fundId = createFund("Pool Fund");

        // Within the pool → OK
        long smallTx = createTransaction("2033-03-05", "Transfer", "300.00");
        mockMvc.perform(post("/api/savings/events/link-transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionId\":" + smallTx + ",\"fundId\":" + fundId +
                                 ",\"eventType\":\"DEPOSIT_ALLOCATED\"}"))
                .andExpect(status().isOk());

        // 300 allocated + 5000 > 500 pool → rejected
        long hugeTx = createTransaction("2033-03-06", "Transfer", "5000.00");
        mockMvc.perform(post("/api/savings/events/link-transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionId\":" + hugeTx + ",\"fundId\":" + fundId +
                                 ",\"eventType\":\"DEPOSIT_ALLOCATED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("exceed savings pool")));
    }

    @Test
    void loweringFundDepositBelowSpentAmount_returns400() throws Exception {
        createAccount("Floor Account", "1000.00");
        long fundId = createFund("Floor Fund");

        String depositBody = mockMvc.perform(post("/api/savings/events/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetFundId\":" + fundId + ",\"amount\":100.00," +
                                 "\"eventDate\":\"2033-04-01\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long depositEventId = objectMapper.readTree(depositBody).get("id").asLong();

        mockMvc.perform(post("/api/savings/events/withdrawal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fundId\":" + fundId + ",\"amount\":80.00," +
                                 "\"eventDate\":\"2033-04-02\"}"))
                .andExpect(status().isOk());

        // Balance is 20; lowering the 100 deposit to 10 would make it -70
        mockMvc.perform(put("/api/savings/events/" + depositEventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"eventDate\":\"2033-04-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("negative fund balance")));
    }

    @Test
    void linkStatusEndpoint_bindsBothCommaSeparatedAndBracketIdForms() throws Exception {
        // Create a budget month so we have real budget item ids
        String budgetBody = mockMvc.perform(get("/api/budgets/2033/5"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode budget = objectMapper.readTree(budgetBody);
        JsonNode items = budget.get("sections").get(0).get("items");
        long id1 = items.get(0).get("id").asLong();
        long id2 = items.get(1).get("id").asLong();

        // The comma form (what the frontend sends since it joins explicitly)
        mockMvc.perform(get("/api/savings/link-status/budget-items")
                        .param("ids", id1 + "," + id2)
                        .param("startDate", "2033-05-01")
                        .param("endDate", "2033-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + id1 + ".totalTransactions").value(0))
                .andExpect(jsonPath("$." + id2 + ".totalTransactions").value(0));

        // The axios default array form ("ids[]") also binds on Spring Framework 6.1+,
        // which falls back to "name[]" for multi-value params. Pinned here so an
        // upgrade that drops the fallback is caught by tests.
        mockMvc.perform(get("/api/savings/link-status/budget-items")
                        .param("ids[]", String.valueOf(id1))
                        .param("ids[]", String.valueOf(id2))
                        .param("startDate", "2033-05-01")
                        .param("endDate", "2033-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + id1 + ".totalTransactions").value(0));
    }

    @Test
    void linkStatus_reportsAllLinkedOnlyWhenEveryTransactionIsLinkedToSameAccount() throws Exception {
        long accountId = createAccount("Status Account", "1000.00");

        String budgetBody = mockMvc.perform(get("/api/budgets/2033/6"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode budget = objectMapper.readTree(budgetBody);
        JsonNode section = null;
        for (JsonNode s : budget.get("sections")) {
            if (!s.get("isIncome").asBoolean() && s.get("items").size() > 0) { section = s; break; }
        }
        assertThat(section).isNotNull();
        long sectionId = section.get("id").asLong();
        long itemId = section.get("items").get(0).get("id").asLong();

        // Two June transactions on the item; link only one
        String txJson = "{\"type\":\"EXPENSE\",\"transactionDate\":\"2033-06-10\",\"merchant\":\"A\"," +
                "\"amount\":10.00,\"sectionId\":" + sectionId + ",\"budgetItemId\":" + itemId + "}";
        String tx1Body = mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON).content(txJson))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long tx1 = objectMapper.readTree(tx1Body).get("id").asLong();
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON).content(txJson))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/savings/accounts/" + accountId + "/link-transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionId\":" + tx1 + ",\"eventType\":\"WITHDRAWAL\"}"))
                .andExpect(status().isOk());

        // One of two linked → not "all linked"
        mockMvc.perform(get("/api/savings/link-status/budget-items")
                        .param("ids", String.valueOf(itemId))
                        .param("startDate", "2033-06-01")
                        .param("endDate", "2033-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + itemId + ".totalTransactions").value(2))
                .andExpect(jsonPath("$." + itemId + ".allLinkedToAccount").value(false));

        // Bulk-link the rest → now all linked to the same account
        mockMvc.perform(post("/api/savings/accounts/" + accountId + "/bulk-link-budget-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budgetItemId\":" + itemId + ",\"eventType\":\"WITHDRAWAL\"," +
                                 "\"startDate\":\"2033-06-01\",\"endDate\":\"2033-06-30\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(1))
                .andExpect(jsonPath("$.skipped").value(1));

        mockMvc.perform(get("/api/savings/link-status/budget-items")
                        .param("ids", String.valueOf(itemId))
                        .param("startDate", "2033-06-01")
                        .param("endDate", "2033-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + itemId + ".allLinkedToAccount").value(true));
    }
}
