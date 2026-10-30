package com.akshay.expense_splitter.expense;

import com.akshay.expense_splitter.exception.FinancialOperationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;

public final class CurrencySupport {
    private CurrencySupport() { }

    public static String normalizeOrDefault(String requestedCode, String fallbackCode) {
        if (requestedCode == null || requestedCode.isBlank()) return normalize(fallbackCode);
        return normalize(requestedCode);
    }

    public static String normalize(String requestedCode) {
        String code = requestedCode == null ? "" : requestedCode.trim().toUpperCase(Locale.ROOT);
        try {
            Currency currency = Currency.getInstance(code);
            int digits = currency.getDefaultFractionDigits();
            if (digits < 0 || digits > 3) throw new IllegalArgumentException();
            return currency.getCurrencyCode();
        } catch (IllegalArgumentException exception) {
            throw new FinancialOperationException("Choose a supported ISO currency code.");
        }
    }

    public static int fractionDigits(String currencyCode) {
        return Currency.getInstance(normalize(currencyCode)).getDefaultFractionDigits();
    }

    public static BigDecimal amount(BigDecimal value, String currencyCode, String fieldName) {
        int digits = fractionDigits(currencyCode);
        try {
            return value.setScale(digits, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new FinancialOperationException(fieldName + " supports at most " + digits
                    + (digits == 1 ? " decimal place" : " decimal places") + " for " + currencyCode + ".");
        }
    }
}
