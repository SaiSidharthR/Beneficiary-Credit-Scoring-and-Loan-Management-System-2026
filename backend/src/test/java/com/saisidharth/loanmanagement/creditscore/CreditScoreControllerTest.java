package com.saisidharth.loanmanagement.creditscore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class CreditScoreControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void calculatesCreditScoreForExistingBeneficiary() throws Exception {
        String response = mockMvc.perform(post("/api/beneficiaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Anika Rao\",\"email\":\"anika@example.com\",\"phone\":\"9876543210\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String beneficiaryId = response.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(post("/api/beneficiaries/{id}/credit-score", beneficiaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monthlyIncome\":50000,\"monthlyDebt\":10000,\"onTimePayments\":10,\"missedPayments\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.beneficiaryId").value(beneficiaryId))
                .andExpect(jsonPath("$.score").value(634))
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.rating").value("FAIR"));
    }

    @Test
        void rejectsInvalidCreditProfile() throws Exception {
        String response = mockMvc.perform(post("/api/beneficiaries")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ravi Shah\",\"email\":\"ravi@example.com\",\"phone\":\"9123456780\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String beneficiaryId = response.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(post("/api/beneficiaries/{id}/credit-score", beneficiaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monthlyIncome\":0,\"monthlyDebt\":100,\"onTimePayments\":1,\"missedPayments\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "monthlyIncome, monthlyDebt, onTimePayments, and missedPayments must be valid"));
    }
}