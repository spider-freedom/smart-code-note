package com.smartcodenote.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(max = 64, message = "nickname length must be less than or equal to 64")
    private String nickname;

    @Email(message = "email format is invalid")
    private String email;

    @Size(max = 255, message = "avatar length must be less than or equal to 255")
    private String avatar;
}
