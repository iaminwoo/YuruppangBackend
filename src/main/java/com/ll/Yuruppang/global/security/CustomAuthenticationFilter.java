package com.ll.Yuruppang.global.security;

import com.ll.Yuruppang.domain.user.entity.User;
import com.ll.Yuruppang.domain.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {

    private final UserContext userContext;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    private static final List<String> PUBLIC_URLS = List.of(
        "/api/users/register",
        "/api/users/login"
    );
    private static final List<String> EXCLUDE_URLS = List.of(
    );

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    record AuthTokens(
            String accessToken,
            String refreshToken
    ) {
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // api 로 시작하지 않으면 넘어감
        if (!uri.startsWith("/api")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 열린 API 거나 제외하는 API 면 넘어감
        if (PUBLIC_URLS.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri))
                && EXCLUDE_URLS.stream().noneMatch(pattern -> pathMatcher.match(pattern, uri))) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthTokens authTokens = getAuthTokensFromRequest();

        if (authTokens == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String refreshToken = authTokens.refreshToken;
        String accessToken = authTokens.accessToken;

        User user = userService.getUserFromToken(accessToken);

        if (user == null) {
            user = refreshAccessToken(refreshToken);
        }

        if (user != null) {
            userContext.setLogin(user);
        }

        filterChain.doFilter(request, response);
    }

    private User refreshAccessToken(String refreshToken) {
        User user = userService.getUserFromToken(refreshToken);
        if(user == null) return null;

        String newAccessToken = jwtUtil.createAccessToken(user);

        userContext.setHeader("Authorization", "Bearer " + refreshToken + " " + newAccessToken);
        userContext.addCookie("accessToken", newAccessToken, 15 * 60); // 15분
        return user;
    }

    private AuthTokens getAuthTokensFromRequest() {
        // 요청 헤더에서 Authorization 얻기
        String authorization = userContext.getHeader("Authorization");

        // Authorization null 아니고 Bearer 시작하면 토큰값 얻기
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length());
            String[] tokenBits = token.split(" ", 2);

            if (tokenBits.length == 2) {
                return new AuthTokens(tokenBits[0], tokenBits[1]);
            }
        }

        // 헤더에 토큰이 없다면 쿠키에서 토큰값 얻기
        String refreshToken = userContext.getCookieValue("refreshToken");
        String accessToken = userContext.getCookieValue("accessToken");

        if (refreshToken != null) {
            return new AuthTokens(accessToken, refreshToken);
        }

        return null;
    }
}