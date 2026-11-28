package com.switchpay.gateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // the SELLER user who owns this merchant (plain id, users table belongs to security module)
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Column(name = "settlement_account_no", nullable = false, length = 34)
    private String settlementAccountNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MerchantStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getSettlementAccountNo() {
        return settlementAccountNo;
    }

    public void setSettlementAccountNo(String settlementAccountNo) {
        this.settlementAccountNo = settlementAccountNo;
    }

    public MerchantStatus getStatus() {
        return status;
    }

    public void setStatus(MerchantStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
