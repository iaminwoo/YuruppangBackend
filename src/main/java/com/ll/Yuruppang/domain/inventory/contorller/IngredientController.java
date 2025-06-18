package com.ll.Yuruppang.domain.inventory.contorller;

import com.ll.Yuruppang.domain.inventory.entity.dto.request.EggBreakRequest;
import com.ll.Yuruppang.domain.inventory.entity.dto.request.IngredientDensityRequest;
import com.ll.Yuruppang.domain.inventory.entity.dto.request.IngredientUnitRequest;
import com.ll.Yuruppang.domain.inventory.entity.dto.response.IngredientResponse;
import com.ll.Yuruppang.domain.inventory.entity.dto.response.StockResponse;
import com.ll.Yuruppang.domain.inventory.service.IngredientService;
import com.ll.Yuruppang.global.response.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

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

    @GetMapping("/{ingredientId}")
    public RsData<IngredientResponse> getIngredientDetail(@PathVariable Long ingredientId) {
        return RsData.success(HttpStatus.OK, ingredientService.getIngredientDetail(ingredientId));
    }

    @PostMapping("/{ingredientId}/recalculate-quantity")
    public RsData<IngredientResponse> recalculateQuantity(
            @PathVariable Long ingredientId, @RequestBody @Valid IngredientDensityRequest request
    ) {
        return RsData.success(HttpStatus.OK, ingredientService.recalculateQuantity(ingredientId, request.unitVolume(), request.unitWeight()));
    }

    @PatchMapping("/{ingredientId}")
    public RsData<IngredientResponse> changeIngredientUnit(
            @PathVariable Long ingredientId, @RequestBody @Valid IngredientUnitRequest request
    ) {
        return RsData.success(HttpStatus.OK, ingredientService.changeIngredientUnit(ingredientId, request.newUnit()));
    }

    @PostMapping("/break-eggs")
    public RsData<String> breakEggs(@RequestBody @Valid EggBreakRequest request) {
        ingredientService.breakEggs(BigDecimal.valueOf(request.quantity()));
        return RsData.success(HttpStatus.OK, "달걀을 깼습니다.");
    }
}
