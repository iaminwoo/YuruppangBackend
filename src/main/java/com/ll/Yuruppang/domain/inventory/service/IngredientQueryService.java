package com.ll.Yuruppang.domain.inventory.service;

import com.ll.Yuruppang.domain.inventory.entity.Ingredient;
import com.ll.Yuruppang.domain.inventory.entity.dto.response.EggResponse;
import com.ll.Yuruppang.domain.inventory.repository.IngredientRepository;
import com.ll.Yuruppang.global.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import static com.ll.Yuruppang.domain.inventory.service.InventoryService.EGG_TOTAL_WEIGHT;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IngredientQueryService {

    private final IngredientRepository ingredientRepository;

    public Ingredient findByName(String name) {
        return ingredientRepository.findByName(name).orElseThrow(ErrorCode.INGREDIENT_NOT_FOUND::throwServiceException);
    }

    public Optional<Ingredient> findByNameOptional(String name) {
        return ingredientRepository.findByName(name);
    }

    public Ingredient findById(Long ingredientId) {
        return ingredientRepository.findById(ingredientId).orElseThrow(ErrorCode.INGREDIENT_NOT_FOUND::throwServiceException);
    }

    public List<Ingredient> findAll() {
        return ingredientRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public List<Ingredient> searchByName(String keyword) {
        return ingredientRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword);
    }

    // 달걀 조회
    public EggResponse getEggs() {
        Ingredient egg = findByName("달걀");
        Ingredient whites = findByName("흰자");
        Ingredient yolks = findByName("노른자");

        BigDecimal eggCount = egg.getTotalStock().divide(EGG_TOTAL_WEIGHT, 0, RoundingMode.DOWN);

        return new EggResponse(eggCount, whites.getTotalStock(), yolks.getTotalStock());
    }
}
