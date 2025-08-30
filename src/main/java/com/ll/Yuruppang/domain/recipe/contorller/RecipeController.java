package com.ll.Yuruppang.domain.recipe.contorller;

import com.ll.Yuruppang.domain.recipe.dto.RecipeCreateRequest;
import com.ll.Yuruppang.domain.recipe.dto.RecipeCreateResponse;
import com.ll.Yuruppang.domain.recipe.dto.RecipeGetResponse;
import com.ll.Yuruppang.domain.recipe.dto.RecipeNameDto;
import com.ll.Yuruppang.domain.recipe.dto.autoRegister.RecipeAutoRegisterRequest;
import com.ll.Yuruppang.domain.recipe.dto.autoRegister.RecipeAutoRegisterResponse;
import com.ll.Yuruppang.domain.recipe.service.RecipeService;
import com.ll.Yuruppang.global.response.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {
    private final RecipeService recipeService;

    @PostMapping
    public RsData<RecipeCreateResponse> createRecipe(@Valid @RequestBody RecipeCreateRequest request) {
        return RsData.success(HttpStatus.OK, recipeService.createRecipe(
                request.name(), request.description(), request.outputQuantity(),
                request.parts(), request.categoryId()
        ));
    }

    @PostMapping("/auto-register")
    public RsData<RecipeAutoRegisterResponse> autoRegisterRecipe(@Valid @RequestBody RecipeAutoRegisterRequest request) {
        return RsData.success(HttpStatus.OK, recipeService.autoRegister(request.text()));
    }

    @GetMapping("/{recipeId}")
    public RsData<RecipeGetResponse> getRecipe(@PathVariable Long recipeId) {
        return RsData.success(HttpStatus.OK, recipeService.getRecipe(recipeId));
    }

    @GetMapping
    public RsData<Page<RecipeNameDto>> getRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long category,
            @RequestParam(defaultValue = "false") boolean favorite,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "id") String sortBy
    ) {
        Sort.Direction direction = sortBy.equals("id") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return RsData.success(HttpStatus.OK, recipeService.getRecipes(pageable, category, favorite, keyword));
    }

    @PutMapping("/{recipeId}")
    public RsData<String> modifyRecipe(@Valid @RequestBody RecipeCreateRequest request, @PathVariable Long recipeId) {
        recipeService.modifyRecipe(
                recipeId, request.name(), request.description(),
                request.outputQuantity(), request.parts(), request.categoryId()
        );
        return RsData.success(HttpStatus.OK, "레시피가 수정되었습니다.");
    }

    @DeleteMapping("/{recipeId}")
    public RsData<String> deleteRecipe(@PathVariable Long recipeId) {
        recipeService.deleteRecipe(recipeId);
        return RsData.success(HttpStatus.OK, "레시피가 삭제되었습니다.");
    }

    @PatchMapping("/{recipeId}/favorite")
    public RsData<String> favoriteRecipe(@PathVariable Long recipeId) {
        recipeService.favoriteRecipe(recipeId);
        return RsData.success(HttpStatus.OK, "레시피 즐겨찾기 토글이 완료되었습니다.");
    }
}
