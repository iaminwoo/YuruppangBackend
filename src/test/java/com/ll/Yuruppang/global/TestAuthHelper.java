package com.ll.Yuruppang.global;

import com.ll.Yuruppang.domain.user.service.UserService;
import com.ll.Yuruppang.global.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class TestAuthHelper {
    private final MockMvc mvc;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    private Cookie accessToken;
    private Cookie refreshToken;

    public TestAuthHelper(MockMvc mvc, UserService userService, JwtUtil jwtUtil) {
        this.mvc = mvc;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    public void createTestUser() throws Exception {
        String body = """
                {
                    "pin" : "9876",
                    "username" : "TestUser"
                }
                """;

        MvcResult result = mvc.perform(post("/api/users/register")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        accessToken = result.getResponse().getCookie("accessToken");
        refreshToken = result.getResponse().getCookie("refreshToken");
    }

    public ResultActions requestWithAuth(MockHttpServletRequestBuilder request) throws Exception {
                return mvc.perform(request
                    .cookie(accessToken)
                    .cookie(refreshToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
    }

    public ResultActions requestWithAuthNoStatus(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request
                        .cookie(accessToken)
                        .cookie(refreshToken)
                        .contentType(MediaType.APPLICATION_JSON));
    }

}
