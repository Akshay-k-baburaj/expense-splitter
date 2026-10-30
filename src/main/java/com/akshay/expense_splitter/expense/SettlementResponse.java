package com.akshay.expense_splitter.expense;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SettlementResponse(
        Long id,
        Long groupId,
        Long payerId,
        String payerUsername,
        Long receiverId,
        String receiverUsername,
        BigDecimal amount,
        String currency,
        OffsetDateTime createdAt) { }
