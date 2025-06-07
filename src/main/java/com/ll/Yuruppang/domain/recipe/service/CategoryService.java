package com.ll.Yuruppang.domain.recipe.service;

import com.ll.Yuruppang.domain.recipe.dto.category.CategoryRecipeResponse;
import com.ll.Yuruppang.domain.recipe.dto.category.CategoryResponse;
import com.ll.Yuruppang.domain.recipe.dto.category.DeleteCategoryRecipesDto;
import com.ll.Yuruppang.domain.recipe.dto.category.DeleteCategoryRequest;
import com.ll.Yuruppang.domain.recipe.entity.Recipe;
import com.ll.Yuruppang.domain.recipe.entity.RecipeCategory;
import com.ll.Yuruppang.domain.recipe.repository.CategoryRepository;
import com.ll.Yuruppang.domain.recipe.repository.RecipeRepository;
import com.ll.Yuruppang.global.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final RecipeRepository recipeRepository;

    public RecipeCategory findById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(ErrorCode.CATEGORY_NOT_FOUND::throwServiceException);
    }

    public List<Recipe> findAllByCategory(RecipeCategory category) {
        return recipeRepository.findAllByCategory(category);
    }

    @Transactional
    public CategoryResponse createCategory(String name) {
        try {
            RecipeCategory category = categoryRepository.save(RecipeCategory.builder().name(name).build());
            return new CategoryResponse(category.getId(), category.getName());
        } catch (DataIntegrityViolationException exception) {
            throw ErrorCode.CATEGORY_ALREADY_EXIST.throwServiceException();
        }
    }

    @Transactional
    public void connectRecipe(Recipe recipe, Long categoryId) {
        RecipeCategory category = findById(categoryId);
        recipe.setCategory(category);
    }

    @Transactional
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName())).toList();
    }

    @Transactional
    public CategoryResponse getCategoryDetail(Long categoryId) {
        RecipeCategory category = findById(categoryId);
        return new CategoryResponse(category.getId(), category.getName());
    }

    @Transactional
    public void changeCategoryName(Long categoryId, String newName) {
        RecipeCategory category = findById(categoryId);
        category.setName(newName);
    }

    @Transactional
    public void deleteCategory(Long categoryId, DeleteCategoryRequest request) {
        RecipeCategory categoryToDelete = findById(categoryId);
        final Map<Long, RecipeCategory> categoryMap = getAllCategoryMap();
        final List<Recipe> recipes = findAllByCategory(categoryToDelete);
        final Map<Long, Recipe> recipeMap = recipes.stream().collect(Collectors.toMap(Recipe::getId, Function.identity()));

        for(DeleteCategoryRecipesDto dto : request.recipes()) {
            Recipe recipe = recipeMap.get(dto.recipeId());
            RecipeCategory newCategory = categoryMap.get(dto.newCategoryId());
            if(newCategory == null) throw ErrorCode.CATEGORY_NOT_FOUND.throwServiceException();
            recipe.setCategory(newCategory);
        }

        categoryRepository.delete(categoryToDelete);
    }

    private Map<Long, RecipeCategory> getAllCategoryMap() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(RecipeCategory::getId, Function.identity()));
    }

    @Transactional(readOnly = true)
    public List<CategoryRecipeResponse> getCategoryRecipes(Long categoryId) {
        return recipeRepository.findAllByCategory(findById(categoryId)).stream()
                .map(recipe -> new CategoryRecipeResponse(recipe.getId(), recipe.getName())).toList();
    }
}
