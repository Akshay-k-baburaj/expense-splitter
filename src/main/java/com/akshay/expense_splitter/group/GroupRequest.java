package com.akshay.expense_splitter.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupRequest {
    @NotBlank
    @NotNull
    @Size(max = 100, message = "Group name must be 100 characters or fewer")
    private String groupName;

    private String description;

    private String currencyCode;

    public GroupRequest(String groupName, String description) {
        this(groupName, description, null);
    }
}
