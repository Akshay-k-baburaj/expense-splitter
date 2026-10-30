package com.akshay.expense_splitter.exception;

public class LegacyAccountLinkRequiredException extends RuntimeException {
    public LegacyAccountLinkRequiredException(String message) {
        super(message);
    }
}
