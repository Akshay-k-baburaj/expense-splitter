package com.akshay.expense_splitter.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record JoinGroupByCodeRequest(
        @NotBlank
        @Pattern(regexp = "[0-9]{6}", message = "Enter the 6-digit group code")
        String code) { }
