package com.ll.Yuruppang.domain.inventory.contorller;

import com.ll.Yuruppang.domain.inventory.entity.dto.response.StockResponse;
import com.ll.Yuruppang.domain.inventory.service.IngredientService;
import com.ll.Yuruppang.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @GetMapping
    public RsData<StockResponse> getStocks(
            @RequestParam(required = false) String keyword
    ) {
        StockResponse response;
        if (keyword == null || keyword.isBlank()) {
            // 키워드 없이 호출될 때
            response = ingredientService.getStocks();
        } else {
            // 키워드가 있을 때
            response = ingredientService.searchStocksByKeyword(keyword.trim());
        }
        return RsData.success(HttpStatus.OK, response);
    }

    @DeleteMapping
    public RsData<StockResponse> cleanStocks() {
        ingredientService.cleanup();
        return RsData.success(HttpStatus.OK, ingredientService.getStocks());
    }
}
