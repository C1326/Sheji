package com.sheji.controller;

import com.sheji.common.Result;
import com.sheji.common.UserContext;
import com.sheji.controller.dto.LoginDTO;
import com.sheji.controller.dto.RegisterDTO;
import com.sheji.controller.dto.UpdateUserDTO;
import com.sheji.controller.vo.UserInfoVO;
import com.sheji.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        return Result.success(userService.getUserInfo(UserContext.getUserId()));
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody UpdateUserDTO dto) {
        userService.updateUserInfo(UserContext.getUserId(), dto.getNickname(), dto.getAvatar());
        return Result.success();
    }

    @PostMapping("/avatar")
    public Result<String> avatar(@RequestParam("file") MultipartFile file) {
        return Result.success(userService.updateAvatar(UserContext.getUserId(), file));
    }
}
