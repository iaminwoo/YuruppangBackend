package com.ll.Yuruppang.domain.recipe.service;

import com.ll.Yuruppang.domain.inventory.entity.Ingredient;
import com.ll.Yuruppang.domain.inventory.service.IngredientService;
import com.ll.Yuruppang.domain.plan.dto.detailResponse.ComparedIngredientDto;
import com.ll.Yuruppang.domain.plan.dto.detailResponse.ComparedPartDto;
import com.ll.Yuruppang.domain.plan.dto.detailResponse.ComparedPlanRecipeDetailDto;
import com.ll.Yuruppang.domain.plan.entity.BakingPlanRecipe;
import com.ll.Yuruppang.domain.recipe.dto.*;
import com.ll.Yuruppang.domain.recipe.dto.autoRegister.RecipeAutoRegisterResponse;
import com.ll.Yuruppang.domain.recipe.dto.category.CategoryResponse;
import com.ll.Yuruppang.domain.recipe.entity.*;
import com.ll.Yuruppang.domain.recipe.repository.RecipePartRepository;
import com.ll.Yuruppang.domain.recipe.repository.RecipeRepository;
import com.ll.Yuruppang.global.exceptions.ErrorCode;
import com.ll.Yuruppang.global.exceptions.ServiceException;
import com.ll.Yuruppang.global.openFeign.gemini.AiResponse;
import com.ll.Yuruppang.global.openFeign.gemini.GenAIClient;
import com.ll.Yuruppang.global.openFeign.gemini.ParseAiJson;
import com.ll.Yuruppang.global.openFeign.youtube.YoutubeApiClient;
import com.ll.Yuruppang.global.openFeign.youtube.YoutubeUtils;
import com.ll.Yuruppang.global.openFeign.youtube.dto.VideoListResponse;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final RecipePartRepository recipePartRepository;

    private final IngredientService ingredientService;
    private final CategoryService categoryService;
    private final PanService panService;

    private final ParseAiJson parseAiJson;

    @Autowired
    private GenAIClient genAIClient;

    @Autowired
    private YoutubeApiClient youtubeApiClient;

    @Value("${ai.apiKey}")
    private String apiKey;

    @Value("${youtube.apiKey}")
    private String youtubeApiKey;

    public Recipe findById(Long recipeId) {
        return recipeRepository.findById(recipeId)
                .orElseThrow(ErrorCode.RECIPE_NOT_FOUND::throwServiceException);
    }

    public Recipe getPlaceholderRecipe() {
        return recipeRepository.findByRecipeType(RecipeType.PLACEHOLDER)
                .orElseThrow(ErrorCode.PLACEHOLDER_NOT_FOUND::throwServiceException);
    }

    @Transactional
    public RecipeCreateResponse createRecipe(
            String name, String description,int outputQuantity,
            List<RecipePartDto> parts, Long categoryId, Long panId
    ) {
        Recipe recipe = Recipe.builder()
                .name(name)
                .description(description)
                .outputQuantity(outputQuantity)
                .build();
        categoryService.connectRecipe(recipe, categoryId);

        if(panId == null) panId = 0L;
        Optional<Pan> panOptional = panService.findByIdOptional(panId);
        panOptional.ifPresent(recipe::setPan);

        recipeRepository.save(recipe);

        for(RecipePartDto dto : parts) {
            List<RecipeIngredientDto> ingredients = dto.ingredients();

            RecipePart part = RecipePart.builder()
                    .recipe(recipe)
                    .name(dto.partName())
                    .build();
            recipePartRepository.save(part);

            int orderIndex = 1;

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
                        .orderIndex(orderIndex++)
                        .build();

                part.addIngredient(partIngredient);
                ingredient.addPartIngredient(partIngredient);
            }

            recipe.addPart(part);
        }

        return new RecipeCreateResponse(recipe.getId());
    }

    // note : 비슷한 조회 메서드
    @Transactional
    public RecipeGetResponse getRecipe(Long recipeId) {
        Recipe recipe = findById(recipeId);

        // 원가 계산
        BigDecimal totalPrice = calculateTotalRecipePrice(recipe.getParts());

        List<RecipePartGetDto> parts = new ArrayList<>();
        // 파트별
        for (RecipePart part : recipe.getParts()) {
            List<RecipeIngredientGetDto> ingredients = new ArrayList<>();
            for (RecipePartIngredient partIngredient : part.getIngredients()) {
                Ingredient ingredient = partIngredient.getIngredient();
                ingredients.add(new RecipeIngredientGetDto(
                        ingredient.getId(), partIngredient.getId(), ingredient.getName(), partIngredient.getQuantity(),
                        partIngredient.getOrderIndex(), ingredient.getUnit(), ingredient.getTotalStock()
                ));
            }

            // 재료 순서 정렬
            ingredients.sort(
                    Comparator.comparing(RecipeIngredientGetDto::orderIndex, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(RecipeIngredientGetDto::ingredientId)
            );

            parts.add(new RecipePartGetDto(part.getId(), part.getName(), ingredients));
        }

        // 파트 번호순 (등록된 순으로 정렬)
        parts.sort(Comparator.comparing(RecipePartGetDto::partId));

        return new RecipeGetResponse(
                recipe.getName(), recipe.getDescription(), recipe.getOutputQuantity(),
                panService.makePanResponse(recipe.getPan()), totalPrice,
                parts, recipe.getCategory().getName(), recipe.getCategory().getId()
        );
    }

    // TODO : 메서드 리펙토링 중
    public List<ComparedPlanRecipeDetailDto> getComparedRecipeDetails(Set<BakingPlanRecipe> planRecipes) {
        // 레시피별로 스트림 처리
        return planRecipes.stream()
                // 메인 처리 로직
                .map(this::convertToComparedRecipeDto)
                .sorted(Comparator.comparing(ComparedPlanRecipeDetailDto::recipeName))
                .collect(Collectors.toList());
    }

    private ComparedPlanRecipeDetailDto convertToComparedRecipeDto(BakingPlanRecipe planRecipe) {
        Recipe originalRecipe = planRecipe.getOriginalRecipe();
        Recipe tempRecipe = getTempOrOriginalRecipe(planRecipe);

        BigDecimal totalPrice = calculateTotalRecipePrice(tempRecipe.getParts());

        // 파트 맵핑 (이름 기반 조회를 위해 미리 준비)
        Map<String, RecipePart> originalPartMap = originalRecipe.getParts().stream()
                .collect(Collectors.toMap(RecipePart::getName, Function.identity()));

        // 파트 비교 수행
        List<ComparedPartDto> comparedParts = tempRecipe.getParts().stream()
                .sorted(Comparator.comparing(RecipePart::getName))
                .map(tempPart -> comparePart(tempPart, originalPartMap.get(tempPart.getName())))
                .sorted(Comparator.comparing(ComparedPartDto::partId))
                .toList();

        return createComparedPlanRecipeDetailDto(planRecipe, originalRecipe, tempRecipe, totalPrice, comparedParts);
    }

    private ComparedPartDto comparePart(RecipePart tempPart, RecipePart originalPart) {
        List<ComparedIngredientDto> comparedIngredients = new ArrayList<>();
        Set<RecipePartIngredient> usedOriginalPis = new HashSet<>();

        for (RecipePartIngredient tempPi : tempPart.getIngredients()) {
            RecipePartIngredient originalPi = findAndMarkOriginalIngredient(originalPart, tempPi, usedOriginalPis);

            comparedIngredients.add(new ComparedIngredientDto(
                    tempPi.getIngredient().getId(),
                    tempPi.getId(),
                    tempPi.getIngredient().getName(),
                    tempPi.getIngredient().getUnit(),
                    originalPi != null ? originalPi.getQuantity() : BigDecimal.ZERO,
                    tempPi.getQuantity(),
                    tempPi.getOrderIndex() == null ? 0 : tempPi.getOrderIndex()
            ));
        }

        // 재료 정렬
        comparedIngredients.sort(
                Comparator.comparing(ComparedIngredientDto::orderIndex, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ComparedIngredientDto::ingredientId)
        );

        return new ComparedPartDto(tempPart.getId(), tempPart.getName(), tempPart.getPercent(), comparedIngredients);
    }

    private ComparedPlanRecipeDetailDto createComparedPlanRecipeDetailDto(
            BakingPlanRecipe planRecipe,
            Recipe originalRecipe,
            Recipe tempRecipe,
            BigDecimal totalPrice,
            List<ComparedPartDto> comparedParts) {
        int yieldPercent = new BigDecimal(tempRecipe.getOutputQuantity())
                .multiply(new BigDecimal(100))
                .divide(new BigDecimal(originalRecipe.getOutputQuantity()), 0, RoundingMode.HALF_UP)
                .intValue();

        boolean isCustomized = !planRecipe.getCustomizedRecipe().getRecipeType().equals(RecipeType.PLACEHOLDER);

        return new ComparedPlanRecipeDetailDto(
                originalRecipe.getId(),
                originalRecipe.getName(),
                originalRecipe.getDescription(),
                totalPrice,
                tempRecipe.getName(),
                tempRecipe.getDescription(),
                panService.makePanResponse(tempRecipe.getPan()),
                isCustomized,
                originalRecipe.getOutputQuantity(),
                tempRecipe.getOutputQuantity(),
                yieldPercent,
                comparedParts
        );
    }

    // note 원가 계산
    private BigDecimal calculateTotalRecipePrice(Set<RecipePart> parts) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (RecipePart part : parts) {
            for (RecipePartIngredient partIngredient : part.getIngredients()) {
                totalPrice = totalPrice.add(calculateIngredientPrice(
                        partIngredient.getQuantity(),
                        partIngredient.getIngredient().getUnitPrice(),
                        partIngredient.getIngredient().getDensity()
                ));
            }
        }
        return totalPrice;
    }

    private BigDecimal calculateIngredientPrice(BigDecimal quantity, BigDecimal price, BigDecimal density) {
        if (density == null || density.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return quantity.multiply(price).divide(density, 2, RoundingMode.HALF_UP);
    }

    // note 임시레시피 조회
    private Recipe getTempOrOriginalRecipe(BakingPlanRecipe planRecipe) {
        Recipe tempRecipe;
        if (planRecipe.getCustomizedRecipe().getRecipeType().equals(RecipeType.PLACEHOLDER)) {
            tempRecipe = planRecipe.getOriginalRecipe();
        } else {
            tempRecipe = planRecipe.getCustomizedRecipe();
        }
        return tempRecipe;
    }

    // note 원본 재료 찾기
    private RecipePartIngredient findAndMarkOriginalIngredient(
            RecipePart originalPart,
            RecipePartIngredient tempPi,
            Set<RecipePartIngredient> usedOriginalPis
    ) {
        if (originalPart == null) return null;

        RecipePartIngredient originalPi;

        if (tempPi.getOrderIndex() != null) {
            // 순서 기반 매칭
            originalPi = originalPart.getIngredients().stream()
                    .filter(pi -> pi.getOrderIndex() != null)
                    .filter(pi -> pi.getOrderIndex().equals(tempPi.getOrderIndex()))
                    .findFirst()
                    .orElse(null);
        } else {
            // 순서 없는 경우 → 이름 기반 매칭 (중복 방지 포함)
            originalPi = originalPart.getIngredients().stream()
                    .filter(pi -> pi.getIngredient().getName().equals(tempPi.getIngredient().getName()))
                    .filter(pi -> !usedOriginalPis.contains(pi)) // 이미 매칭된 건 제외
                    .findFirst()
                    .orElse(null);
        }

        if (originalPi != null) {
            usedOriginalPis.add(originalPi); // 사용 표시
        }

        return originalPi;
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
            Long panId,
            List<RecipePartDto> newParts,
            Long newCategoryId
    ) {
        // 레시피 기본 정보 업데이트
        Recipe recipe = findById(recipeId);
        recipe.update(newName, newDescription, newOutputQuantity);
        categoryService.connectRecipe(recipe, newCategoryId);

        if(panId == null) panId = 0L;
        Optional<Pan> panOptional = panService.findByIdOptional(panId);
        panOptional.ifPresent(recipe::setPan);

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

            int orderIndex = 1;

            // DTO 순서대로 RecipePartIngredient 생성
            for (RecipeIngredientDto newIngredientDto : newPartDto.ingredients()) {
                Ingredient ingredient = ingredientService.findOrCreate(
                        newIngredientDto.ingredientName(),
                        newIngredientDto.unit()
                );

                RecipePartIngredient partIngredient = RecipePartIngredient.builder()
                        .recipePart(part)
                        .ingredient(ingredient)
                        .quantity(newIngredientDto.quantity())
                        .orderIndex(orderIndex++)
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

    private String getAllCategories() {
        List<CategoryResponse> allCategories = categoryService.getAllCategories();

        StringBuilder categories = new StringBuilder();
        for(CategoryResponse categoryResponse : allCategories) {
            String name = categoryResponse.categoryName();
            Long id = categoryResponse.categoryId();
            categories.append(id).append(":").append(name).append("/");
        }

        return categories.toString();
    }

    @Transactional
    public RecipeAutoRegisterResponse autoRegister(String text) {
        String categories = getAllCategories();

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

    @Transactional
    public RecipeAutoRegisterResponse autoRegisterWithUrl(String url) {
        String videoId = YoutubeUtils.extractVideoId(url);

        VideoListResponse videoInfo = youtubeApiClient.getVideoInfo(videoId, youtubeApiKey);
        String videoInfoText = YoutubeUtils.getVideoInfoText(videoInfo);

        return autoRegister(videoInfoText);
    }
}
