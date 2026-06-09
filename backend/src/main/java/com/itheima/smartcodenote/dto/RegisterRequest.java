package com.itheima.smartcodenote.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "username is required")
    @Size(min = 3, max = 32, message = "username length must be between 3 and 32")
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 32, message = "password length must be between 6 and 32")
    private String password;

    @NotBlank(message = "confirmPassword is required")
    private String confirmPassword;

    @Email(message = "email format is invalid")
    private String email;

    @Size(max = 64, message = "nickname length must be less than or equal to 64")
    private String nickname;
}
