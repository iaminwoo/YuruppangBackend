package com.ll.Yuruppang.domain.plan.controller;

import com.ll.Yuruppang.domain.plan.dto.PlanAddRecipeRequest;
import com.ll.Yuruppang.domain.plan.dto.PlanCreateRequest;
import com.ll.Yuruppang.domain.plan.dto.PlanIdResponse;
import com.ll.Yuruppang.domain.plan.dto.PlanSimpleResponse;
import com.ll.Yuruppang.domain.plan.dto.complete.PlanCompleteRequest;
import com.ll.Yuruppang.domain.plan.dto.detailResponse.PlanDetailGetResponse;
import com.ll.Yuruppang.domain.plan.dto.modifyPlan.PlanMemoModifyRequest;
import com.ll.Yuruppang.domain.plan.dto.modifyPlan.PlanRecipeOutputModifyPercentRequest;
import com.ll.Yuruppang.domain.plan.dto.modifyPlan.PlanRecipeOutputModifyRequest;
import com.ll.Yuruppang.domain.plan.service.PlanService;
import com.ll.Yuruppang.domain.recipe.dto.RecipeDescriptionRequest;
import com.ll.Yuruppang.domain.recipe.dto.RecipePartDto;
import com.ll.Yuruppang.domain.recipe.dto.RecipePartPercentDto;
import com.ll.Yuruppang.global.response.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {
    private final PlanService planService;

    @PostMapping
    public RsData<PlanIdResponse> createPlan(@Valid @RequestBody PlanCreateRequest request) {
        return RsData.success(HttpStatus.OK, planService.makePlan(request.recipes()));
    }

    @GetMapping
    public RsData<Page<PlanSimpleResponse>> getPlans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return RsData.success(HttpStatus.OK, planService.getPlans(pageable));
    }

    @GetMapping("/{planId}")
    public RsData<PlanDetailGetResponse> getPlan(@PathVariable Long planId) {
        return RsData.success(HttpStatus.OK, planService.getPlanDetail(planId));
    }

    @PostMapping("/{planId}/recipes")
    public RsData<PlanDetailGetResponse> addRecipe(
            @PathVariable Long planId,
            @RequestBody @Valid PlanAddRecipeRequest request) {
        return RsData.success(HttpStatus.OK, planService.addRecipe(planId, request.recipeId()));
    }

    @DeleteMapping("/{planId}/recipes/{recipeId}")
    public RsData<PlanDetailGetResponse> deleteRecipe(@PathVariable Long planId, @PathVariable Long recipeId) {
        return RsData.success(HttpStatus.OK, planService.deleteRecipe(planId, recipeId));
    }

    @PatchMapping("/{planId}/recipes/{recipeId}/output")
    public RsData<PlanDetailGetResponse> updateRecipeOutputQuantity(
            @PathVariable Long planId, @PathVariable Long recipeId,
            @RequestBody @Valid PlanRecipeOutputModifyRequest outputRequest) {
        return RsData.success(HttpStatus.OK, planService.modifyOutputQuantity(planId, recipeId, outputRequest));
    }

    @PatchMapping("/{planId}/recipes/{recipeId}/output/percent")
    public RsData<PlanDetailGetResponse> updateRecipeOutputQuantityPercent(
            @PathVariable Long planId, @PathVariable Long recipeId,
            @RequestBody @Valid PlanRecipeOutputModifyPercentRequest outputRequest) {
        return RsData.success(HttpStatus.OK, planService.modifyOutputQuantityPercent(planId, recipeId, outputRequest));
    }

    @PatchMapping("/{planId}/recipes/{recipeId}/ingredients")
    public RsData<PlanDetailGetResponse> updateRecipeIngredients(
            @PathVariable Long planId, @PathVariable Long recipeId,
            @RequestBody List<RecipePartDto> parts) {
        return RsData.success(HttpStatus.OK, planService.modifyIngredients(planId, recipeId, parts));
    }

    @PatchMapping("/{planId}/recipes/{recipeId}/ingredients/percent")
    public RsData<PlanDetailGetResponse> updateRecipeIngredientsPercent(
            @PathVariable Long planId, @PathVariable Long recipeId,
            @RequestBody List<RecipePartPercentDto> percents) {
        return RsData.success(HttpStatus.OK, planService.modifyIngredientsPercent(planId, recipeId, percents));
    }

    @PatchMapping("/{planId}/recipes/{recipeId}/reset")
    public RsData<PlanDetailGetResponse> updateRecipeReset(
            @PathVariable Long planId, @PathVariable Long recipeId) {
        return RsData.success(HttpStatus.OK, planService.resetRecipe(planId, recipeId));
    }

    @PatchMapping("/{planId}/recipes/{recipeId}/description")
    public RsData<String> changeDescription(
            @PathVariable Long planId, @PathVariable Long recipeId, @RequestBody RecipeDescriptionRequest request
    ) {
        planService.changeDescription(planId, recipeId, request.newDescription());
        return RsData.success(HttpStatus.OK, "레시피 설명이 수정되었습니다.");
    }

    @PatchMapping("/{planId}/memo")
    public RsData<PlanDetailGetResponse> updateMemo(
            @PathVariable Long planId, @RequestBody @Valid PlanMemoModifyRequest memoRequest) {
        return RsData.success(HttpStatus.OK, planService.modifyMemo(planId, memoRequest.newMemo()));
    }

    @DeleteMapping("/{planId}")
    public RsData<String> deletePlan(@PathVariable Long planId) {
        planService.deletePlan(planId);
        return RsData.success(HttpStatus.OK, "오늘의 유루빵이 삭제되었습니다.");
    }

    @PostMapping("/{planId}")
    public RsData<String> completePlan(@PathVariable Long planId, @Valid @RequestBody PlanCompleteRequest request) {
        planService.completePlan(planId, request);
        return RsData.success(HttpStatus.OK, "오늘의 유루빵이 완성처리 되었습니다. 수고하셨습니다.");
    }
}
