package com.akshay.expense_splitter.expense;

import java.math.BigDecimal;

public record MemberBalanceResponse(Long userId, String username, BigDecimal netBalance) { }
