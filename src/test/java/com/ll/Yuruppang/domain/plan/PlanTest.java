package com.ll.Yuruppang.domain.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.Yuruppang.domain.inventory.entity.Ingredient;
import com.ll.Yuruppang.domain.inventory.entity.IngredientLog;
import com.ll.Yuruppang.domain.inventory.repository.IngredientRepository;
import com.ll.Yuruppang.domain.inventory.repository.LogRepository;
import com.ll.Yuruppang.domain.plan.repository.PlanRepository;
import com.ll.Yuruppang.domain.recipe.entity.Recipe;
import com.ll.Yuruppang.domain.recipe.entity.RecipePart;
import com.ll.Yuruppang.domain.recipe.entity.RecipePartIngredient;
import com.ll.Yuruppang.domain.recipe.entity.RecipeType;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class PlanTest {

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Autowired
    private PlanRepository planRepository;
    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private LogRepository logRepository;

    private static Long categoryId;
    private static Long recipeId;
    private static Long planId;

    @BeforeEach
    public void setUp() throws Exception {
        createTestUser();
        addRecipe();
    }

    public void createTestUser() throws Exception {
        testAuthHelper.createTestUser();
    }

    private String getRecipeCreateBody() {
        return String.format("""
                {
                    "name": "테스트 초코머핀 레시피",
                    "description": "부드럽고 진한 초콜릿 머핀 레시피입니다.",
                    "outputQuantity": 15,
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
                """, categoryId);
    }

    public void addRecipe() throws Exception {
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

        MockHttpServletRequestBuilder request2 = post("/api/recipes")
                .content(getRecipeCreateBody());

        String response2 = testAuthHelper.requestWithAuth(request2)
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode rootNode2 = objectMapper.readTree(response2);
        JsonNode dataNode2 = rootNode2.get("data");
        recipeId = dataNode2.get("recipeId").asLong();
    }

    private void createTestPlan() throws Exception {
        String body = String.format("""
                {
                  "memo": "2025년 6월 6일 생산 계획",
                  "recipes": [%s]
                }
                """, recipeId);

        MockHttpServletRequestBuilder request = post("/api/plans")
                .content(body);

        String response = testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data.planId").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(response);
        JsonNode dataNode = rootNode.get("data");
        planId = dataNode.get("planId").asLong();
    }

    @Test
    @DisplayName("플랜 등록")
    public void createPlan() throws Exception {
        String body = String.format("""
                {
                  "memo": "2025년 6월 6일 생산 계획",
                  "recipes": [%s]
                }
                """, recipeId);

        MockHttpServletRequestBuilder request = post("/api/plans")
                .content(body);

        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data.planId").isNumber());
    }

    @Test
    @DisplayName("플랜 레시피 수량 변경, 재료량 변경")
    public void modifyOutput() throws Exception {
        createTestPlan();

        // 레시피 수량 30으로 2배 변경
        String body = """
                {
                  "newOutput": 30
                }
                """;

        MockHttpServletRequestBuilder request = patch(String.format("/api/plans/%s/recipes/%s/output", planId, recipeId))
                .content(body);

        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"));

        // 변동 확인
        checkOutputChange();
    }

    @Test
    @DisplayName("플랜 레시피 수량 퍼센트 변경, 재료량 변경")
    public void modifyOutputPercent() throws Exception {
        createTestPlan();

        // 레시피 수량 200% 로 변경
        String body = """
                {
                  "newPercent": 200
                }
                """;

        MockHttpServletRequestBuilder request = patch(String.format("/api/plans/%s/recipes/%s/output/percent", planId, recipeId))
                .content(body);

        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"));

        // 변동 확인
        checkOutputChange();
    }

    private void checkOutputChange() {
        Recipe customizedRecipe = planRepository.findById(planId).orElseThrow().getRecipes().stream()
                .filter(planRecipe -> planRecipe.getOriginalRecipe().getId().equals(recipeId))
                .findFirst().orElseThrow().getCustomizedRecipe();

        // 수량 2배, 30으로 변경 확인
        assertEquals(30, customizedRecipe.getOutputQuantity());

        RecipePart part = customizedRecipe.getParts().stream().findFirst().orElseThrow();
        RecipePartIngredient partIngredient = part.getIngredients().stream().filter(recipePartIngredient -> recipePartIngredient.getIngredient().getName().equals("박력분")).findFirst().orElseThrow();

        // 재료량 2배, 1000으로 변경 확인
        assertEquals(new BigDecimal(1000), partIngredient.getQuantity().setScale(0, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("플랜 레시피 재료량 변경")
    public void modifyIngredient() throws Exception {
        createTestPlan();

        // 박력분만 2배, 1000g 으로 변경
        String body = """
                [
                    {
                        "partName": "기본",
                        "ingredients": [
                            {
                                "ingredientName": "박력분",
                                "quantity": 1000.00,
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
                ]
                """;

        MockHttpServletRequestBuilder request = patch(String.format("/api/plans/%s/recipes/%s/ingredients", planId, recipeId))
                .content(body);

        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"));

        // 변동 확인
        checkIngredientChange(false);
    }

    @Test
    @DisplayName("플랜 레시피 파트 퍼센트 변경")
    public void modifyIngredientPercent() throws Exception {
        createTestPlan();

        // 기본 파트 전체 2배
        String body = """
                [
                        {
                            "partName" : "기본",
                            "percent" : 200
                        }
                ]
                """;

        MockHttpServletRequestBuilder request = patch(String.format("/api/plans/%s/recipes/%s/ingredients/percent", planId, recipeId))
                .content(body);

        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"));

        // 변동 확인
        checkIngredientChange(true);
    }

    private void checkIngredientChange(boolean partPercent) {
        Recipe customizedRecipe = planRepository.findById(planId).orElseThrow().getRecipes().stream()
                .filter(planRecipe -> planRecipe.getOriginalRecipe().getId().equals(recipeId))
                .findFirst().orElseThrow().getCustomizedRecipe();

        // 수량 변동 없음 확인
        assertEquals(15, customizedRecipe.getOutputQuantity());

        RecipePart part = customizedRecipe.getParts().stream().findFirst().orElseThrow();
        RecipePartIngredient partIngredient1 = part.getIngredients().stream()
                .filter(recipePartIngredient -> recipePartIngredient.getIngredient().getName().equals("박력분"))
                .findFirst().orElseThrow();
        RecipePartIngredient partIngredient2 = part.getIngredients().stream()
                .filter(recipePartIngredient -> recipePartIngredient.getIngredient().getName().equals("코코아파우더"))
                .findFirst().orElseThrow();

        // 박력분 2배, 1000으로 변경 확인
        assertEquals(new BigDecimal(1000), partIngredient1.getQuantity().setScale(0, RoundingMode.HALF_UP));
        // 코코아파우더 변동 없음 or 2배, 80으로 변경 확인
        assertEquals(new BigDecimal(partPercent ? 80 : 40), partIngredient2.getQuantity().setScale(0, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("플랜 레시피 수정 후 초기화")
    public void resetRecipe() throws Exception {
        // 플랜 생성 및 수정
        modifyIngredientPercent();

        MockHttpServletRequestBuilder request = patch(String.format("/api/plans/%s/recipes/%s/reset", planId, recipeId));

        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"));

        Recipe customizedRecipe = planRepository.findById(planId).orElseThrow().getRecipes().stream()
                .filter(planRecipe -> planRecipe.getOriginalRecipe().getId().equals(recipeId))
                .findFirst().orElseThrow().getCustomizedRecipe();

        // 초기화 확인
        assertEquals(RecipeType.PLACEHOLDER, customizedRecipe.getRecipeType());
    }

    @Test
    @DisplayName("플랜 레시피 설명 변경")
    public void changeDescription() throws Exception {
        // 플랜 생성
        createTestPlan();

        String body = """
                {
                    "newDescription" : "설명 변경"
                }
                """;

        MockHttpServletRequestBuilder request = patch(String.format("/api/plans/%s/recipes/%s/description", planId, recipeId))
                .content(body);

        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"));

        Recipe customizedRecipe = planRepository.findById(planId).orElseThrow().getRecipes().stream()
                .filter(planRecipe -> planRecipe.getOriginalRecipe().getId().equals(recipeId))
                .findFirst().orElseThrow().getCustomizedRecipe();

        // 변경 확인
        assertEquals("설명 변경", customizedRecipe.getDescription());
    }

    @Test
    @DisplayName("플랜 삭제")
    public void deletePlan() throws Exception {
        createTestPlan();

        MockHttpServletRequestBuilder request = delete(String.format("/api/plans/%s", planId));

        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data").value("오늘의 유루빵이 삭제되었습니다."));

        // 삭제 확인
        MockHttpServletRequestBuilder request2 = get("/api/plans/" + planId);

        testAuthHelper.requestWithAuthNoStatus(request2)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("플랜 완성, 소비 기록, 재고 차감")
    public void completePlan() throws Exception {
        // 테스트용 플랜 및 필요한 재료 준비
        createTestPlan();
        createIngredient();

        // 플랜 완성 처리
        String body = """
                {
                    "recipes": []
                }
                """; // 수정되는 레시피가 없어서 빈 배열

        MockHttpServletRequestBuilder request = post(String.format("/api/plans/%s", planId))
                .content(body);

        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data").value("오늘의 유루빵이 완성처리 되었습니다. 수고하셨습니다."));

        // 완성 처리 확인
        checkCompleteLogs();
        checkCompleteStocks();
    }

    private String createPurchaseJson() {
        return """
        {
          "description": "테스트 구매",
          "actualAt": "2025-06-06",
          "requestList": [
            {
              "name": "박력분",
              "unit": "G",
              "totalPrice": "1000",
              "totalQuantity": "1000"
            },
            {
              "name": "코코아파우더",
              "unit": "G",
              "totalPrice": "1000",
              "totalQuantity": "1000"
            },
            {
              "name": "설탕",
              "unit": "G",
              "totalPrice": "1000",
              "totalQuantity": "1000"
            },
            {
              "name": "버터",
              "unit": "G",
              "totalPrice": "1000",
              "totalQuantity": "1000"
            },
            {
              "name": "달걀",
              "unit": "개",
              "totalPrice": "1000",
              "totalQuantity": "1000"
            }
          ]
        }
        """;
    }

    private void createIngredient() throws Exception {
        MockHttpServletRequestBuilder request = post("/api/ingredientLogs/purchase")
                .content(createPurchaseJson());

        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data").value("구매가 성공적으로 등록 되었습니다."));
    }

    private void checkCompleteLogs() {
        List<IngredientLog> logs = logRepository.findByDescription("테스트 초코머핀 레시피 제작");
        assertEquals(5, logs.size()); // 5개의 소비 로그 작성 확인
    }

    private void checkCompleteStocks() {
        Ingredient ingredient = ingredientRepository.findByName("박력분").orElseThrow();
        // 박력분 재고 1000g 에서 500g 으로 감소 확인
        assertEquals(new BigDecimal(500), ingredient.getTotalStock().setScale(0, RoundingMode.HALF_UP));
    }
}
