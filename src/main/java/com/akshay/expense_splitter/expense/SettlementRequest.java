package com.akshay.expense_splitter.expense;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SettlementRequest(
        @NotNull @Positive Long receiverId,
        @NotNull @Positive @Digits(integer = 10, fraction = 3) BigDecimal amount) { }
