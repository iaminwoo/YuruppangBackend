package com.ll.Yuruppang.domain.inventory;

import com.ll.Yuruppang.domain.inventory.entity.Ingredient;
import com.ll.Yuruppang.domain.inventory.repository.IngredientRepository;
import com.ll.Yuruppang.domain.inventory.service.IngredientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class IngredientTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private IngredientService ingredientService;

    private String createPurchaseJson() {
        return """
        {
          "description": "테스트 구매",
          "actualAt": "2025-06-11",
          "requestList": [
            {
              "name": "우유",
              "unit": "ml",
              "totalPrice": "1000",
              "totalQuantity": "1000"
            }
          ]
        }
        """;
    }

    private void addIngredient() throws Exception {
        mockMvc.perform(post("/api/ingredientLogs/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPurchaseJson()));
    }

    @Test
    @DisplayName("밀도 변경에 따른 재료 수량 변경")
    public void recalculateQuantity() throws Exception {
        addIngredient();

        Ingredient ingredient = ingredientService.findIngredientByName("우유");

        String body = """
                {
                    "unitVolume": 100,
                    "unitWeight": 200
                }
                """;

        mockMvc.perform(post("/api/ingredients/" + ingredient.getId() + "/recalculate-quantity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data.density").value(2))
                .andExpect(jsonPath("$.data.totalStock").value(2000));
    }
}
