package com.akshay.expense_splitter.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ExpenseRequest(
        @NotBlank @Size(max = 255) String description,
        @NotNull @Positive @Digits(integer = 10, fraction = 3) BigDecimal amount,
        @NotEmpty @Size(max = 100) List<@NotNull @Positive Long> participantUserIds,
        Map<@NotNull @Positive Long, @NotNull @Positive @Digits(integer = 10, fraction = 3) BigDecimal> participantAmounts) {
    public ExpenseRequest(String description, BigDecimal amount, List<Long> participantUserIds) {
        this(description, amount, participantUserIds, null);
    }
}
