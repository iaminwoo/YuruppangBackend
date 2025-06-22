package com.ll.Yuruppang.domain.user.service;

import com.ll.Yuruppang.domain.user.dto.response.UserResponse;
import com.ll.Yuruppang.domain.user.entity.User;
import com.ll.Yuruppang.domain.user.repository.UserRepository;
import com.ll.Yuruppang.global.exceptions.ErrorCode;
import com.ll.Yuruppang.global.security.JwtUtil;
import com.ll.Yuruppang.global.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.Map;
import java.util.Objects;

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
    public UserResponse createUser(String pin, String username) {
        String pinHash = passwordEncoder.encode(pin);

        User newUser = User.builder()
                .username(username)
                .pinHash(pinHash)
                .build();

        userRepository.save(newUser);

        return loginWithUserAndReturnResponse(newUser);
    }

    @Transactional
    public UserResponse login(String rawPin) {
        return loginWithUserAndReturnResponse(findByPin(rawPin));
    }

    private User findByPin(String rawPin) {
        return userRepository.findAll().stream()
                .filter(user -> passwordEncoder.matches(rawPin, user.getPinHash()))
                .findFirst()
                .orElseThrow(ErrorCode.USER_NOT_FOUND::throwServiceException);
    }

    private UserResponse loginWithUserAndReturnResponse(User actor) {
        String accessToken = jwtUtil.createAccessToken(actor);
        String refreshToken = jwtUtil.createRefreshToken(actor);

        // ✅ 쿠키에 저장
        userContext.addCookie("accessToken", accessToken, 15 * 60); // 15분
        userContext.addCookie("refreshToken", refreshToken, 7 * 24 * 60 * 60); // 7일

        return new UserResponse(actor.getId(), actor.getUsername());
    }

    public User getUserFromToken(String token) {
        if(Objects.isNull(token)) return null;

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
