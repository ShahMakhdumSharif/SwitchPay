package com.switchpay.common.dto;

import com.switchpay.common.enums.TransactionStatus;
import com.switchpay.common.enums.TransactionType;
import java.time.Instant;

public class TransactionFilter {

    private TransactionStatus status;
    private TransactionType type;
    private String channelCode;
    private Long channelId;
    private Long userId;
    private Long merchantId;
    private Instant fromDate;
    private Instant toDate;
    private Integer stuckOlderThanMinutes;
    private int page = 0;
    private int size = 20;

    public TransactionFilter() {
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Instant getFromDate() {
        return fromDate;
    }

    public void setFromDate(Instant fromDate) {
        this.fromDate = fromDate;
    }

    public Instant getToDate() {
        return toDate;
    }

    public void setToDate(Instant toDate) {
        this.toDate = toDate;
    }

    public Integer getStuckOlderThanMinutes() {
        return stuckOlderThanMinutes;
    }

    public void setStuckOlderThanMinutes(Integer stuckOlderThanMinutes) {
        this.stuckOlderThanMinutes = stuckOlderThanMinutes;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
