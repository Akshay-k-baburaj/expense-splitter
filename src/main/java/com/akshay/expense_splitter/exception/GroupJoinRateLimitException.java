package com.akshay.expense_splitter.exception;

public class GroupJoinRateLimitException extends RuntimeException {
    public GroupJoinRateLimitException() {
        super("Too many group code attempts. Please try again in 15 minutes.");
    }
}
