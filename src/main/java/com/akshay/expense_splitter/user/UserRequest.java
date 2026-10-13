package com.akshay.expense_splitter.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserRequest {
    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String email;


}
