package com.ll.Yuruppang.global.security;

import com.ll.Yuruppang.domain.user.entity.User;
import com.ll.Yuruppang.domain.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RequestScope
@Component
@RequiredArgsConstructor
public class UserContext {

    private final UserService userService;
    private final HttpServletResponse resp;
    private final HttpServletRequest req;

    // 쿠키 생성
    public void setCookie(String name, String value) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .build();

        resp.addHeader("Set-Cookie", cookie.toString());
    }

    // 요청에서 헤더 얻어오기
    public String getHeader(String name) {
        return req.getHeader(name);
    }

    // 쿠키 값 얻기
    public String getCookieValue(String name) {
        return Optional
                .ofNullable(req.getCookies())
                .stream()
                .flatMap(Arrays::stream)
                .filter(cookie -> cookie.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    // 응답 헤더 설정
    public void setHeader(String name, String value) {
        resp.setHeader(name, value);
    }

    // 로그인 설정
    public void setLogin(User user) {
        UserDetails userDetails = new SecurityUser(
                user.getId(),
                user.getUsername(),
                List.of()
        );

        // 인증
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        // 인증 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // 쿠키 삭제
    public void deleteCookie(String name) {
        ResponseCookie cookie = ResponseCookie.from(name, null)
                .path("/")
                .httpOnly(true)
                .maxAge(0)
                .build();

        resp.addHeader("Set-Cookie", cookie.toString());
    }

    // 요청을 보낸 사용자의 인증 정보를 가져와 해당 사용자를 조회, 시큐리티 내부에 인증된 사용자를 반환
    public User getActor() {
        return Optional.ofNullable(
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                )
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof SecurityUser)
                .map(principal -> (SecurityUser) principal)
                .map(securityUser -> new User(securityUser.getId(), securityUser.getUsername()))
                .orElse(null);
    }

    public User getActualActor() {
        User actor = getActor();
        return userService.findById(actor.getId());
    }
}