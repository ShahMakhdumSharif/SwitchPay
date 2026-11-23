package com.switchpay.switching;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.switchpay.common.enums.TransactionType;
import com.switchpay.security.TestAuth;
import com.switchpay.switching.dto.SubmitTransactionRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SwitchingIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAuth testAuth;

    @Test
    void submitTransaction_validRequest_returnsApprovedOutcome() throws Exception {
        SubmitTransactionRequest body = new SubmitTransactionRequest();
        body.setChannelCode("ATM");
        body.setType(TransactionType.CASH_WITHDRAWAL);
        body.setSourceAccountNo("ACC-1001");
        body.setAmount(new BigDecimal("1500.00"));
        body.setIdempotencyKey("it4-" + UUID.randomUUID());

        mockMvc.perform(post("/api/transactions")
                        .with(testAuth.asBuyer(201L, "buyer201@switchpay.local"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").isNotEmpty())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void getTransaction_anotherUsersTransaction_returns403() throws Exception {
        SubmitTransactionRequest body = new SubmitTransactionRequest();
        body.setChannelCode("MOBILE");
        body.setType(TransactionType.TRANSFER);
        body.setSourceAccountNo("ACC-2001");
        body.setDestinationAccountNo("ACC-2002");
        body.setAmount(new BigDecimal("500.00"));
        body.setIdempotencyKey("it5-" + UUID.randomUUID());

        MvcResult created = mockMvc.perform(post("/api/transactions")
                        .with(testAuth.asBuyer(201L, "buyer201@switchpay.local"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        String reference = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("reference").asText();

        mockMvc.perform(get("/api/transactions/" + reference)
                        .with(testAuth.asBuyer(999L, "otherbuyer@switchpay.local")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
