package com.ll.Yuruppang.domain.recipe;

import com.ll.Yuruppang.global.TestAuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class PanTest {

    @Autowired
    private TestAuthHelper testAuthHelper;

    @BeforeEach
    public void setUp() throws Exception {
        createTestUser();
    }

    private void createTestUser() throws Exception {
        testAuthHelper.createTestUser();
    }

    @Test
    @DisplayName("틀 등록 테스트")
    void createPan() throws Exception {
        createTestPan();
    }

    private void createTestPan() throws Exception {
        String body = """
                {
                    "panType": "SQUARE",
                    "width": 10,
                    "length": 10,
                    "height": 10
                }
                """;

        MockHttpServletRequestBuilder request = post("/api/pans")
                .content(body);

        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data.panType").value("SQUARE"))
                .andExpect(jsonPath("$.data.measurements").value("가로: 10cm / 세로: 10cm / 높이: 10cm"));
    }
}
