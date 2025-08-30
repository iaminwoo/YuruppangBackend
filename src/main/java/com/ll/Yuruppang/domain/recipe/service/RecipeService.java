package com.ll.Yuruppang.domain.recipe.service;

import com.ll.Yuruppang.domain.inventory.entity.Ingredient;
import com.ll.Yuruppang.domain.inventory.service.IngredientService;
import com.ll.Yuruppang.domain.recipe.dto.*;
import com.ll.Yuruppang.domain.recipe.dto.autoRegister.RecipeAutoRegisterResponse;
import com.ll.Yuruppang.domain.recipe.dto.category.CategoryResponse;
import com.ll.Yuruppang.domain.recipe.entity.*;
import com.ll.Yuruppang.domain.recipe.repository.RecipePartRepository;
import com.ll.Yuruppang.domain.recipe.repository.RecipeRepository;
import com.ll.Yuruppang.global.exceptions.ErrorCode;
import com.ll.Yuruppang.global.exceptions.ServiceException;
import com.ll.Yuruppang.global.openFeign.AiResponse;
import com.ll.Yuruppang.global.openFeign.GenAIClient;
import com.ll.Yuruppang.global.openFeign.ParseAiJson;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final RecipePartRepository recipePartRepository;
    private final IngredientService ingredientService;
    private final CategoryService categoryService;
    private final ParseAiJson parseAiJson;

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
//            validateIngredientsDuplication(ingredients);

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
                        ingredient.getId(), partIngredient.getId(), ingredient.getName(), partIngredient.getQuantity(),
                        ingredient.getUnit(), ingredient.getTotalStock()
                ));

                // 원가 계산
                BigDecimal quantity = partIngredient.getQuantity();
                BigDecimal unitPrice = partIngredient.getIngredient().getUnitPrice();
                BigDecimal density = partIngredient.getIngredient().getDensity();
                totalPrice = totalPrice.add(quantity.multiply(unitPrice.divide(density, 2, RoundingMode.HALF_UP)));
            }

            // 재료 id 순 (등록된 순으로 정렬)
            ingredients.sort(Comparator.comparing(RecipeIngredientGetDto::ingredientPartId));

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
            Long recipeId,
            String newName,
            String newDescription,
            int newOutputQuantity,
            List<RecipePartDto> newParts,
            Long newCategoryId
    ) {
        // 레시피 기본 정보 업데이트
        Recipe recipe = findById(recipeId);
        recipe.update(newName, newDescription, newOutputQuantity);
        categoryService.connectRecipe(recipe, newCategoryId);

        List<RecipePart> updatedParts = new ArrayList<>();

        for (RecipePartDto newPartDto : newParts) {
            // 기존 파트 가져오거나 새로 생성
            RecipePart part = recipe.getParts().stream()
                    .filter(p -> p.getName().equals(newPartDto.partName()))
                    .findFirst()
                    .orElse(RecipePart.builder()
                            .recipe(recipe)
                            .name(newPartDto.partName())
                            .build()
                    );

            // 기존 재료 모두 제거 (orphanRemoval 설정 필요)
            part.getIngredients().clear();

            // DTO 순서대로 RecipePartIngredient 생성
            for (RecipeIngredientDto newIngredientDto : newPartDto.ingredients()) {
                Ingredient ingredient = ingredientService.findOrCreate(
                        newIngredientDto.ingredientName(),
                        newIngredientDto.unit()
                );

                RecipePartIngredient partIngredient = RecipePartIngredient.builder()
                        .recipePart(part)
                        .ingredient(ingredient)  // 기존 Ingredient 재사용
                        .quantity(newIngredientDto.quantity())
                        .build();

                part.addIngredient(partIngredient);
            }

            updatedParts.add(part);
        }

        // 레시피에 파트 순서 그대로 반영
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

    @Autowired
    private GenAIClient genAIClient;

    @Value("${ai.apiKey}")
    private String apiKey;

    @Transactional
    public RecipeAutoRegisterResponse autoRegister(String text) {
        List<CategoryResponse> allCategories = categoryService.getAllCategories();

        StringBuilder categories = new StringBuilder();
        for(CategoryResponse categoryResponse : allCategories) {
            String name = categoryResponse.categoryName();
            Long id = categoryResponse.categoryId();
            categories.append(id).append(":").append(name).append("/");
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of(
                                                "text",
                                                "아래 레시피를 읽고, 필요한 재료를 JSON으로 변환해줘. " +
                                                        "* 카테고리 목록 : " + categories + "\n" +
                                                        "JSON 구조는 반드시 다음과 같아야 해:\n" +
                                                        "{\n" +
                                                        "  \"name\": \"레시피 이름 (문자열)\",\n" +
                                                        "  \"description\": \"레시피 설명 (문자열, 선택 가능)\",\n" +
                                                        "  \"outputQuantity\": 정수,\n" +
                                                        "  \"categoryId\": 정수 (카테고리 목록에서 선택, 비슷한게 없으면 0으로),\n" +
                                                        "  \"parts\": [\n" +
                                                        "    {\n" +
                                                        "      \"partName\": \"파트 이름\",\n" +
                                                        "      \"ingredients\": [\n" +
                                                        "        { \"ingredientName\": \"재료 이름\", \"quantity\": 정수, \"unit\": \"g|ml|ea\" }\n" +
                                                        "      ]\n" +
                                                        "    }\n" +
                                                        "  ]\n" +
                                                        "}\n" +
                                                        "quantity가 2-3처럼 범위일 경우 평균값을 정수로 사용하고, unit은 g, ml, 개 중 하나로 맞춰줘.\n" +
                                                        "레시피: " + text
                                        )
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                )
        );

        AiResponse aiResponse = genAIClient.generateRecipe(apiKey, requestBody);

        // 첫 번째 후보의 첫 번째 파트 텍스트(JSON) 가져오기
        String jsonString = null;
        if (aiResponse.getCandidates() != null && !aiResponse.getCandidates().isEmpty()) {
            AiResponse.Candidate candidate = aiResponse.getCandidates().getFirst();
            if (candidate.getContent() != null && candidate.getContent().getParts() != null && !candidate.getContent().getParts().isEmpty()) {
                jsonString = candidate.getContent().getParts().getFirst().getText();
            }
        }

        if (jsonString == null) {
            throw new RuntimeException("AI 응답에서 텍스트를 가져오지 못했습니다.");
        }

        return parseAiJson.parse(jsonString);
    }
}
