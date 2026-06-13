package com.itheima.smartcodenote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.smartcodenote.config.properties.FileStorageProperties;
import com.itheima.smartcodenote.dto.ChangePasswordRequest;
import com.itheima.smartcodenote.dto.LoginRequest;
import com.itheima.smartcodenote.dto.LoginResponse;
import com.itheima.smartcodenote.dto.RegisterRequest;
import com.itheima.smartcodenote.dto.UpdateUserRequest;
import com.itheima.smartcodenote.dto.UserInfoResponse;
import com.itheima.smartcodenote.entity.User;
import com.itheima.smartcodenote.exception.BusinessException;
import com.itheima.smartcodenote.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.smartcodenote.config.WechatProperties;
import com.itheima.smartcodenote.dto.WxLoginRequest;
import com.itheima.smartcodenote.service.UserService;
import com.itheima.smartcodenote.util.JwtUtil;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final WechatProperties wechatProperties;
    private final FileStorageProperties fileStorageProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public UserInfoResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("passwords do not match");
        }
        ensureUsernameAvailable(request.getUsername(), null);
        ensureEmailAvailable(request.getEmail(), null);

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setEmail(emptyToNull(request.getEmail()));
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setDeleted(0);
        userMapper.insert(user);
        return toUserInfo(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = findByAccount(request.getAccount());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("invalid account or password");
        }
        return LoginResponse.builder()
                .token(jwtUtil.generateToken(user.getId()))
                .user(toUserInfo(user))
                .build();
    }

    @Override
    @Cacheable(value = "user", key = "#userId")
    public UserInfoResponse getUserInfo(Long userId) {
        return toUserInfo(requireUser(userId));
    }

    @Override
    @CacheEvict(value = "user", key = "#userId")
    public UserInfoResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = requireUser(userId);
        ensureEmailAvailable(request.getEmail(), userId);
        user.setNickname(request.getNickname());
        user.setEmail(emptyToNull(request.getEmail()));
        user.setAvatar(request.getAvatar());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return toUserInfo(user);
    }

    @Override
    public LoginResponse wxLogin(WxLoginRequest request) {
        String openid;
        try {
            openid = fetchOpenid(request.getCode());
        } catch (Exception e) {
            throw new BusinessException("微信登录失败：" + e.getMessage());
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, openid)
                .last("LIMIT 1"));

        if (user == null) {
            LocalDateTime now = LocalDateTime.now();
            user = new User();
            user.setUsername("wx_" + openid.substring(Math.max(0, openid.length() - 8)));
            user.setPassword(passwordEncoder.encode(openid));
            user.setNickname("微信用户");
            user.setOpenid(openid);
            user.setCreateTime(now);
            user.setUpdateTime(now);
            user.setDeleted(0);
            userMapper.insert(user);
        }

        return LoginResponse.builder()
                .token(jwtUtil.generateToken(user.getId()))
                .user(toUserInfo(user))
                .build();
    }

    private String fetchOpenid(String code) throws Exception {
        String url = "https://api.weixin.qq.com/sns/jscode2session"
                + "?appid=" + wechatProperties.getAppId()
                + "&secret=" + wechatProperties.getAppSecret()
                + "&js_code=" + code
                + "&grant_type=authorization_code";

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        JsonNode node = objectMapper.readTree(response.body());

        if (node.has("errcode") && node.get("errcode").asInt() != 0) {
            throw new RuntimeException("errcode: " + node.get("errcode").asInt() + ", errmsg: " + node.get("errmsg").asText());
        }

        return node.get("openid").asText();
    }

    @Override
    @CacheEvict(value = "user", key = "#userId")
    public UserInfoResponse uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("file is required");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException("invalid file");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        if (!ext.equals(".png") && !ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".gif") && !ext.equals(".webp")) {
            throw new BusinessException("only image files are supported");
        }

        try {
            Path uploadRoot = Path.of(fileStorageProperties.getUploadDir(), "avatars").toAbsolutePath().normalize();
            Files.createDirectories(uploadRoot);
            String storedName = UUID.randomUUID() + ext;
            Path target = uploadRoot.resolve(storedName).normalize();
            if (!target.startsWith(uploadRoot)) {
                throw new BusinessException("invalid file path");
            }
            file.transferTo(target);

            String avatarUrl = "/uploads/avatars/" + storedName;
            User user = requireUser(userId);
            user.setAvatar(avatarUrl);
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);
            return toUserInfo(user);
        } catch (IOException e) {
            throw new BusinessException("failed to save avatar file");
        }
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("old password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private User findByAccount(String account) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .and(wrapper -> wrapper.eq(User::getUsername, account).or().eq(User::getEmail, account))
                .last("LIMIT 1"));
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "user not found");
        }
        return user;
    }

    private void ensureUsernameAvailable(String username, Long currentUserId) {
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
        if (existing != null && !existing.getId().equals(currentUserId)) {
            throw new BusinessException("username already exists");
        }
    }

    private void ensureEmailAvailable(String email, Long currentUserId) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .last("LIMIT 1"));
        if (existing != null && !existing.getId().equals(currentUserId)) {
            throw new BusinessException("email already exists");
        }
    }

    private UserInfoResponse toUserInfo(User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .createTime(user.getCreateTime())
                .build();
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
