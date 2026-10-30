package com.akshay.expense_splitter.exception;

public class InvalidGroupInviteCodeException extends RuntimeException {
    public InvalidGroupInviteCodeException() {
        super("This group code is invalid or has expired.");
    }
}
