package com.akshay.expense_splitter.exception;

import java.time.OffsetDateTime;

public record ErrorResponse(
        OffsetDateTime time,
        int status,
        String error,
        String message
) {
}
