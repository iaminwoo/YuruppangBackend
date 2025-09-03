package com.ll.Yuruppang.global.openFeign.fastAPI;

import com.ll.Yuruppang.domain.recipe.dto.autoRegister.RecipeAutoRegisterResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "fastApiClient", url = "http://fastapi-app:8000/generate-recipe")
public interface FastApiClient {

    @PostMapping(consumes = "application/json")
    RecipeAutoRegisterResponse generateRecipe(
            @RequestBody VideoURLRequest request
    );
}
