package com.ll.Yuruppang.domain.recipe.repository;

import com.ll.Yuruppang.domain.recipe.entity.Recipe;
import com.ll.Yuruppang.domain.recipe.entity.RecipeCategory;
import com.ll.Yuruppang.domain.recipe.entity.RecipeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    Optional<Recipe> findByName(String name);
    Optional<Recipe> findByRecipeType(RecipeType recipeType);
    List<Recipe> findAllByCategory(RecipeCategory category);

    Page<Recipe> findAllByRecipeType(RecipeType recipeType, Pageable pageable);
    Page<Recipe> findAllByRecipeTypeAndFavorite(RecipeType recipeType, boolean favorite, Pageable pageable);
    Page<Recipe> findAllByRecipeTypeAndCategory(RecipeType recipeType, RecipeCategory category, Pageable pageable);
    Page<Recipe> findAllByRecipeTypeAndCategoryAndFavorite(RecipeType recipeType, RecipeCategory category, boolean favorite, Pageable pageable);
    Page<Recipe> findAllByRecipeTypeAndNameContainingIgnoreCase(RecipeType recipeType, String keyword, Pageable pageable);
    Page<Recipe> findAllByRecipeTypeAndFavoriteAndNameContainingIgnoreCase(RecipeType recipeType, boolean favorite, String keyword, Pageable pageable);
    Page<Recipe> findAllByRecipeTypeAndCategoryAndNameContainingIgnoreCase(RecipeType recipeType, RecipeCategory category, String keyword, Pageable pageable);
    Page<Recipe> findAllByRecipeTypeAndCategoryAndFavoriteAndNameContainingIgnoreCase(RecipeType recipeType, RecipeCategory category, boolean favorite, String keyword, Pageable pageable);
}
