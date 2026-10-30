package com.akshay.expense_splitter.expense;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EqualExpenseSplitCalculatorTest {
    private final EqualExpenseSplitCalculator calculator = new EqualExpenseSplitCalculator();

    @Test
    void distributesRemainderPaiseInAscendingUserIdOrder() {
        var result = calculator.allocate(new BigDecimal("10.00"), List.of(9L, 2L, 5L));

        assertEquals(new BigDecimal("3.34"), result.get(2L));
        assertEquals(new BigDecimal("3.33"), result.get(5L));
        assertEquals(new BigDecimal("3.33"), result.get(9L));
        assertEquals(new BigDecimal("10.00"), result.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void allocatesSingleParticipantFullAmount() {
        var result = calculator.allocate(new BigDecimal("0.01"), List.of(4L));

        assertEquals(new BigDecimal("0.01"), result.get(4L));
    }

    @Test
    void allocatesWholeCurrencyUnitsForZeroFractionCurrencies() {
        var result = calculator.allocate(new BigDecimal("5"), List.of(3L, 1L, 2L), 0);

        assertEquals(new BigDecimal("2"), result.get(1L));
        assertEquals(new BigDecimal("2"), result.get(2L));
        assertEquals(new BigDecimal("1"), result.get(3L));
    }

    @Test
    void allocatesThreeFractionCurrencyUnitsWithoutLosingRemainders() {
        var result = calculator.allocate(new BigDecimal("1.000"), List.of(3L, 1L, 2L), 3);

        assertEquals(new BigDecimal("0.334"), result.get(1L));
        assertEquals(new BigDecimal("0.333"), result.get(2L));
        assertEquals(new BigDecimal("0.333"), result.get(3L));
    }
}
