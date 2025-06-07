package com.ll.Yuruppang.global.initData;

import com.ll.Yuruppang.domain.recipe.entity.Recipe;
import com.ll.Yuruppang.domain.recipe.entity.RecipeType;
import com.ll.Yuruppang.domain.recipe.repository.RecipeRepository;
import com.ll.Yuruppang.domain.recipe.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class BaseInitData {

    private final RecipeService recipeService;
    private final RecipeRepository recipeRepository;

    @Bean
    public ApplicationRunner baseInitDataApplicationRunner() {
        return args -> {
            makePlaceholderRecipe();
        };
    }

    private void makePlaceholderRecipe() {
        Optional<Recipe> optionalRecipe = recipeRepository.findByRecipeType(RecipeType.PLACEHOLDER);
        if(optionalRecipe.isEmpty()) {
            recipeRepository.save(recipeRepository.save(Recipe.builder()
                    .name("PLACEHOLDER")
                    .recipeType(RecipeType.PLACEHOLDER)
                    .build()));
        }
    }
}
