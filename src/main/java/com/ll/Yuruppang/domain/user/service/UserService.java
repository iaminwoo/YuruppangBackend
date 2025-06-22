package com.ll.Yuruppang.domain.user.service;

import com.ll.Yuruppang.domain.user.dto.response.UserResponse;
import com.ll.Yuruppang.domain.user.entity.User;
import com.ll.Yuruppang.domain.user.repository.UserRepository;
import com.ll.Yuruppang.global.exceptions.ErrorCode;
import com.ll.Yuruppang.global.security.JwtUtil;
import com.ll.Yuruppang.global.security.UserContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserContext userContext;

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(ErrorCode.USER_NOT_FOUND::throwServiceException);
    }

    @Transactional
    public UserResponse createUser(String pin, String username, HttpServletResponse response) {
        String pinHash = passwordEncoder.encode(pin);

        User newUser = User.builder()
                .username(username)
                .pinHash(pinHash)
                .build();

        userRepository.save(newUser);

        return loginWithUserAndReturnResponse(newUser, response);
    }

    @Transactional
    public UserResponse login(String rawPin, HttpServletResponse response) {
        return loginWithUserAndReturnResponse(findByPin(rawPin), response);
    }

    private User findByPin(String rawPin) {
        return userRepository.findAll().stream()
                .filter(user -> passwordEncoder.matches(rawPin, user.getPinHash()))
                .findFirst()
                .orElseThrow(ErrorCode.USER_NOT_FOUND::throwServiceException);
    }

    private UserResponse loginWithUserAndReturnResponse(User actor, HttpServletResponse response) {
        String accessToken = jwtUtil.createAccessToken(actor);
        String refreshToken = jwtUtil.createRefreshToken(actor);

        // ✅ 쿠키에 저장
        addCookie(response, "accessToken", accessToken, 15 * 60); // 15분
        addCookie(response, "refreshToken", refreshToken, 7 * 24 * 60 * 60); // 7일

        return new UserResponse(actor.getId(), actor.getUsername());
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        response.addCookie(cookie);
    }

    public User getUserFromToken(String token) {
        Map<String, Object> payload = payload(token);

        if (ObjectUtils.isEmpty(payload)) {
            return null;
        }

        return User.builder()
                .id(((Number) payload.get("id")).longValue())
                .username((String) payload.get("username"))
                .build();
    }

    public Map<String, Object> payload(String accessToken) {
        Map<String, Object> parsedPayload = jwtUtil.parse(accessToken);

        if (ObjectUtils.isEmpty(parsedPayload)) return null;

        return Map.of("id", parsedPayload.get("id"), "username", parsedPayload.get("username"));
    }

    @Transactional(readOnly = true)
    public void logout() {
        SecurityContextHolder.clearContext();
        userContext.deleteCookie("accessToken");
        userContext.deleteCookie("refreshToken");
    }
}
