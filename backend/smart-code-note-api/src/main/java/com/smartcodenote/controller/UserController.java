package com.smartcodenote.controller;

import com.smartcodenote.common.Result;
import com.smartcodenote.dto.ChangePasswordRequest;
import com.smartcodenote.dto.LoginRequest;
import com.smartcodenote.dto.LoginResponse;
import com.smartcodenote.dto.RegisterRequest;
import com.smartcodenote.dto.UpdateUserRequest;
import com.smartcodenote.dto.UserInfoResponse;
import com.smartcodenote.dto.WxLoginRequest;
import com.smartcodenote.security.CurrentUser;
import com.smartcodenote.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<UserInfoResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    @PostMapping("/wx-login")
    public Result<LoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return Result.success(userService.wxLogin(request));
    }

    @GetMapping("/info")
    public Result<UserInfoResponse> info() {
        return Result.success(userService.getUserInfo(CurrentUser.getUserId()));
    }

    @PutMapping("/update")
    public Result<UserInfoResponse> update(@Valid @RequestBody UpdateUserRequest request) {
        return Result.success(userService.updateUser(CurrentUser.getUserId(), request));
    }

    @PostMapping("/avatar")
    public Result<UserInfoResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(userService.uploadAvatar(CurrentUser.getUserId(), file));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(CurrentUser.getUserId(), request);
        return Result.success();
    }
}
