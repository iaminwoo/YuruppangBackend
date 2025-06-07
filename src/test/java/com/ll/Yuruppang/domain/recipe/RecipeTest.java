package com.ll.Yuruppang.domain.recipe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.Yuruppang.domain.recipe.entity.Recipe;
import com.ll.Yuruppang.domain.recipe.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
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
    private MockMvc mockMvc;

    @Autowired
    private RecipeRepository recipeRepository;

    private static Long categoryId;
    private static Long recipeId;

    @BeforeEach
    public void addCategory() throws Exception {
        String createBody = """
                {
                  "name": "테스트 카테고리"
                }
                """;

        String response = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
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
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getCreateBody(15)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data.recipeId").isNumber());
    }

    private void addRecipe() throws Exception {
        String response = mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getCreateBody(15)))
                .andExpect(status().isOk())
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

        mockMvc.perform(put("/api/recipes/" + recipeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getCreateBody(20)))
                .andExpect(status().isOk())
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

        mockMvc.perform(delete("/api/recipes/" + recipeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data").value("레시피가 삭제되었습니다."));

        // 삭제 확인
        mockMvc.perform(get("/api/recipes/" + recipeId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("레시피 즐겨찾기")
    public void favoriteRecipe() throws Exception {
        addRecipe();

        mockMvc.perform(patch("/api/recipes/" + recipeId + "/favorite")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data").value("레시피 즐겨찾기 토글이 완료되었습니다."));

        // 즐겨찾기 확인
        Recipe recipe = recipeRepository.findById(recipeId).orElseThrow();
        assertTrue(recipe.isFavorite());
    }
}
