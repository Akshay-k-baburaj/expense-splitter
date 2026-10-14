package com.akshay.expense_splitter.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GroupRequest {
    @NotBlank
    @NotNull
    private String groupName;

    private String description;
}
