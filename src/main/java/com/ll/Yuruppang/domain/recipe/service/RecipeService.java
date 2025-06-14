package com.ll.Yuruppang.domain.recipe.service;

import com.ll.Yuruppang.domain.inventory.entity.Ingredient;
import com.ll.Yuruppang.domain.inventory.service.IngredientService;
import com.ll.Yuruppang.domain.recipe.dto.*;
import com.ll.Yuruppang.domain.recipe.entity.*;
import com.ll.Yuruppang.domain.recipe.repository.RecipePartRepository;
import com.ll.Yuruppang.domain.recipe.repository.RecipeRepository;
import com.ll.Yuruppang.global.exceptions.ErrorCode;
import com.ll.Yuruppang.global.exceptions.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final RecipePartRepository recipePartRepository;
    private final IngredientService ingredientService;
    private final CategoryService categoryService;

    public Recipe findById(Long recipeId) {
        return recipeRepository.findById(recipeId)
                .orElseThrow(ErrorCode.RECIPE_NOT_FOUND::throwServiceException);
    }

    public Recipe findByName(String name) {
        return recipeRepository.findByName(name)
                .orElseThrow(ErrorCode.RECIPE_NOT_FOUND::throwServiceException);
    }

    public Recipe getPlaceholderRecipe() {
        return recipeRepository.findByRecipeType(RecipeType.PLACEHOLDER)
                .orElseThrow(ErrorCode.PLACEHOLDER_NOT_FOUND::throwServiceException);
    }

    private void validateIngredientsDuplication(List<RecipeIngredientDto> ingredients) {
        Set<String> nameSet = new HashSet<>();
        for (RecipeIngredientDto dto : ingredients) {
            String name = dto.ingredientName().trim();
            if (!nameSet.add(name)) {
                throw new IllegalArgumentException("중복된 재료 이름: " + name);
            }
        }
    }

    @Transactional
    public RecipeCreateResponse createRecipe(
            String name, String description,int outputQuantity, List<RecipePartDto> parts, Long categoryId
    ) {
        Recipe recipe = Recipe.builder()
                .name(name)
                .description(description)
                .outputQuantity(outputQuantity)
                .build();
        categoryService.connectRecipe(recipe, categoryId);
        recipeRepository.save(recipe);

        for(RecipePartDto dto : parts) {
            List<RecipeIngredientDto> ingredients = dto.ingredients();
            validateIngredientsDuplication(ingredients);

            RecipePart part = RecipePart.builder()
                    .recipe(recipe)
                    .name(dto.partName())
                    .build();
            recipePartRepository.save(part);

            for(RecipeIngredientDto ingredientDto : ingredients) {
                Ingredient ingredient;
                try {
                    ingredient = ingredientService.findIngredientByName(ingredientDto.ingredientName());
                } catch (ServiceException exception) {
                    ingredient = ingredientService.createIngredient(ingredientDto.ingredientName(), ingredientDto.unit(),
                            BigDecimal.ZERO, BigDecimal.ZERO);
                }

                RecipePartIngredient partIngredient = RecipePartIngredient.builder()
                        .recipePart(part)
                        .ingredient(ingredient)
                        .quantity(ingredientDto.quantity())
                        .build();

                part.addIngredient(partIngredient);
                ingredient.addPartIngredient(partIngredient);
            }

            recipe.addPart(part);
        }

        return new RecipeCreateResponse(recipe.getId());
    }


    @Transactional
    public RecipeGetResponse getRecipe(Long recipeId) {
        Recipe recipe = findById(recipeId);

        BigDecimal totalPrice = BigDecimal.ZERO;
        List<RecipePartGetDto> parts = new ArrayList<>();
        // 파트별
        for(RecipePart part : recipe.getParts()) {

            List<RecipeIngredientGetDto> ingredients = new ArrayList<>();
            for(RecipePartIngredient partIngredient : part.getIngredients()) {
                Ingredient ingredient = partIngredient.getIngredient();
                ingredients.add(new RecipeIngredientGetDto(
                        ingredient.getId(), ingredient.getName(), partIngredient.getQuantity(), ingredient.getUnit(),
                        ingredient.getTotalStock()
                ));

                // 원가 계산
                BigDecimal quantity = partIngredient.getQuantity();
                BigDecimal unitPrice = partIngredient.getIngredient().getUnitPrice();
                totalPrice = totalPrice.add(quantity.multiply(unitPrice));
            }

            // 재료 id 순 (등록된 순으로 정렬)
            ingredients.sort(Comparator.comparing(RecipeIngredientGetDto::ingredientId));

            parts.add(new RecipePartGetDto(part.getId(), part.getName(), ingredients));
        }

        // 파트 번호순 (등록된 순으로 정렬)
        parts.sort(Comparator.comparing(RecipePartGetDto::partId));

        return new RecipeGetResponse(
                recipe.getName(), recipe.getDescription(), recipe.getOutputQuantity(), totalPrice,
                parts, recipe.getCategory().getName(), recipe.getCategory().getId()
        );
    }

    @Transactional(readOnly = true)
    public Page<RecipeNameDto> getRecipes(Pageable pageable, Long categoryId, boolean favorite, String keyword) {
        String processedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Page<Recipe> recipes;
        if(processedKeyword == null) {
            if(categoryId == null) {
                if(favorite) {
                    // 카테고리 없이 즐겨찾기
                    recipes = recipeRepository.findAllByRecipeTypeAndFavorite(RecipeType.NORMAL, true, pageable);
                } else {
                    // 카테고리 없이 전체
                    recipes = recipeRepository.findAllByRecipeType(RecipeType.NORMAL, pageable);
                }
            } else {
                RecipeCategory category = categoryService.findById(categoryId);
                if(favorite) {
                    // 카테고리 포함 즐겨찾기
                    recipes = recipeRepository.findAllByRecipeTypeAndCategoryAndFavorite(RecipeType.NORMAL, category, true, pageable);
                } else {
                    // 카테고리 포함 전체
                    recipes = recipeRepository.findAllByRecipeTypeAndCategory(RecipeType.NORMAL, category, pageable);
                }
            }
        } else {
            if(categoryId == null) {
                if(favorite) {
                    // 카테고리 없이 즐겨찾기 + 키워드
                    recipes = recipeRepository.findAllByRecipeTypeAndFavoriteAndNameContainingIgnoreCase(RecipeType.NORMAL, true, processedKeyword, pageable);
                } else {
                    // 카테고리 없이 전체 + 키워드
                    recipes = recipeRepository.findAllByRecipeTypeAndNameContainingIgnoreCase(RecipeType.NORMAL, processedKeyword, pageable);
                }
            } else {
                RecipeCategory category = categoryService.findById(categoryId);
                if(favorite) {
                    // 카테고리 포함 즐겨찾기 + 키워드
                    recipes = recipeRepository.findAllByRecipeTypeAndCategoryAndFavoriteAndNameContainingIgnoreCase(RecipeType.NORMAL, category, true, processedKeyword, pageable);
                } else {
                    // 카테고리 포함 전체 + 키워드
                    recipes = recipeRepository.findAllByRecipeTypeAndCategoryAndNameContainingIgnoreCase(RecipeType.NORMAL, category, processedKeyword, pageable);
                }
            }
        }

        return recipes.map(recipe ->
                new RecipeNameDto(recipe.getId(), recipe.getName(), recipe.getOutputQuantity(), recipe.isFavorite())
        );
    }

    @Transactional
    public void modifyRecipe(
            Long recipeId, String newName, String newDescription,
            int newOutputQuantity, List<RecipePartDto> newParts, Long newCategoryId
    ) {
        Recipe recipe = findById(recipeId);
        recipe.update(newName, newDescription, newOutputQuantity);
        categoryService.connectRecipe(recipe, newCategoryId);

        Map<String, RecipePart> existingParts = recipe.getParts().stream()
                .collect(Collectors.toMap(RecipePart::getName, Function.identity()));

        Set<RecipePart> updatedParts = new HashSet<>();

        for (RecipePartDto newPartDto : newParts) {
            RecipePart part = existingParts.getOrDefault(newPartDto.partName(),
                    RecipePart.builder().recipe(recipe).name(newPartDto.partName()).build());

            validateIngredientsDuplication(newPartDto.ingredients());

            Map<String, RecipePartIngredient> existingIngredients = part.getIngredients().stream()
                    .collect(Collectors.toMap(ri -> ri.getIngredient().getName(), Function.identity()));

            part.getIngredients().clear();

            for (RecipeIngredientDto newIngredientDto : newPartDto.ingredients()) {
                Ingredient ingredient = ingredientService.findOrCreate(
                        newIngredientDto.ingredientName(), newIngredientDto.unit()
                );

                RecipePartIngredient partIngredient = existingIngredients.getOrDefault(ingredient.getName(),
                        RecipePartIngredient.builder().recipePart(part).ingredient(ingredient).build());

                partIngredient.setQuantity(newIngredientDto.quantity());

                part.addIngredient(partIngredient);
            }
            updatedParts.add(part);
        }
        recipe.getParts().clear();
        recipe.getParts().addAll(updatedParts);
    }

    @Transactional
    public void deleteRecipe(Long recipeId) {
        Recipe recipe = findById(recipeId);
        recipeRepository.delete(recipe);
    }

    @Transactional
    public void favoriteRecipe(Long recipeId) {
        Recipe recipe = findById(recipeId);
        recipe.changeFavorite();
    }
}
