package com.onedao.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 8, max = 50, message = "Username must be 8 to 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Pattern(regexp = "^(?=[^@]*@[^@]*$)(?=.*_)\\S+$", message = "Email must contain '@' and '_'")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 12, message = "Password must be 8 to 12 characters")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).*$",
                message = "Password needs at least 1 capital letter, 1 small letter, 1 number and 1 special character")
        String password,

        @NotBlank(message = "User type is required")
        @Pattern(regexp = "Standard User|Administrator", message = "User type must be 'Standard User' or 'Administrator'")
        String userType
) {}
