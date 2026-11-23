package com.switchpay.common.dto;

import com.switchpay.common.enums.EntryType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostingRequest {

    private final Long transactionId;
    private final String transactionRef;
    private final Long adjustmentId;
    private final List<PostingLine> lines;

    private PostingRequest(Long transactionId, String transactionRef, Long adjustmentId, List<PostingLine> lines) {
        this.transactionId = transactionId;
        this.transactionRef = transactionRef;
        this.adjustmentId = adjustmentId;
        this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public Long getAdjustmentId() {
        return adjustmentId;
    }

    public List<PostingLine> getLines() {
        return lines;
    }

    public static class PostingLine {
        private final Long accountId;
        private final String accountNo;
        private final EntryType entryType;
        private final BigDecimal amount;

        public PostingLine(Long accountId, String accountNo, EntryType entryType, BigDecimal amount) {
            this.accountId = accountId;
            this.accountNo = accountNo;
            this.entryType = entryType;
            this.amount = amount;
        }

        public Long getAccountId() {
            return accountId;
        }

        public String getAccountNo() {
            return accountNo;
        }

        public EntryType getEntryType() {
            return entryType;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }

    public static class Builder {
        private Long transactionId;
        private String transactionRef;
        private Long adjustmentId;
        private final List<PostingLine> lines = new ArrayList<>();

        public Builder transactionId(Long transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder transactionRef(String transactionRef) {
            this.transactionRef = transactionRef;
            return this;
        }

        public Builder adjustmentId(Long adjustmentId) {
            this.adjustmentId = adjustmentId;
            return this;
        }

        public Builder line(String accountNo, EntryType entryType, BigDecimal amount) {
            this.lines.add(new PostingLine(null, accountNo, entryType, amount));
            return this;
        }

        public Builder line(Long accountId, String accountNo, EntryType entryType, BigDecimal amount) {
            this.lines.add(new PostingLine(accountId, accountNo, entryType, amount));
            return this;
        }

        public Builder debit(String accountNo, BigDecimal amount) {
            return line(accountNo, EntryType.DEBIT, amount);
        }

        public Builder credit(String accountNo, BigDecimal amount) {
            return line(accountNo, EntryType.CREDIT, amount);
        }

        public PostingRequest build() {
            return new PostingRequest(transactionId, transactionRef, adjustmentId, lines);
        }
    }
}
