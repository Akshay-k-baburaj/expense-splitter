package com.akshay.expense_splitter.expense;

import com.akshay.expense_splitter.exception.FinancialOperationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CurrencySupportTest {
    @Test
    void normalizesIsoCurrencyCodeAndUsesItsFractionDigits() {
        assertEquals("USD", CurrencySupport.normalize(" usd "));
        assertEquals(0, CurrencySupport.fractionDigits("JPY"));
        assertEquals(3, CurrencySupport.fractionDigits("KWD"));
    }

    @Test
    void rejectsAmountsWithMoreFractionDigitsThanTheCurrencyAllows() {
        assertThrows(FinancialOperationException.class,
                () -> CurrencySupport.amount(new BigDecimal("12.345"), "INR", "Expense amount"));
        assertEquals(new BigDecimal("12"), CurrencySupport.amount(new BigDecimal("12.00"), "JPY", "Expense amount"));
    }

    @Test
    void rejectsUnknownCurrencyCodes() {
        assertThrows(FinancialOperationException.class, () -> CurrencySupport.normalize("ZZZ"));
    }
}
