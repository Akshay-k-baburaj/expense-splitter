package com.akshay.expense_splitter.expense;

import java.math.BigDecimal;

public record ExpenseSplitResponse(Long userId, String username, BigDecimal amount) { }
