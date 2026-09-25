package com.saisidharth.loanmanagement.loanapplication;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class LoanApplicationControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void createsReadsAndDeletesLoanApplication() throws Exception {
        String beneficiaryId = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/beneficiaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Meera Nair\",\"email\":\"meera@example.com\",\"phone\":\"9988776655\"}"))
                .andExpect(status().isCreated());

        String beneficiaryResponse = mockMvc.perform(post("/api/beneficiaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Meera Nair\",\"email\":\"meera@example.com\",\"phone\":\"9988776655\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String createdBeneficiaryId = beneficiaryResponse.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        String response = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"beneficiaryId\":\"" + createdBeneficiaryId + "\",\"amount\":5000.00,\"termMonths\":12,\"purpose\":\"Education\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.beneficiaryId").value(createdBeneficiaryId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = response.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(get("/api/loans/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purpose").value("Education"));

        mockMvc.perform(delete("/api/loans/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/loans/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void approvesLoanAndCalculatesRepaymentSchedule() throws Exception {
        String response = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"beneficiaryId\":\"" + UUID.randomUUID() + "\",\"amount\":5000.00,\"termMonths\":12,\"purpose\":\"Education\",\"annualInterestRate\":10.0}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = response.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(patch("/api/loans/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"reviewNotes\":\"Approved after verification\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/loans/{id}/schedule", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value(id))
                .andExpect(jsonPath("$.termMonths").value(12))
                .andExpect(jsonPath("$.principal").value(5000.00))
                .andExpect(jsonPath("$.annualInterestRate").value(10.0))
                .andExpect(jsonPath("$.monthlyEmi").value(439.58))
                .andExpect(jsonPath("$.monthlyEmi").isNumber());
    }

    @Test
    void recordsRepaymentsAndReturnsLoanSummary() throws Exception {
        String response = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"beneficiaryId\":\"" + UUID.randomUUID() + "\",\"amount\":5000.00,\"termMonths\":12,\"purpose\":\"Education\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = response.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(patch("/api/loans/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"reviewNotes\":\"Approved\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/loans/{id}/repayments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1500.00,\"paymentMode\":\"BANK_TRANSFER\",\"reference\":\"REF-1001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(1500.00));

        mockMvc.perform(get("/api/loans/{id}/summary", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value(id))
                .andExpect(jsonPath("$.totalPaid").value(1500.00))
                .andExpect(jsonPath("$.outstandingBalance").value(3500.00));
    }

    @Test
    void rejectsRepaymentForPendingLoanAndOverpayment() throws Exception {
        String response = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"beneficiaryId\":\"" + UUID.randomUUID() + "\",\"amount\":5000.00,\"termMonths\":12,\"purpose\":\"Education\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = response.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(post("/api/loans/{id}/repayments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00,\"paymentMode\":\"CASH\",\"reference\":\"REF-2001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("only approved loans can receive repayments"));

        mockMvc.perform(patch("/api/loans/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/loans/{id}/repayments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":5000.01,\"paymentMode\":\"CASH\",\"reference\":\"REF-2002\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("repayment exceeds outstanding balance"));
    }

    @Test
    void rejectsReviewOfAlreadyReviewedLoan() throws Exception {
        String response = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"beneficiaryId\":\"" + UUID.randomUUID() + "\",\"amount\":2500.00,\"termMonths\":6,\"purpose\":\"Medical\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = response.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(patch("/api/loans/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\",\"reviewNotes\":\"Insufficient documentation\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/loans/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("only pending loans can be reviewed"));
    }

    @Test
    void returnsPortfolioSummaryAndAuditTrail() throws Exception {
        String beneficiaryId = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/beneficiaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Asha Rao\",\"email\":\"asha@example.com\",\"phone\":\"9988776644\"}"))
                .andExpect(status().isCreated());

        String loanResponse = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"beneficiaryId\":\"" + beneficiaryId + "\",\"amount\":4500.00,\"termMonths\":9,\"purpose\":\"Home Repair\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = loanResponse.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(patch("/api/loans/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"reviewNotes\":\"Approved for processing\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/loans/{id}/repayments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000.00,\"paymentMode\":\"UPI\",\"reference\":\"REF-9001\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reports/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").exists())
                .andExpect(jsonPath("$.approvedApplications").exists())
                .andExpect(jsonPath("$.outstandingBalance").exists());

        mockMvc.perform(get("/api/loans/{id}/audit-log", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").exists())
                .andExpect(jsonPath("$[0].message").exists());
    }

    @Test
    void filtersLoansByStatusAndPurpose() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"beneficiaryId\":\"" + UUID.randomUUID() + "\",\"amount\":2000.00,\"termMonths\":6,\"purpose\":\"Business Equipment\"}"))
                .andExpect(status().isCreated());

        String approvedResponse = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"beneficiaryId\":\"" + UUID.randomUUID() + "\",\"amount\":3500.00,\"termMonths\":12,\"purpose\":\"Day Ten Education Fees\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String approvedId = approvedResponse.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(patch("/api/loans/{id}/status", approvedId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/loans")
                        .param("status", "approved")
                        .param("purpose", "day ten education"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(approvedId))
                .andExpect(jsonPath("$[0].status").value("APPROVED"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void rejectsInvalidLoanFilters() throws Exception {
        mockMvc.perform(get("/api/loans").param("status", "in-review"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("status must be PENDING, APPROVED, or REJECTED"));
    }

        @Test
        void paginatesFilteredLoans() throws Exception {
                for (int index = 0; index < 3; index++) {
                        mockMvc.perform(post("/api/loans")
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content("{\"beneficiaryId\":\"" + UUID.randomUUID() + "\",\"amount\":1000.00,\"termMonths\":6,\"purpose\":\"Day Eleven Pagination\"}"))
                                        .andExpect(status().isCreated());
                }

                mockMvc.perform(get("/api/loans")
                                                .param("purpose", "day eleven pagination")
                                                .param("page", "1")
                                                .param("size", "2"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].purpose").value("Day Eleven Pagination"))
                                .andExpect(jsonPath("$[1]").doesNotExist());
        }

        @Test
        void capsPageSizeAtFifty() throws Exception {
                for (int index = 0; index < 60; index++) {
                        mockMvc.perform(post("/api/loans")
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content("{\"beneficiaryId\":\"" + UUID.randomUUID() + "\",\"amount\":1000.00,\"termMonths\":6,\"purpose\":\"Day Twelve Pagination\"}"))
                                        .andExpect(status().isCreated());
                }

                mockMvc.perform(get("/api/loans")
                                                .param("purpose", "day twelve pagination")
                                                .param("page", "0")
                                                .param("size", "100"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[50]").doesNotExist());
        }

        @Test
        void rejectsInvalidPagination() throws Exception {
                mockMvc.perform(get("/api/loans").param("page", "-1"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("page must be zero or greater and size must be greater than zero"));
        }

    @Test
    void rejectsInvalidLoanApplication() throws Exception {
        mockMvc.perform(post("/api/loan-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"beneficiaryId\":\"" + UUID.randomUUID() + "\",\"amount\":0,\"termMonths\":0,\"purpose\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("beneficiaryId, amount, termMonths, and purpose are required"));
    }
}
