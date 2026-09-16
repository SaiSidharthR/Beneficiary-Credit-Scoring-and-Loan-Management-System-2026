package com.saisidharth.loanmanagement.beneficiary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class BeneficiaryControllerTest {

    @Autowired
        private WebApplicationContext webApplicationContext;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        }

    @Test
    void createsReadsAndDeletesBeneficiary() throws Exception {
        String response = mockMvc.perform(post("/api/beneficiaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Asha Rao\",\"email\":\"asha@example.com\",\"phone\":\"9876543210\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Asha Rao"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = response.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(get("/api/beneficiaries/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("asha@example.com"));

        mockMvc.perform(delete("/api/beneficiaries/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/beneficiaries/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsIncompleteBeneficiary() throws Exception {
        mockMvc.perform(post("/api/beneficiaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"asha@example.com\",\"phone\":\"9876543210\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name, email, and phone are required"));
    }
}
