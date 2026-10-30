package com.akshay.expense_splitter.exception;

public class ExpenseNotFoundException extends RuntimeException {
    public ExpenseNotFoundException() { super("Expense not found."); }
}
