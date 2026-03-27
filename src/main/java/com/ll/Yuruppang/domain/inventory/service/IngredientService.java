package com.ll.Yuruppang.domain.inventory.service;

import com.ll.Yuruppang.domain.inventory.entity.Ingredient;
import com.ll.Yuruppang.domain.inventory.entity.IngredientUnit;
import com.ll.Yuruppang.domain.inventory.entity.LogType;
import com.ll.Yuruppang.domain.inventory.entity.dto.IngredientDto;
import com.ll.Yuruppang.domain.inventory.entity.dto.response.IngredientResponse;
import com.ll.Yuruppang.domain.inventory.entity.dto.response.StockResponse;
import com.ll.Yuruppang.domain.inventory.repository.IngredientRepository;
import com.ll.Yuruppang.domain.plan.dto.detailResponse.IngredientLackDto;
import com.ll.Yuruppang.global.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IngredientService {
    private final IngredientRepository ingredientRepository;
    private final IngredientQueryService ingredientQueryService;

    public Ingredient findOrCreate(String name, IngredientUnit unit) {
        return ingredientQueryService.findByNameOptional(name)
                .orElseGet(() -> createIngredient(name, unit, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    public Ingredient createIngredient(String name, IngredientUnit unit, BigDecimal unitPrice, BigDecimal totalQuantity) {
        final Ingredient ingredient = Ingredient.builder()
                .name(name)
                .unit(unit)
                .unitPrice(unitPrice)
                .totalStock(totalQuantity)
                .build();
        return ingredientRepository.save(ingredient);
    }

    public Ingredient addIngredient(String name, IngredientUnit unit, BigDecimal totalPrice, BigDecimal totalQuantity) {
        return ingredientQueryService.findByNameOptional(name)
                .map(ingredient -> {
                    ingredient.updateQuantityAndPrice(totalPrice, totalQuantity);
                    return ingredient;
                })
                .orElseGet(() -> createIngredient(name, unit, totalPrice.divide(totalQuantity, 2, RoundingMode.HALF_UP), totalQuantity));
    }

    public Ingredient useIngredient(String name, BigDecimal quantity) {
        final Ingredient ingredient = ingredientQueryService.findByName(name);

        if (ingredient.getTotalStock().compareTo(quantity) < 0) {
            throw ErrorCode.STOCK_NOT_ENOUGH.throwServiceException();
        }

        ingredient.addTotalQuantity(quantity.negate());
        return ingredient;
    }

    public StockResponse getStocks(int offset, int limit) {
        return makeResponseWithPaging(
                ingredientQueryService.findAll().stream().map(this::makeIngredientDto).toList(), offset, limit
        );
    }

    public StockResponse searchStocksByKeyword(String rawKeyword, int offset, int limit) {
        List<String> keywords = Arrays.stream(rawKeyword.split(","))
                .map(String::trim)
                .map(this::normalizeKeyword)
                .distinct()
                .toList();

        List<IngredientDto> resultDtos = keywords.stream()
                .flatMap(keyword -> ingredientQueryService.searchByName(keyword).stream())
                .distinct()
                .map(this::makeIngredientDto)
                .sorted(Comparator.comparing(IngredientDto::ingredientId).reversed())
                .toList();

        return makeResponseWithPaging(resultDtos, offset, limit);
    }

    private String normalizeKeyword(String keyword) {
        return "계란".equals(keyword) ? "달걀" : keyword;
    }

    private IngredientDto makeIngredientDto(Ingredient ingredient) {
        return new IngredientDto(
                ingredient.getId(), ingredient.getName(), ingredient.getUnit().getValue(),
                String.valueOf(ingredient.getUnitPrice()), String.valueOf(ingredient.getTotalStock())
        );
    }

    private StockResponse makeResponseWithPaging(List<IngredientDto> list, int offset, int limit) {
        int total = list.size();
        int fromIndex = Math.min(offset, total);
        int toIndex = Math.min(offset + limit, total);
        List<IngredientDto> resultList = list.subList(fromIndex, toIndex);

        int totalPage = (int) Math.ceil((double) total / limit);

        return new StockResponse(resultList, totalPage);
    }

    @Transactional(readOnly = true)
    public IngredientResponse getIngredientDetail(Long ingredientId) {
        return makeResponseDto(ingredientQueryService.findById(ingredientId));
    }

    @Transactional
    public IngredientResponse changeIngredientUnit(Long ingredientId, IngredientUnit newUnit) {
        final Ingredient ingredient = ingredientQueryService.findById(ingredientId);
        ingredient.updateUnit(newUnit);
        return makeResponseDto(ingredient);
    }

    @Transactional
    public IngredientResponse updateDensityAndQuantity(Long ingredientId, BigDecimal unitVolume, BigDecimal unitWeight) {
        Ingredient ingredient = ingredientQueryService.findById(ingredientId);
        ingredient.recalculateQuantityByDensity(unitVolume, unitWeight);
        return makeResponseDto(ingredient);
    }

    private IngredientResponse makeResponseDto(Ingredient ingredient) {
        return new IngredientResponse(
                ingredient.getId(), ingredient.getName(), ingredient.getUnit(),
                ingredient.getUnitPrice(), ingredient.getTotalStock(), ingredient.getDensity()
        );
    }

    public List<IngredientLackDto> calculateLackIngredients(Map<Long, BigDecimal> totalIngredient) {
        List<IngredientLackDto> lackIngredients = new ArrayList<>();

        for (Long ingredientId : totalIngredient.keySet()) {
            Ingredient ingredient = ingredientQueryService.findById(ingredientId);
            BigDecimal customizedQuantity = totalIngredient.get(ingredientId);

            // 부족 재료 추가
            if (ingredient.getTotalStock().compareTo(customizedQuantity) < 0) {
                lackIngredients.add(new IngredientLackDto(
                        ingredientId, ingredient.getName(),
                        customizedQuantity, ingredient.getTotalStock(),
                        customizedQuantity.subtract(ingredient.getTotalStock())
                ));
            }
        }

        lackIngredients.sort(Comparator.comparing(IngredientLackDto::name));

        return lackIngredients;
    }

    @Transactional
    public void applyLogEffect(Ingredient ingredient, LogType type, BigDecimal quantity, BigDecimal price, boolean isRollback) {
        BigDecimal effectiveQuantity = quantity;

        if (type == LogType.PURCHASE) {
            if (isRollback) {
                ingredient.subtractUnitPrice(price, quantity);
                effectiveQuantity = quantity.negate();
            } else {
                ingredient.changeUnitPrice(price, quantity);
            }
        } else {
            if (!isRollback) effectiveQuantity = quantity.negate();
        }

        ingredient.addTotalQuantity(effectiveQuantity);
    }
}
