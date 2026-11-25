package com.switchpay.banking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.switchpay.banking.dto.CreateAccountRequest;
import com.switchpay.banking.enums.AccountType;
import com.switchpay.security.TestAuth;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BankingIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAuth testAuth;

    @Test
    void createAccount_asAdmin_returns201() throws Exception {
        String accountNo = "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        CreateAccountRequest request = new CreateAccountRequest(
                accountNo,
                101L,
                AccountType.SAVINGS,
                new BigDecimal("5000.00"),
                "BDT"
        );

        mockMvc.perform(post("/api/accounts")
                        .with(testAuth.asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNo").value(accountNo))
                .andExpect(jsonPath("$.balance").value(5000.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.type").value("SAVINGS"));
    }

    @Test
    void getAccount_otherUsersAccountAsBuyer_returns403() throws Exception {
        mockMvc.perform(get("/api/accounts/ACC-1001")
                        .with(testAuth.asBuyer(999L, "otherbuyer@switchpay.local")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
