package com.itheima.smartcodenote.service;

import com.itheima.smartcodenote.dto.ChangePasswordRequest;
import com.itheima.smartcodenote.dto.LoginRequest;
import com.itheima.smartcodenote.dto.LoginResponse;
import com.itheima.smartcodenote.dto.RegisterRequest;
import com.itheima.smartcodenote.dto.UpdateUserRequest;
import com.itheima.smartcodenote.dto.UserInfoResponse;
import com.itheima.smartcodenote.dto.WxLoginRequest;
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
