package com.ll.Yuruppang.domain.inventory.service;

import com.ll.Yuruppang.domain.inventory.entity.Ingredient;
import com.ll.Yuruppang.domain.inventory.entity.IngredientLog;
import com.ll.Yuruppang.domain.inventory.entity.IngredientUnit;
import com.ll.Yuruppang.domain.inventory.entity.LogType;
import com.ll.Yuruppang.domain.inventory.entity.dto.IngredientDto;
import com.ll.Yuruppang.domain.inventory.entity.dto.request.IngredientAddRequest;
import com.ll.Yuruppang.domain.inventory.entity.dto.request.IngredientUseRequest;
import com.ll.Yuruppang.domain.inventory.entity.dto.response.IngredientResponse;
import com.ll.Yuruppang.domain.inventory.entity.dto.response.StockResponse;
import com.ll.Yuruppang.domain.inventory.repository.IngredientRepository;
import com.ll.Yuruppang.domain.inventory.repository.LogRepository;
import com.ll.Yuruppang.domain.recipe.repository.PartIngredientRepository;
import com.ll.Yuruppang.global.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IngredientService {
    private final IngredientRepository ingredientRepository;
    private final LogRepository logRepository;
    private final PartIngredientRepository partIngredientRepository;

    public Ingredient findIngredientByName(String name) {
        return ingredientRepository.findByName(name)
                .orElseThrow(ErrorCode.INGREDIENT_NOT_FOUND::throwServiceException);
    }

    public Ingredient findById(Long ingredientId) {
        return ingredientRepository.findById(ingredientId)
                .orElseThrow(ErrorCode.INGREDIENT_NOT_FOUND::throwServiceException);
    }

    public Ingredient findOrCreate(String name, IngredientUnit unit) {
        Optional<Ingredient> found = ingredientRepository.findByName(name);
        return found.orElseGet(() -> createIngredient(name, unit, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Transactional
    public Ingredient addIngredient(String name, IngredientUnit unit, BigDecimal totalPrice, BigDecimal totalQuantity) {
        return ingredientRepository.findByName(name)
                .map(ingredient -> addIngredient(ingredient, totalPrice, totalQuantity))
                .orElseGet(() -> createIngredient(
                        name,
                        unit,
                        totalPrice.divide(totalQuantity, 2, RoundingMode.HALF_UP),
                        totalQuantity
                ));
    }
    public Ingredient createIngredient(String name, IngredientUnit unit, BigDecimal unitPrice, BigDecimal totalQuantity) {
        Ingredient ingredient = Ingredient.builder()
                .name(name)
                .unit(unit)
                .unitPrice(unitPrice)
                .totalStock(totalQuantity)
                .build();
        return ingredientRepository.save(ingredient);
    }
    public Ingredient addIngredient(Ingredient ingredient, BigDecimal price, BigDecimal quantity) {
        ingredient.changeUnitPrice(price, quantity);
        ingredient.addTotalQuantity(quantity);
        return ingredient;
    }

    @Transactional
    public void purchaseIngredient(String description, List<IngredientAddRequest> requestList, LocalDate actualAt) {
        for(IngredientAddRequest request : requestList) {
            Ingredient ingredient = addIngredient(
                    request.name(),
                    request.unit(),
                    new BigDecimal(request.totalPrice()),
                    new BigDecimal(request.totalQuantity())
            );

            IngredientLog log = IngredientLog.builder()
                    .type(LogType.PURCHASE)
                    .description(description)
                    .ingredient(ingredient)
                    .actualAt(actualAt)
                    .totalPrice(new BigDecimal(request.totalPrice()))
                    .quantity(new BigDecimal(request.totalQuantity()))
                    .build();
            logRepository.save(log);
        }
    }

    @Transactional
    public void useIngredient(String description, List<IngredientUseRequest> requestList, LocalDate actualAt) {
        for(IngredientUseRequest request : requestList) {
            Ingredient ingredient = useIngredient(
                    request.name(),
                    new BigDecimal(request.totalQuantity())
            );

            IngredientLog log = IngredientLog.builder()
                    .type(LogType.CONSUMPTION)
                    .description(description)
                    .ingredient(ingredient)
                    .actualAt(actualAt)
                    .quantity(new BigDecimal(request.totalQuantity()))
                    .build();
            logRepository.save(log);
        }
    }

    public Ingredient useIngredient(String name, BigDecimal quantity) {
        Optional<Ingredient> optionalIngredient = ingredientRepository.findByName(name);
        if(optionalIngredient.isEmpty()) {
            throw ErrorCode.INGREDIENT_NOT_FOUND.throwServiceException();
        }
        Ingredient ingredient = optionalIngredient.get();
        if (ingredient.getTotalStock().compareTo(quantity) < 0) {
            throw ErrorCode.STOCK_NOT_ENOUGH.throwServiceException();
        }
        ingredient.addTotalQuantity(quantity.negate());
        return ingredient;
    }

    @Transactional(readOnly = true)
    public StockResponse getStocks() {
        List<Ingredient> all = ingredientRepository.findAll();
        List<IngredientDto> dtoList = new ArrayList<>();

        for (Ingredient ingredient : all) {
            dtoList.add(makeIngredientDto(ingredient));
        }

        return new StockResponse(dtoList);
    }

    @Transactional(readOnly = true)
    public StockResponse searchStocksByKeyword(String keyword) {
        if(keyword.equals("계란")) {
            keyword = "달걀";
        }

        List<IngredientDto> dtos = ingredientRepository
                .findByNameContainingIgnoreCaseOrderByNameAsc(keyword)
                .stream()
                .map(this::makeIngredientDto)
                .collect(Collectors.toList());
        return new StockResponse(dtos);
    }

    private IngredientDto makeIngredientDto(Ingredient ingredient) {
        return new IngredientDto(
                ingredient.getId(), ingredient.getName(), ingredient.getUnit().getValue(),
                String.valueOf(ingredient.getUnitPrice()), String.valueOf(ingredient.getTotalStock())
        );
    }

    @Transactional
    public void cleanup() {
        List<Ingredient> ingredients = ingredientRepository.findAll();

        for (Ingredient ingredient : ingredients) {
            boolean isUsedInLog = logRepository.existsByIngredient(ingredient);
            boolean isUsedInRecipe = partIngredientRepository.existsByIngredient(ingredient);

            if (!isUsedInLog && !isUsedInRecipe) {
                ingredientRepository.delete(ingredient);
            }
        }
    }

    @Transactional
    public IngredientResponse recalculateQuantity(Long ingredientId, BigDecimal unitVolume, BigDecimal unitWeight) {
        Ingredient ingredient = findById(ingredientId);
        // 단위가 g 이 아닌 경우만 밀도 변경 가능하도록
        if(!ingredient.getUnit().equals(IngredientUnit.G)){
            BigDecimal newDensity = unitWeight.divide(unitVolume, 4, RoundingMode.HALF_UP);

            BigDecimal volume = ingredient.getTotalStock().divide(ingredient.getDensity(), 4, RoundingMode.HALF_UP);
            ingredient.setTotalStock(volume.multiply(newDensity));
            ingredient.setDensity(newDensity);
        }
        return makeResponseDto(ingredient);
    }

    @Transactional(readOnly = true)
    public IngredientResponse getIngredientDetail(Long ingredientId) {
        Ingredient ingredient = findById(ingredientId);
        return makeResponseDto(ingredient);
    }

    @Transactional
    public IngredientResponse changeIngredientUnit(Long ingredientId, IngredientUnit newUnit) {
        Ingredient ingredient = findById(ingredientId);
        ingredient.setUnit(newUnit);
        return makeResponseDto(ingredient);
    }

    private IngredientResponse makeResponseDto(Ingredient ingredient) {
        return new IngredientResponse(
                ingredient.getId(), ingredient.getName(), ingredient.getUnit(),
                ingredient.getUnitPrice(), ingredient.getTotalStock(), ingredient.getDensity()
        );
    }

    @Transactional
    public void breakEggs(BigDecimal quantity) {
        Ingredient egg = findIngredientByName("달걀");
        BigDecimal eggWeight = BigDecimal.valueOf(54);
        BigDecimal eggUnitPricePerG = egg.getUnitPrice().divide(eggWeight, 2, RoundingMode.HALF_UP);

        if(egg.getTotalStock().compareTo(quantity) < 0) throw ErrorCode.STOCK_NOT_ENOUGH.throwServiceException();

        egg.addTotalQuantity(quantity.negate());

        Ingredient whites = findIngredientByName("흰자");
        BigDecimal whitesWeight = BigDecimal.valueOf(36);
        whites.addTotalQuantity(quantity.multiply(whitesWeight));
        whites.setUnitPrice(eggUnitPricePerG);

        Ingredient yolks = findIngredientByName("노른자");
        BigDecimal yolksWeight = BigDecimal.valueOf(18);
        yolks.addTotalQuantity(quantity.multiply(yolksWeight));
        yolks.setUnitPrice(eggUnitPricePerG);
    }
}
