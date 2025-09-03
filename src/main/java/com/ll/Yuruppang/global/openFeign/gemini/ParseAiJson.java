package com.ll.Yuruppang.global.openFeign.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.Yuruppang.domain.inventory.entity.IngredientUnit;
import com.ll.Yuruppang.domain.inventory.service.IngredientService;
import com.ll.Yuruppang.domain.recipe.dto.autoRegister.RecipeAutoRegisterResponse;
import com.ll.Yuruppang.domain.recipe.dto.autoRegister.RecipeIngredientResponse;
import com.ll.Yuruppang.domain.recipe.dto.autoRegister.RecipePartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ParseAiJson {
    private final IngredientService ingredientService;

    public RecipeAutoRegisterResponse parse(String jsonString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // JSON을 먼저 임시 DTO로 변환
            TempRecipe temp = objectMapper.readValue(jsonString, TempRecipe.class);

            // Part, Ingredient 변환
            List<RecipePartResponse> parts = temp.parts().stream().map(p ->
                    new RecipePartResponse(
                            p.partName(),
                            p.ingredients().stream().map(i -> {
                                BigDecimal stock;
                                try {
                                    stock = ingredientService.findIngredientByName(i.ingredientName()).getTotalStock();
                                } catch (Exception e) {
                                    stock = BigDecimal.ZERO;
                                }
                                return new RecipeIngredientResponse(
                                        i.ingredientName(),
                                        BigDecimal.valueOf(i.quantity()),
                                        IngredientUnit.valueOf(i.unit().toUpperCase()),
                                        stock
                                );
                            }).toList()
                    )
            ).toList();

            return new RecipeAutoRegisterResponse(
                    temp.name(),
                    temp.description(),
                    temp.outputQuantity(),
                    temp.categoryId(),
                    parts
            );

        } catch (Exception e) {
            throw new RuntimeException("JSON 변환 실패", e);
        }
    }

    // 임시 DTO (Jackson용)
    record TempRecipe(
            String name,
            String description,
            int outputQuantity,
            Long categoryId,
            List<TempRecipePart> parts
    ) {}

    record TempRecipePart(
            String partName,
            List<TempRecipeIngredient> ingredients
    ) {}

    record TempRecipeIngredient(
            String ingredientName,
            double quantity,
            String unit
    ) {}
}
