package com.akshay.expense_splitter.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserProfileRequest(
        @NotBlank
        @Size(max = 50, message = "Username must be 50 characters or fewer")
        String username
) {
}
