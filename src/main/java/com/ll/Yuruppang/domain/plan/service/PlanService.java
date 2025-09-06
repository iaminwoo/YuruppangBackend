package com.ll.Yuruppang.domain.plan.service;

import com.ll.Yuruppang.domain.inventory.entity.Ingredient;
import com.ll.Yuruppang.domain.inventory.entity.dto.request.IngredientUseRequest;
import com.ll.Yuruppang.domain.inventory.service.IngredientService;
import com.ll.Yuruppang.domain.plan.dto.PlanIdResponse;
import com.ll.Yuruppang.domain.plan.dto.PlanSimpleResponse;
import com.ll.Yuruppang.domain.plan.dto.complete.PlanCompleteRequest;
import com.ll.Yuruppang.domain.plan.dto.complete.PlanRecipeCompleteDto;
import com.ll.Yuruppang.domain.plan.dto.detailResponse.*;
import com.ll.Yuruppang.domain.plan.dto.modifyPlan.PlanRecipeOutputModifyPercentRequest;
import com.ll.Yuruppang.domain.plan.dto.modifyPlan.PlanRecipeOutputModifyRequest;
import com.ll.Yuruppang.domain.plan.entity.BakingPlan;
import com.ll.Yuruppang.domain.plan.entity.BakingPlanRecipe;
import com.ll.Yuruppang.domain.plan.repository.PlanRecipeRepository;
import com.ll.Yuruppang.domain.plan.repository.PlanRepository;
import com.ll.Yuruppang.domain.recipe.dto.RecipeIngredientDto;
import com.ll.Yuruppang.domain.recipe.dto.RecipePartDto;
import com.ll.Yuruppang.domain.recipe.dto.RecipePartPercentDto;
import com.ll.Yuruppang.domain.recipe.entity.Recipe;
import com.ll.Yuruppang.domain.recipe.entity.RecipePart;
import com.ll.Yuruppang.domain.recipe.entity.RecipePartIngredient;
import com.ll.Yuruppang.domain.recipe.entity.RecipeType;
import com.ll.Yuruppang.domain.recipe.repository.RecipeRepository;
import com.ll.Yuruppang.domain.recipe.service.PanService;
import com.ll.Yuruppang.domain.recipe.service.RecipeService;
import com.ll.Yuruppang.global.exceptions.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanService {
    private final PlanRepository planRepository;
    private final PlanRecipeRepository planRecipeRepository;

    private final RecipeService recipeService;
    private final RecipeRepository recipeRepository;
    private final IngredientService ingredientService;
    private final PanService panService;

    @PersistenceContext
    private EntityManager em;

    private BakingPlan findById(Long planId) {
        return planRepository.findWithAllById(planId)
                .orElseThrow(ErrorCode.PLAN_NOT_FOUND::throwServiceException);
    }

    @Transactional
    public PlanIdResponse makePlan(List<Long> recipes) {
        LocalDate today = LocalDate.now();
        String formattedDate = today.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"));
        String planName = formattedDate + " 등록한 베이킹 플랜";

        BakingPlan plan = BakingPlan.builder()
                .name(planName)
                .createdAt(today)
                .build();
        planRepository.save(plan);

        for (Long recipeId : recipes) {
            Recipe originalRecipe = recipeService.findById(recipeId);

            Recipe placeholderRecipe = recipeService.getPlaceholderRecipe();

            BakingPlanRecipe planRecipe = BakingPlanRecipe.builder()
                    .originalRecipe(originalRecipe)
                    .customizedRecipe(placeholderRecipe)
                    .build();

            plan.addRecipe(planRecipe);
        }

        return new PlanIdResponse(plan.getId());
    }

    @Transactional(readOnly = true)
    public Page<PlanSimpleResponse> getPlans(@NonNull Pageable pageable) {
        return planRepository.findAll(pageable)
                .map(plan -> {
                    Set<BakingPlanRecipe> recipes = plan.getRecipes();

                    List<String> recipeNames = new ArrayList<>();
                    for(BakingPlanRecipe planRecipe : recipes) {
                        if(planRecipe.getCustomizedRecipe().getRecipeType().equals(RecipeType.PLACEHOLDER)) {
                            recipeNames.add(planRecipe.getOriginalRecipe().getName());
                        } else {
                            String fullName = planRecipe.getCustomizedRecipe().getName();
                            String displayName;

                            int index = fullName.indexOf("(수정중)");
                            if (index != -1) {
                                displayName = fullName.substring(0, index + "(수정중)".length());
                            } else {
                                displayName = fullName;
                            }

                            recipeNames.add(displayName);
                        }
                    }

                    int recipeCount = recipes.size();

                    return new PlanSimpleResponse(
                            plan.getId(), plan.getName(), plan.getCreatedAt(), plan.getCompletedAt(),
                            recipeNames, recipeCount, plan.isComplete()
                    );
                });
    }

    @Transactional(readOnly = true)
    public PlanDetailGetResponse getPlanDetail(Long planId) {
        BakingPlan plan = findById(planId);

        List<ComparedPlanRecipeDetailDto> recipeDetails = new ArrayList<>();
        List<IngredientLackDto> lackIngredients = new ArrayList<>();

        // 레시피 별
        for(BakingPlanRecipe planRecipe : plan.getRecipes()) {
            Recipe originalRecipe = planRecipe.getOriginalRecipe();

            Recipe tempRecipe;
            boolean isTemp = false;
            if(planRecipe.getCustomizedRecipe().getRecipeType().equals(RecipeType.PLACEHOLDER)) {
                tempRecipe = planRecipe.getOriginalRecipe();
            } else {
                tempRecipe = planRecipe.getCustomizedRecipe();
                isTemp = true;
            }

            Map<String, RecipePart> originalPartMap = originalRecipe.getParts().stream()
                    .collect(Collectors.toMap(RecipePart::getName, Function.identity()));

            BigDecimal totalPrice = BigDecimal.ZERO;

            List<ComparedPartDto> comparedParts = new ArrayList<>();
            // 파트 별
            List<RecipePart> sortedParts = new ArrayList<>(tempRecipe.getParts());
            sortedParts.sort(Comparator.comparing(RecipePart::getName));

            for (RecipePart tempPart : sortedParts) {

                RecipePart originalPart = originalPartMap.get(tempPart.getName());
                List<ComparedIngredientDto> comparedIngredients = new ArrayList<>();
                List<RecipePartIngredient> tempIngredients = new ArrayList<>(tempPart.getIngredients());
                Set<RecipePartIngredient> usedOriginalPis = new HashSet<>();

                for (RecipePartIngredient partIngredient : tempIngredients) {
                    Ingredient ingredient = partIngredient.getIngredient();
                    BigDecimal customizedQuantity = partIngredient.getQuantity();

                    RecipePartIngredient originalPi = null;

                    if (originalPart != null) {
                        // 동일 ingredientId를 가진 원본 재료 목록
                        List<RecipePartIngredient> originalPis = originalPart.getIngredients().stream()
                                .filter(pi -> pi.getIngredient().getId().equals(ingredient.getId()))
                                .toList();

                        // 아직 사용되지 않은 첫 번째 원본 재료 선택
                        originalPi = originalPis.stream()
                                .filter(pi -> !usedOriginalPis.contains(pi))
                                .findFirst()
                                .orElse(null);

                        if (originalPi != null) {
                            usedOriginalPis.add(originalPi); // 사용 표시
                        }
                    }

                    BigDecimal originalQuantity = originalPi != null ? originalPi.getQuantity() : BigDecimal.ZERO;

                    comparedIngredients.add(new ComparedIngredientDto(
                            ingredient.getId(),
                            partIngredient.getId(),
                            ingredient.getName(),
                            ingredient.getUnit(),
                            originalQuantity,
                            customizedQuantity
                    ));

                    // 원가 계산
                    BigDecimal unitPrice = partIngredient.getIngredient().getUnitPrice();
                    BigDecimal quantity = partIngredient.getQuantity();
                    BigDecimal density = partIngredient.getIngredient().getDensity();
                    totalPrice = totalPrice.add(unitPrice.multiply(quantity.divide(density, 2, RoundingMode.HALF_UP)));
                }

                // 재료 id 순 (등록된 순으로 정렬)
                comparedIngredients.sort(Comparator.comparing(ComparedIngredientDto::ingredientPartId));

                comparedParts.add(new ComparedPartDto(
                        tempPart.getId(), tempPart.getName(), tempPart.getPercent(), comparedIngredients
                ));
            }

            // 파트 번호순 (등록된 순으로 정렬)
            comparedParts.sort(Comparator.comparing(ComparedPartDto::partId));

            recipeDetails.add(new ComparedPlanRecipeDetailDto(
                    originalRecipe.getId(),
                    originalRecipe.getName(),
                    originalRecipe.getDescription(),
                    totalPrice,
                    tempRecipe.getName(),
                    tempRecipe.getDescription(),
                    panService.makePanResponse(tempRecipe.getPan()),
                    isTemp,
                    originalRecipe.getOutputQuantity(),
                    tempRecipe.getOutputQuantity(),
                    new BigDecimal(tempRecipe.getOutputQuantity())
                            .multiply(new BigDecimal(100))
                            .divide(new BigDecimal(originalRecipe.getOutputQuantity()), 0, RoundingMode.HALF_UP)
                            .intValue(),
                    comparedParts
            ));
        }

        // 레시피 이름 순으로 정렬
        recipeDetails.sort(Comparator.comparing(ComparedPlanRecipeDetailDto::recipeName));

        Map<Long, BigDecimal> totalIngredient = new HashMap<>();
        for(ComparedPlanRecipeDetailDto recipeDetailDto : recipeDetails) {
            for(ComparedPartDto comparedPartDto : recipeDetailDto.comparedParts()) {
                for(ComparedIngredientDto ingredientDto : comparedPartDto.comparedIngredients()){
                    totalIngredient.put(ingredientDto.ingredientId(),
                            totalIngredient.getOrDefault(ingredientDto.ingredientId(), BigDecimal.ZERO)
                                    .add(ingredientDto.customizedQuantity()));
                }
            }
        }

        for(Long ingredientId : totalIngredient.keySet()) {
            Ingredient ingredient = ingredientService.findById(ingredientId);
            BigDecimal customizedQuantity = totalIngredient.get(ingredientId);

            // 부족 재료 추가
            if(ingredient.getTotalStock().compareTo(customizedQuantity) < 0) {
                lackIngredients.add(new IngredientLackDto(
                        ingredientId, ingredient.getName(),
                        customizedQuantity, ingredient.getTotalStock(),
                        customizedQuantity.subtract(ingredient.getTotalStock())
                ));
            }
        }

        lackIngredients.sort(Comparator.comparing(IngredientLackDto::name));

        return new PlanDetailGetResponse(
                plan.getName(),
                plan.getMemo(),
                plan.isComplete(),
                recipeDetails,
                lackIngredients
        );
    }

    @Transactional
    public PlanDetailGetResponse modifyOutputQuantity(Long planId, Long recipeId, PlanRecipeOutputModifyRequest outputRequest) {
        Recipe customizedRecipe = getCustomizedRecipe(planId, recipeId);

        updateGoalQuantity(customizedRecipe, outputRequest.newOutput());
        em.flush();
        em.clear();
        return getPlanDetail(planId);
    }

    @Transactional
    public PlanDetailGetResponse modifyOutputQuantityPercent(Long planId, Long recipeId, PlanRecipeOutputModifyPercentRequest outputRequest) {
        CalNewOutput calNewOutput = calNewOutput(planId, recipeId, outputRequest.newPercent());
        Recipe customizedRecipe = calNewOutput.customizedRecipe;
        int newOutput = calNewOutput.newOutput;

        updateGoalQuantity(customizedRecipe, newOutput);
        em.flush();
        em.clear();
        return getPlanDetail(planId);
    }

    private record CalNewOutput(
            Recipe customizedRecipe,
            int newOutput
    ){ }

    private CalNewOutput calNewOutput(Long planId, Long recipeId, int newPercent) {
        BakingPlan plan = findById(planId);

        BakingPlanRecipe planRecipe = plan.getRecipes().stream()
                .filter(pr -> pr.getOriginalRecipe().getId().equals(recipeId))
                .findFirst()
                .orElseThrow(ErrorCode.RECIPE_NOT_FOUND::throwServiceException);

        Recipe customizedRecipe = getOrCreateCustomizedRecipe(planRecipe);

        int newOutput = (int) (planRecipe.getOriginalRecipe().getOutputQuantity() * (newPercent / 100.0));

        return new CalNewOutput(customizedRecipe, newOutput);
    }

    // 목표 수량만 변경
    public void updateGoalQuantity(Recipe customizedRecipe, int newGoalQuantity) {
        int oldOutput = customizedRecipe.getOutputQuantity();
        for(RecipePart part : customizedRecipe.getParts()) {
            // 재료 수량 배율 변경
            for(RecipePartIngredient partIngredient : part.getIngredients()) {
                partIngredient.multiply(oldOutput, newGoalQuantity);
            }
        }

        // 레시피 output 변경
        customizedRecipe.setOutputQuantity(newGoalQuantity);
    }

    private Recipe getOrCreateCustomizedRecipe(BakingPlanRecipe planRecipe) {
        RecipeType recipeType = planRecipe.getCustomizedRecipe().getRecipeType();
        if(recipeType.equals(RecipeType.PLACEHOLDER)) {
            Recipe newRecipe = recipeRepository.save(Recipe.copyOf(planRecipe.getOriginalRecipe()));
            planRecipe.setCustomizedRecipe(newRecipe);
            return newRecipe;
        } else {
            return planRecipe.getCustomizedRecipe();
        }
    }

    @Transactional
    public PlanDetailGetResponse modifyIngredients(Long planId, Long recipeId, List<RecipePartDto> parts) {
        Recipe customizedRecipe = getCustomizedRecipe(planId, recipeId);
        modifyRecipe(customizedRecipe, parts);

        em.flush();
        em.clear();
        return getPlanDetail(planId);
    }

    @Transactional
    public PlanDetailGetResponse modifyIngredientsPercent(Long planId, Long recipeId, List<RecipePartPercentDto> percents) {
        Recipe customizedRecipe = getCustomizedRecipe(planId, recipeId);
        List<RecipePartDto> parts = makeParts(customizedRecipe, percents);
        modifyRecipe(customizedRecipe, parts);

        em.flush();
        em.clear();
        return getPlanDetail(planId);
    }

    private Recipe getCustomizedRecipe(Long planId, Long recipeId) {
        BakingPlan plan = findById(planId);

        BakingPlanRecipe planRecipe = plan.getRecipes().stream()
                .filter(pr -> pr.getOriginalRecipe().getId().equals(recipeId))
                .findFirst()
                .orElseThrow(ErrorCode.RECIPE_NOT_FOUND::throwServiceException);

        return getOrCreateCustomizedRecipe(planRecipe);
    }

    private List<RecipePartDto> makeParts(Recipe customizedRecipe, List<RecipePartPercentDto> percents) {
        Map<String, RecipePart> partMap = customizedRecipe.getParts().stream()
                .collect(Collectors.toMap(RecipePart::getName, Function.identity()));

        List<RecipePartDto> parts = new ArrayList<>();
        for(RecipePartPercentDto dto : percents) {
            RecipePart part = partMap.get(dto.partName());

            BigDecimal ratio = new BigDecimal(dto.percent())
                    .divide(new BigDecimal(part.getPercent()), 2, RoundingMode.HALF_UP);

            part.setPercent(dto.percent());

            List<RecipeIngredientDto> list = part.getIngredients().stream()
                    .map(recipePartIngredient -> new RecipeIngredientDto(
                            recipePartIngredient.getIngredient().getName(),
                            recipePartIngredient.getQuantity().multiply(ratio),
                            recipePartIngredient.getIngredient().getUnit()
                    ))
                    .toList();

            parts.add(new RecipePartDto(part.getName(), list));
        }

        return parts;
    }

    private void modifyRecipe(Recipe customizedRecipe, List<RecipePartDto> parts) {
        recipeService.modifyRecipe(customizedRecipe.getId(),
                customizedRecipe.getName(), customizedRecipe.getDescription(), customizedRecipe.getOutputQuantity(),
                customizedRecipe.getPan().getId(), parts, customizedRecipe.getCategory().getId());
    }

    @Transactional
    public PlanDetailGetResponse resetRecipe(Long planId, Long recipeId) {
        reset(planId, recipeId);
        em.flush();
        em.clear();
        return getPlanDetail(planId);
    }

    private void reset(Long planId, Long recipeId) {
        BakingPlan plan = findById(planId);

        BakingPlanRecipe planRecipe = plan.getRecipes().stream()
                .filter(pr -> pr.getOriginalRecipe().getId().equals(recipeId))
                .findFirst()
                .orElseThrow(ErrorCode.RECIPE_NOT_FOUND::throwServiceException);

        Recipe customizedRecipe = planRecipe.getCustomizedRecipe();
        if(customizedRecipe.getRecipeType().equals(RecipeType.TEMP)) {
            planRecipe.setCustomizedRecipe(recipeService.getPlaceholderRecipe());
            recipeService.deleteRecipe(customizedRecipe.getId());
        }
    }

    @Transactional
    public void deletePlan(Long planId) {
        BakingPlan plan = findById(planId);

        Set<BakingPlanRecipe> planRecipes = plan.getRecipes();

        for(BakingPlanRecipe planRecipe : planRecipes) {
            planRecipeRepository.delete(planRecipe);

            Recipe recipe = planRecipe.getCustomizedRecipe();
            if(recipe.getRecipeType().equals(RecipeType.TEMP)) {
                recipeRepository.delete(recipe);
            }
        }

        planRepository.delete(plan);
    }

    @Transactional
    public void completePlan(Long planId, PlanCompleteRequest request) {
        BakingPlan plan = findById(planId);

        // 임시 레시피 정식 등록
        for(BakingPlanRecipe planRecipe : plan.getRecipes()) {
            Recipe originalRecipe = planRecipe.getOriginalRecipe();
            Recipe customRecipe = planRecipe.getCustomizedRecipe();
            Recipe usedRecipe;

            if(customRecipe.getRecipeType().equals(RecipeType.TEMP)) {
                PlanRecipeCompleteDto matchedDto = request.recipes().stream()
                        .filter(dto -> originalRecipe.getId().equals(dto.recipeId()))
                        .findFirst()
                        .orElseThrow(ErrorCode.TEMP_RECIPE_NOT_REGISTERED::throwServiceException);

                customRecipe.update(matchedDto.newName(), matchedDto.newDescription(), customRecipe.getOutputQuantity());
                customRecipe.register();

                usedRecipe = customRecipe;
            } else {
                usedRecipe = originalRecipe;
            }

            // 소비 기록 처리
            List<IngredientUseRequest> useList = getIngredientUseRequests(usedRecipe);
            ingredientService.useIngredient(usedRecipe.getName() + " 제작", useList, LocalDate.now());
        }

        LocalDate today = LocalDate.now();
        String formattedDate = today.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"));
        String planName = formattedDate + " 완성한 베이킹 플랜";

        plan.complete(planName);
    }

    private List<IngredientUseRequest> getIngredientUseRequests(Recipe recipe) {
        // 한 레시피에서 사용된 재료는 통합하기 위해서 map 사용
        Map<String, BigDecimal> ingredientMap = new HashMap<>();

        for (RecipePart part : recipe.getParts()) {
            for (RecipePartIngredient partIngredient : part.getIngredients()) {
                String name = partIngredient.getIngredient().getName();
                BigDecimal quantity = partIngredient.getQuantity();

                ingredientMap.put(name, ingredientMap.getOrDefault(name, BigDecimal.ZERO).add(quantity));
            }
        }

        return ingredientMap.entrySet().stream()
                .map(e -> new IngredientUseRequest(e.getKey(), e.getValue().toPlainString()))
                .collect(Collectors.toList());
    }

    @Transactional
    public PlanDetailGetResponse modifyMemo(Long planId, String newMemo) {
        changeMemo(planId, newMemo);
        em.flush();
        em.clear();
        return getPlanDetail(planId);
    }

    private void changeMemo(Long planId, String newMemo) {
        BakingPlan plan = findById(planId);

        plan.changeMemo(newMemo);
    }

    @Transactional
    public PlanDetailGetResponse addRecipe(Long planId, Long newRecipeId) {
        addOneRecipe(planId, newRecipeId);
        em.flush();
        em.clear();
        return getPlanDetail(planId);
    }

    private void addOneRecipe(Long planId, Long newRecipeId) {
        BakingPlan plan = findById(planId);

        Recipe originalRecipe = recipeService.findById(newRecipeId);

        Recipe placeholderRecipe = recipeService.getPlaceholderRecipe();

        BakingPlanRecipe planRecipe = BakingPlanRecipe.builder()
                .originalRecipe(originalRecipe)
                .customizedRecipe(placeholderRecipe)
                .build();

        plan.addRecipe(planRecipe);
        planRepository.save(plan);
    }

    @Transactional
    public PlanDetailGetResponse deleteRecipe(Long planId, Long recipeId) {
        deleteOneRecipe(planId, recipeId);
        em.flush();
        em.clear();
        return getPlanDetail(planId);
    }

    private void deleteOneRecipe(Long planId, Long recipeId) {
        BakingPlan plan = findById(planId);

        BakingPlanRecipe planRecipe = plan.getRecipes().stream()
                .filter(pr -> pr.getOriginalRecipe().getId().equals(recipeId))
                .findFirst()
                .orElseThrow(ErrorCode.RECIPE_NOT_FOUND::throwServiceException);

        Recipe customizedRecipe = planRecipe.getCustomizedRecipe();
        if(customizedRecipe.getRecipeType().equals(RecipeType.TEMP)) {
            recipeRepository.delete(customizedRecipe);
        }

        plan.deleteRecipe(planRecipe);
        planRepository.save(plan);
    }

    @Transactional
    public void changeDescription(Long planId, Long recipeId, String newDescription) {
        BakingPlan plan = findById(planId);

        BakingPlanRecipe planRecipe = plan.getRecipes().stream()
                .filter(pr -> pr.getOriginalRecipe().getId().equals(recipeId))
                .findFirst()
                .orElseThrow(ErrorCode.RECIPE_NOT_FOUND::throwServiceException);

        Recipe customizedRecipe = getOrCreateCustomizedRecipe(planRecipe);
        customizedRecipe.changeDescription(newDescription);
        recipeRepository.save(customizedRecipe);
    }
}

