package com.sheji.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sheji.common.BusinessException;
import com.sheji.common.JwtUtil;
import com.sheji.controller.dto.LoginDTO;
import com.sheji.controller.dto.RegisterDTO;
import com.sheji.controller.vo.UploadVO;
import com.sheji.controller.vo.UserInfoVO;
import com.sheji.entity.User;
import com.sheji.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final MediaService mediaService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper, JwtUtil jwtUtil, MediaService mediaService) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.mediaService = mediaService;
    }

    /**
     * 旧数据兼容：SHA-256 无盐加密（仅用于校验历史用户，登录成功后会自动升级为 BCrypt）。
     */
    private String encodePwd(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BusinessException(500, "密码加密失败");
        }
    }

    private boolean isBcrypt(String password) {
        return password != null && password.startsWith("$2");
    }

    public String register(RegisterDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() == null || dto.getNickname().isEmpty()
                ? dto.getUsername() : dto.getNickname());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return jwtUtil.generateToken(user.getUserId());
    }

    public String login(LoginDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        if (isBcrypt(user.getPassword())) {
            if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
                throw new BusinessException(400, "密码错误");
            }
        } else {
            // 历史 SHA-256 密码：校验通过后升级为 BCrypt
            if (!encodePwd(dto.getPassword()).equals(user.getPassword())) {
                throw new BusinessException(400, "密码错误");
            }
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);
        }
        return jwtUtil.generateToken(user.getUserId());
    }

    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        return toVO(user);
    }

    public void updateUserInfo(Long userId, String nickname, String avatar) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (avatar != null) {
            user.setAvatar(avatar);
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public String updateAvatar(Long userId, MultipartFile file) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        UploadVO vo = mediaService.upload(file);
        user.setAvatar(vo.getFilePath());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return vo.getFilePath();
    }

    private UserInfoVO toVO(User user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
