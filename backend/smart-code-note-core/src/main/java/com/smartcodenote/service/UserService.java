package com.smartcodenote.service;

import com.smartcodenote.dto.ChangePasswordRequest;
import com.smartcodenote.dto.LoginRequest;
import com.smartcodenote.dto.LoginResponse;
import com.smartcodenote.dto.RegisterRequest;
import com.smartcodenote.dto.UpdateUserRequest;
import com.smartcodenote.dto.UserInfoResponse;
import com.smartcodenote.dto.WxLoginRequest;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserInfoResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse wxLogin(WxLoginRequest request);

    UserInfoResponse getUserInfo(Long userId);

    UserInfoResponse updateUser(Long userId, UpdateUserRequest request);

    UserInfoResponse uploadAvatar(Long userId, MultipartFile file);

    void changePassword(Long userId, ChangePasswordRequest request);
}
