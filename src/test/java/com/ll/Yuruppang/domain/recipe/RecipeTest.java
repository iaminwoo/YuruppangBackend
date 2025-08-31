package com.ll.Yuruppang.domain.recipe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.Yuruppang.domain.recipe.entity.Recipe;
import com.ll.Yuruppang.domain.recipe.repository.RecipeRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class RecipeTest {

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Autowired
    private RecipeRepository recipeRepository;

    private static Long categoryId;
    private static Long recipeId;

    @BeforeEach
    public void setUp() throws Exception {
        createTestUser();
        addCategory();
    }

    private void createTestUser() throws Exception {
        testAuthHelper.createTestUser();
    }

    private void addCategory() throws Exception {
        String createBody = """
                {
                  "name": "테스트 카테고리"
                }
                """;

        MockHttpServletRequestBuilder request = post("/api/categories")
                .content(createBody);

        String response = testAuthHelper.requestWithAuth(request)
                .andReturn()
                .getResponse()
                .getContentAsString();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(response);
        JsonNode dataNode = rootNode.get("data");
        categoryId = dataNode.get("categoryId").asLong();
    }

    private String getCreateBody(int output) {
        return String.format("""
                {
                    "name": "초코머핀",
                    "description": "부드럽고 진한 초콜릿 머핀 레시피입니다.",
                    "outputQuantity": %d,
                    "parts": [
                        {
                            "partName": "기본",
                            "ingredients": [
                                {
                                    "ingredientName": "박력분",
                                    "quantity": 500.00,
                                    "unit": "G"
                                },
                                {
                                    "ingredientName": "코코아파우더",
                                    "quantity": 40.00,
                                    "unit": "G"
                                },
                                {
                                    "ingredientName": "설탕",
                                    "quantity": 150.00,
                                    "unit": "G"
                                },
                                {
                                    "ingredientName": "버터",
                                    "quantity": 300.00,
                                    "unit": "G"
                                },
                                {
                                    "ingredientName": "달걀",
                                    "quantity": 5.00,
                                    "unit": "개"
                                }
                            ]
                        }
                    ],
                  "categoryId": %d
                }
                """, output, categoryId);
    }

    @Test
    @DisplayName("레시피 등록")
    public void createRecipe() throws Exception {
        MockHttpServletRequestBuilder request = post("/api/recipes")
                .content(getCreateBody(15));

        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data.recipeId").isNumber());
    }

    private void addRecipe() throws Exception {
        MockHttpServletRequestBuilder request = post("/api/recipes")
                .content(getCreateBody(15));

        String response = testAuthHelper.requestWithAuth(request)
                .andReturn()
                .getResponse()
                .getContentAsString();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(response);
        JsonNode dataNode = rootNode.get("data");
        recipeId = dataNode.get("recipeId").asLong();
    }

    @Test
    @DisplayName("레시피 수정")
    public void modifyRecipe() throws Exception {
        addRecipe();

        MockHttpServletRequestBuilder request = put("/api/recipes/" + recipeId)
                .content(getCreateBody(20));

        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data").value("레시피가 수정되었습니다."));

        Recipe recipe = recipeRepository.findById(recipeId).orElseThrow();
        assertEquals(20, recipe.getOutputQuantity());
    }

    @Test
    @DisplayName("레시피 삭제")
    public void deleteRecipe() throws Exception {
        addRecipe();

        MockHttpServletRequestBuilder request = delete("/api/recipes/" + recipeId);

        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data").value("레시피가 삭제되었습니다."));

        // 삭제 확인
        MockHttpServletRequestBuilder request2 = get("/api/recipes/" + recipeId);

        testAuthHelper.requestWithAuthNoStatus(request2)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("레시피 즐겨찾기")
    public void favoriteRecipe() throws Exception {
        addRecipe();

        MockHttpServletRequestBuilder request = patch("/api/recipes/" + recipeId + "/favorite");

        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data").value("레시피 즐겨찾기 토글이 완료되었습니다."));

        // 즐겨찾기 확인
        Recipe recipe = recipeRepository.findById(recipeId).orElseThrow();
        assertTrue(recipe.isFavorite());
    }
}
