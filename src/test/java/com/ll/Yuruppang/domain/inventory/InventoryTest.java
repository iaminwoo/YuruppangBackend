package com.ll.Yuruppang.domain.inventory;

import com.ll.Yuruppang.domain.inventory.entity.Ingredient;
import com.ll.Yuruppang.domain.inventory.entity.IngredientLog;
import com.ll.Yuruppang.domain.inventory.repository.IngredientRepository;
import com.ll.Yuruppang.domain.inventory.repository.LogRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class InventoryTest {

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private LogRepository logRepository;

    @BeforeEach
    public void createTestUser() throws Exception {
        testAuthHelper.createTestUser();
    }

    private String createPurchaseJson() {
        return """
        {
          "description": "테스트 구매",
          "actualAt": "2025-06-06",
          "requestList": [
            {
              "name": "밀가루",
              "unit": "G",
              "totalPrice": "1000",
              "totalQuantity": "1000"
            },
            {
              "name": "설탕",
              "unit": "G",
              "totalPrice": "1500",
              "totalQuantity": "500"
            }
          ]
        }
        """;
    }

    private String createUseJson() {
        return """
        {
          "description": "테스트 소비",
          "actualAt": "2025-06-06",
          "requestList": [
            {
              "name": "밀가루",
              "totalQuantity": "500"
            }
          ]
        }
        """;
    }

    private void addIngredient() throws Exception {
        MockHttpServletRequestBuilder request = post("/api/ingredientLogs/purchase")
                .content(createPurchaseJson());

        testAuthHelper.requestWithAuth(request);
    }

    private void assertStockEquals(String name, int expectedQuantity) {
        Ingredient ingredient = ingredientRepository.findByName(name)
                .orElseThrow(() -> new AssertionError(name + " 재고가 존재하지 않습니다."));
        assertEquals(BigDecimal.valueOf(expectedQuantity), ingredient.getTotalStock(), name + " 수량이 맞지 않습니다.");
    }

    @Test
    @DisplayName("구매 기록 등록, 재고 증가")
    public void purchaseTest() throws Exception {
        MockHttpServletRequestBuilder request = post("/api/ingredientLogs/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPurchaseJson());

        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data").value("구매가 성공적으로 등록 되었습니다."));

        assertStockEquals("밀가루", 1000);
        assertStockEquals("설탕", 500);
    }

    @Test
    @DisplayName("소비 기록 등록, 재고 감소")
    public void useTest() throws Exception {
        addIngredient();

        MockHttpServletRequestBuilder request = post("/api/ingredientLogs/use")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUseJson());

        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data").value("소비가 성공적으로 등록 되었습니다."));

        assertStockEquals("밀가루", 500); // 1000 - 500
        assertStockEquals("설탕", 500);
    }

    @Test
    @DisplayName("소비 기록 구매로 수정, 재고 증가")
    public void modifyTest() throws Exception {
        addIngredient();

        MockHttpServletRequestBuilder request = post("/api/ingredientLogs/use")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUseJson());

        // 소비 기록
        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        // 기록 id 찾기
        IngredientLog log = logRepository.findByDescription("테스트 소비").getFirst();
        Long logId = log.getId();

        // 구매로 수정, 나머지 유지
        String modifyBody = """
                {
                  "type": "PURCHASE",
                  "description": "수량변경",
                  "ingredientName": "밀가루",
                  "quantity": 500,
                  "price": 1000,
                  "actualAt": "2025-06-06"
                }
                """;

        MockHttpServletRequestBuilder request2 = put("/api/ingredientLogs/" + logId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(modifyBody);

        testAuthHelper.requestWithAuth(request2)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        assertStockEquals("밀가루", 1500); // 1000 + 500
        assertStockEquals("설탕", 500);
    }

    @Test
    @DisplayName("소비 기록 구매로 수정, 재고 증가")
    public void deleteTest() throws Exception {
        addIngredient(); // 밀가루, 설탕 기록 2개 생김

        // 밀가루 기록 id 찾기
        IngredientLog log = logRepository.findByDescription("테스트 구매").stream()
                .filter(ingredientLog -> ingredientLog.getIngredient().getName().equals("밀가루"))
                .findFirst().orElseThrow();
        Long logId = log.getId();

        MockHttpServletRequestBuilder request = delete("/api/ingredientLogs/" + logId);

        // 밀가루 기록만 삭제
        testAuthHelper.requestWithAuth(request)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        assertStockEquals("밀가루", 0);
        assertStockEquals("설탕", 500);
    }
}
