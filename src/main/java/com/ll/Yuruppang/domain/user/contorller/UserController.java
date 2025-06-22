package com.ll.Yuruppang.domain.user.contorller;

import com.ll.Yuruppang.domain.user.dto.request.UserLoginRequest;
import com.ll.Yuruppang.domain.user.dto.request.UserRegisterRequest;
import com.ll.Yuruppang.domain.user.dto.response.UserResponse;
import com.ll.Yuruppang.domain.user.service.UserService;
import com.ll.Yuruppang.global.response.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public RsData<UserResponse> createUser(@Valid @RequestBody UserRegisterRequest request) {
        return RsData.success(HttpStatus.OK, userService.createUser(request.pin(), request.username()));
    }

    @PostMapping("/login")
    public RsData<UserResponse> login(@Valid @RequestBody UserLoginRequest request) {
        return RsData.success(HttpStatus.OK, userService.login(request.pin()));
    }

    @PostMapping("/logout")
    public RsData<String> logout() {
        userService.logout();
        return RsData.success(HttpStatus.OK, "성공적으로 로그아웃 되었습니다.");
    }
}
