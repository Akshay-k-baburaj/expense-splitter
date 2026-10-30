package com.akshay.expense_splitter.group;

import jakarta.validation.constraints.NotNull;

public record TransferGroupOwnershipRequest(@NotNull Long userId) {
}
