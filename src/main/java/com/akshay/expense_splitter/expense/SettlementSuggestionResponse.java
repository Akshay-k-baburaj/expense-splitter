package com.akshay.expense_splitter.expense;

import java.math.BigDecimal;

public record SettlementSuggestionResponse(
        Long fromUserId,
        String fromUsername,
        Long toUserId,
        String toUsername,
        BigDecimal amount) { }
