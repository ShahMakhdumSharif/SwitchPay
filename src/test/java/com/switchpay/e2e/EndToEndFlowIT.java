package com.switchpay.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.switchpay.common.enums.RoleName;
import com.switchpay.common.enums.TransactionType;
import com.switchpay.security.TestAuth;
import com.switchpay.security.dto.RegisterRequest;
import com.switchpay.security.dto.UpdateUserRequest;
import com.switchpay.security.repository.AuditLogRepository;
import com.switchpay.switching.dto.SubmitTransactionRequest;
import com.switchpay.switching.repository.TransactionRepository;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
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
    private TransactionRepository transactionRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void e1AndE2_atmWithdrawalAndOverLimitDecline_recordsTransactionAndAudit() throws Exception {
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").isNotEmpty())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String reference = json.get("reference").asText();

        // Idempotent replay with same key must return identical transaction reference (no double charge)
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
    void e3AndE7_merchantPaymentRbacSweepAndPreAuthorizeAudit_enforcesRoleMatrixAndUniformApiError()
            throws Exception {
        long initialTxnCount = transactionRepository.count();

        // 1. Register a real BUYER user via public POST /api/auth/register
        RegisterRequest registerBuyer = new RegisterRequest();
        registerBuyer.setEmail("buyer-" + UUID.randomUUID() + "@switchpay.local");
        registerBuyer.setPassword("BuyerPass123!");
        registerBuyer.setFullName("E3 Buyer User");
        registerBuyer.setRole(RoleName.BUYER);

        MvcResult registered = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBuyer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        long buyerUserId = objectMapper.readTree(registered.getResponse().getContentAsString())
                .get("id").asLong();

        // 2. E3 Web Channel Merchant Checkout Payment
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
                        .with(testAuth.asBuyer(buyerUserId, registerBuyer.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").isNotEmpty())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn();

        String ref = objectMapper.readTree(payResult.getResponse().getContentAsString())
                .get("reference").asText();

        assertThat(transactionRepository.count()).isEqualTo(initialTxnCount + 1);

        // 3. E7 RBAC Matrix Sweep across anonymous, BUYER, SELLER, and ADMIN

        // 3a. Anonymous calls to protected endpoints -> 401 with uniform C2 ApiError
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/users"));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // 3b. BUYER calling ADMIN-only endpoints -> 403 with uniform C2 ApiError
        mockMvc.perform(get("/api/users")
                        .with(testAuth.asBuyer(buyerUserId, registerBuyer.getEmail())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(delete("/api/users/" + buyerUserId)
                        .with(testAuth.asBuyer(buyerUserId, registerBuyer.getEmail())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // 3c. SELLER calling ADMIN-only or BUYER/ADMIN-only endpoints -> 403
        mockMvc.perform(get("/api/users")
                        .with(testAuth.asSeller(401L, "seller401@switchpay.local")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/transactions")
                        .with(testAuth.asSeller(401L, "seller401@switchpay.local")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // 3d. BUYER allowed on own profile, own transaction detail, and own paged transaction history
        mockMvc.perform(get("/api/users/" + buyerUserId)
                        .with(testAuth.asBuyer(buyerUserId, registerBuyer.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(buyerUserId));

        mockMvc.perform(get("/api/transactions/" + ref)
                        .with(testAuth.asBuyer(buyerUserId, registerBuyer.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value(ref))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/transactions")
                        .param("page", "0")
                        .param("size", "10")
                        .with(testAuth.asBuyer(buyerUserId, registerBuyer.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // 3e. ADMIN allowed on user CRUD (list, update, deactivate) and full transaction queries matching DB count
        mockMvc.perform(get("/api/users")
                        .with(testAuth.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        UpdateUserRequest updateReq = new UpdateUserRequest();
        updateReq.setFullName("E3 Buyer Updated");
        updateReq.setActive(true);
        updateReq.setRoles(Set.of(RoleName.BUYER));

        mockMvc.perform(put("/api/users/" + buyerUserId)
                        .with(testAuth.asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("E3 Buyer Updated"));

        MvcResult adminTxns = mockMvc.perform(get("/api/transactions")
                        .param("page", "0")
                        .param("size", "50")
                        .with(testAuth.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andReturn();

        long apiTotalElements = objectMapper.readTree(adminTxns.getResponse().getContentAsString())
                .get("totalElements").asLong();
        assertThat(apiTotalElements).isEqualTo(transactionRepository.count());

        // 4. Final Audit Check: every mapped endpoint method on every @RestController in com.switchpay carries @PreAuthorize
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
