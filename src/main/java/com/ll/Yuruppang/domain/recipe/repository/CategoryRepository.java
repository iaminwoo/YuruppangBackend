package com.ll.Yuruppang.domain.recipe.repository;

import com.ll.Yuruppang.domain.recipe.entity.RecipeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface CategoryRepository extends JpaRepository<RecipeCategory, Long> {
    Optional<RecipeCategory> findByName(String categoryName);
}
