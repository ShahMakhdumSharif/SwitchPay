package com.switchpay.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.switchpay.common.enums.TransactionType;
import com.switchpay.security.TestAuth;
import com.switchpay.security.repository.AuditLogRepository;
import com.switchpay.switching.dto.SubmitTransactionRequest;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EndToEndFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAuth testAuth;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void e1_atmWithdrawalAndOverLimitDecline_recordsTransactionAndAudit() throws Exception {
        long initialAudits = auditLogRepository.count();

        SubmitTransactionRequest withdrawal = new SubmitTransactionRequest();
        withdrawal.setChannelCode("ATM");
        withdrawal.setType(TransactionType.CASH_WITHDRAWAL);
        withdrawal.setSourceAccountNo("ACC-1001");
        withdrawal.setAmount(new BigDecimal("2500.00"));
        String idemKey = "e1-atm-" + UUID.randomUUID();
        withdrawal.setIdempotencyKey(idemKey);

        MvcResult result = mockMvc.perform(post("/api/transactions")
                        .with(testAuth.asBuyer(301L, "e1buyer@switchpay.local"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawal)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").isNotEmpty())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String reference = json.get("reference").asText();

        // Idempotent replay with same key must return identical transaction reference
        mockMvc.perform(post("/api/transactions")
                        .with(testAuth.asBuyer(301L, "e1buyer@switchpay.local"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value(reference))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Over-limit withdrawal (E2) must return DECLINED with reason, not HTTP error
        SubmitTransactionRequest overLimit = new SubmitTransactionRequest();
        overLimit.setChannelCode("ATM");
        overLimit.setType(TransactionType.CASH_WITHDRAWAL);
        overLimit.setSourceAccountNo("ACC-1001");
        overLimit.setAmount(new BigDecimal("250000.00"));
        overLimit.setIdempotencyKey("e2-over-" + UUID.randomUUID());

        mockMvc.perform(post("/api/transactions")
                        .with(testAuth.asBuyer(301L, "e1buyer@switchpay.local"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overLimit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.declineReason").isNotEmpty());

        assertThat(auditLogRepository.count()).isGreaterThan(initialAudits);
    }

    @Test
    void e3_webMerchantPaymentAndRbacAudit_enforcesMethodSecurityAndTracksStatus() throws Exception {
        SubmitTransactionRequest payment = new SubmitTransactionRequest();
        payment.setChannelCode("WEB");
        payment.setType(TransactionType.MERCHANT_PAYMENT);
        payment.setSourceAccountNo("ACC-2001");
        payment.setDestinationAccountNo("SYS-MERCHANT-PAYABLE");
        payment.setAmount(new BigDecimal("1850.00"));
        payment.setMerchantId(10L);
        payment.setCheckoutSessionId(20L);
        payment.setIdempotencyKey("e3-web-" + UUID.randomUUID());

        MvcResult payResult = mockMvc.perform(post("/api/transactions")
                        .with(testAuth.asBuyer(302L, "e3buyer@switchpay.local"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").isNotEmpty())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn();

        String ref = objectMapper.readTree(payResult.getResponse().getContentAsString())
                .get("reference").asText();

        // Owner BUYER can inspect transaction status detail
        mockMvc.perform(get("/api/transactions/" + ref)
                        .with(testAuth.asBuyer(302L, "e3buyer@switchpay.local")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value(ref))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // ADMIN can inspect all transactions via paged C3 endpoint
        mockMvc.perform(get("/api/transactions")
                        .param("page", "0")
                        .param("size", "10")
                        .param("channel", "WEB")
                        .param("status", "APPROVED")
                        .with(testAuth.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // Audit check: every mapped method on every @RestController in com.switchpay must have @PreAuthorize
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(RestController.class);
        int auditedMethods = 0;
        for (Object bean : controllers.values()) {
            Class<?> targetClass = bean.getClass();
            if (targetClass.getName().contains("$$")) {
                targetClass = targetClass.getSuperclass();
            }
            if (!targetClass.getPackageName().startsWith("com.switchpay")) {
                continue;
            }
            boolean classHasPreAuthorize = targetClass.isAnnotationPresent(PreAuthorize.class);
            for (Method method : targetClass.getDeclaredMethods()) {
                boolean isEndpoint = method.isAnnotationPresent(GetMapping.class)
                        || method.isAnnotationPresent(PostMapping.class)
                        || method.isAnnotationPresent(PutMapping.class)
                        || method.isAnnotationPresent(DeleteMapping.class)
                        || method.isAnnotationPresent(PatchMapping.class)
                        || method.isAnnotationPresent(RequestMapping.class);
                if (isEndpoint) {
                    auditedMethods++;
                    boolean hasPreAuthorize = classHasPreAuthorize || method.isAnnotationPresent(PreAuthorize.class);
                    assertThat(hasPreAuthorize)
                            .as("Controller method %s.%s must carry @PreAuthorize",
                                    targetClass.getSimpleName(), method.getName())
                            .isTrue();
                }
            }
        }
        assertThat(auditedMethods).isGreaterThanOrEqualTo(10);
    }
}
