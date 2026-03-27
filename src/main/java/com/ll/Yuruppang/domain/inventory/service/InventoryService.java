package com.ll.Yuruppang.domain.inventory.service;

import com.ll.Yuruppang.domain.inventory.entity.Ingredient;
import com.ll.Yuruppang.domain.inventory.entity.IngredientLog;
import com.ll.Yuruppang.domain.inventory.entity.LogType;
import com.ll.Yuruppang.domain.inventory.entity.dto.request.IngredientAddRequest;
import com.ll.Yuruppang.domain.inventory.entity.dto.request.IngredientUseRequest;
import com.ll.Yuruppang.domain.inventory.entity.dto.response.IngredientResponse;
import com.ll.Yuruppang.domain.inventory.entity.dto.response.LogGetResponse;
import com.ll.Yuruppang.domain.inventory.repository.IngredientRepository;
import com.ll.Yuruppang.domain.recipe.service.RecipeService;
import com.ll.Yuruppang.global.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final IngredientService ingredientService;
    private final IngredientQueryService ingredientQueryService;
    private final LogService logService;
    private final RecipeService recipeService;

    private final IngredientRepository ingredientRepository;

    public static final BigDecimal EGG_TOTAL_WEIGHT = BigDecimal.valueOf(54);
    public static final BigDecimal EGG_WHITE_WEIGHT = BigDecimal.valueOf(36);
    public static final BigDecimal EGG_YOLK_WEIGHT = BigDecimal.valueOf(18);

    // 재료 구매
    @Transactional
    public void purchaseIngredient(String description, List<IngredientAddRequest> requestList, LocalDate actualAt) {
        for (IngredientAddRequest request : requestList) {
            final BigDecimal totalPrice = new BigDecimal(request.totalPrice());
            final BigDecimal totalQuantity = new BigDecimal(request.totalQuantity());

            final Ingredient ingredient = ingredientService.addIngredient(request.name(), request.unit(), totalPrice, totalQuantity);
            logService.createLog(LogType.PURCHASE, ingredient, description, actualAt, totalPrice, totalQuantity);
        }
    }

    // 재료 소비 (소비, 플랜 완료)
    @Transactional
    public void useIngredient(String description, List<IngredientUseRequest> requestList, LocalDate actualAt) {
        for (IngredientUseRequest request : requestList) {
            final BigDecimal totalQuantity = new BigDecimal(request.totalQuantity());

            Ingredient ingredient = ingredientService.useIngredient(request.name(), totalQuantity);
            logService.createLog(LogType.CONSUMPTION, ingredient, description, actualAt, BigDecimal.ZERO, totalQuantity);
        }
    }

    // 재료 밀도 변경
    @Transactional
    public IngredientResponse recalculateQuantity(Long ingredientId, BigDecimal unitVolume, BigDecimal unitWeight) {
        return ingredientService.updateDensityAndQuantity(ingredientId, unitVolume, unitWeight);
    }

    // 미사용 재료 삭제
    @Transactional
    public void cleanupUnusedIngredient() {
        List<Ingredient> toDelete = ingredientQueryService.findAll().stream().filter(this::isNotUsed).toList();
        ingredientRepository.deleteAllInBatch(toDelete);
    }

    private boolean isNotUsed(Ingredient ingredient) {
        boolean isUsedInLog = logService.existsByIngredient(ingredient);
        boolean isUsedInRecipe = recipeService.existsByIngredient(ingredient);
        return !isUsedInLog && !isUsedInRecipe;
    }

    // 달걀 깨기
    @Transactional
    public void breakEggs(BigDecimal quantity) {
        Ingredient egg = ingredientQueryService.findByName("달걀");
        Ingredient whites = ingredientQueryService.findByName("흰자");
        Ingredient yolks = ingredientQueryService.findByName("노른자");

        if (egg.getTotalStock().compareTo(quantity) < 0) throw ErrorCode.STOCK_NOT_ENOUGH.throwServiceException();

        egg.addTotalQuantity(quantity.negate());

        BigDecimal eggUnitPricePerG = egg.getUnitPrice().divide(EGG_TOTAL_WEIGHT, 2, RoundingMode.HALF_UP);

        whites.addTotalQuantity(quantity.multiply(EGG_WHITE_WEIGHT));
        whites.updateUnitPrice(eggUnitPricePerG);

        yolks.addTotalQuantity(quantity.multiply(EGG_YOLK_WEIGHT));
        yolks.updateUnitPrice(eggUnitPricePerG);
    }

    // 로그 수정
    @Transactional
    public LogGetResponse modifyLog(Long logId, LogType newType, String description,
                                    String ingredientName, BigDecimal newQuantity,
                                    BigDecimal newPrice, LocalDate actualAt) {
        IngredientLog log = logService.findById(logId);
        Ingredient newIngredient = ingredientQueryService.findByName(ingredientName);

        ingredientService.applyLogEffect(log.getIngredient(), log.getType(), log.getQuantity(), log.getTotalPrice(), true);
        ingredientService.applyLogEffect(newIngredient, newType, newQuantity, newPrice, false);

        return logService.modifyLog(log, newType, description, newIngredient, newQuantity, newPrice, actualAt);
    }

    // 로그 삭제
    @Transactional
    public void deleteLog(Long logId) {
        IngredientLog log = logService.findById(logId);

        ingredientService.applyLogEffect(log.getIngredient(), log.getType(), log.getQuantity(), log.getTotalPrice(), true);

        logService.delete(log);
    }
}
