package com.ll.Yuruppang.domain.recipe.contorller;

import com.ll.Yuruppang.domain.recipe.dto.category.CategoryRecipeResponse;
import com.ll.Yuruppang.domain.recipe.dto.category.CategoryRequest;
import com.ll.Yuruppang.domain.recipe.dto.category.CategoryResponse;
import com.ll.Yuruppang.domain.recipe.dto.category.DeleteCategoryRequest;
import com.ll.Yuruppang.domain.recipe.service.CategoryService;
import com.ll.Yuruppang.global.response.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class RecipeCategoryController {
    private final CategoryService categoryService;

    @PostMapping
    public RsData<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return RsData.success(HttpStatus.OK, categoryService.createCategory(request.name()));
    }

    @GetMapping
    public RsData<List<CategoryResponse>> getCategories() {
        return RsData.success(HttpStatus.OK, categoryService.getAllCategories());
    }

    @GetMapping("/{categoryId}")
    public RsData<CategoryResponse> getCategory(@PathVariable Long categoryId) {
        return RsData.success(HttpStatus.OK, categoryService.getCategoryDetail(categoryId));
    }

    @GetMapping("/{categoryId}/recipes")
    public RsData<List<CategoryRecipeResponse>> getCategoryRecipes(@PathVariable Long categoryId) {
        return RsData.success(HttpStatus.OK, categoryService.getCategoryRecipes(categoryId));
    }

    @PutMapping("/{categoryId}")
    public RsData<String> changeCategoryName(@PathVariable Long categoryId, @Valid @RequestBody CategoryRequest request) {
        categoryService.changeCategoryName(categoryId, request.name());
        return RsData.success(HttpStatus.OK, "카테고리 이름이 변경되었습니다.");
    }

    @DeleteMapping("/{categoryId}")
    public RsData<String> deleteCategory(@PathVariable Long categoryId, @Valid @RequestBody DeleteCategoryRequest request) {
        categoryService.deleteCategory(categoryId, request);
        return RsData.success(HttpStatus.OK, "카테고리가 삭제되었습니다.");
    }
}
