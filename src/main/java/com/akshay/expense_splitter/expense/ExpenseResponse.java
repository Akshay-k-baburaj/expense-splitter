package com.akshay.expense_splitter.expense;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ExpenseResponse(
        Long id,
        Long groupId,
        String description,
        BigDecimal amount,
        String currency,
        Long payerId,
        String payerUsername,
        OffsetDateTime createdAt,
        List<ExpenseSplitResponse> splits,
        ExpenseSplitType splitType) { }
