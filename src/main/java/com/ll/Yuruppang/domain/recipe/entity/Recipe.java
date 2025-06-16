package com.ll.Yuruppang.domain.recipe.entity;

import com.ll.Yuruppang.global.exceptions.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "output_quantity")
    private int outputQuantity;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private RecipeType recipeType = RecipeType.NORMAL;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder.Default
    private boolean favorite = false;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RecipePart> parts = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private RecipeCategory category;

    public void addPart(RecipePart part) {
        this.parts.add(part);
        part.setRecipe(this);
    }

    public void update(String newName, String newDescription, int newOutputQuantity) {
        this.name = newName;
        this.description = newDescription;
        this.outputQuantity = newOutputQuantity;
    }

    public void register() {
        if(!this.recipeType.equals(RecipeType.TEMP)) throw ErrorCode.RECIPE_NOT_TEMP.throwServiceException();
        this.recipeType = RecipeType.NORMAL;
    }

    public static Recipe copyOf(Recipe originalRecipe) {
        Recipe copy = Recipe.builder()
                .name(originalRecipe.getName() + "(수정중)" + UUID.randomUUID().toString().substring(0, 8))
                .description(originalRecipe.getDescription())
                .outputQuantity(originalRecipe.getOutputQuantity())
                .category(originalRecipe.getCategory())
                .recipeType(RecipeType.TEMP)
                .build();

        List<RecipePart> partList = originalRecipe.getParts().stream()
                .sorted(Comparator.comparing(RecipePart::getId))
                .toList();

        for(RecipePart part : partList) {
            RecipePart copiedPart = RecipePart.builder()
                    .recipe(copy)
                    .name(part.getName())
                    .build();

            for(RecipePartIngredient recipeIngredient : part.getIngredients()) {
                RecipePartIngredient copiedRecipeIngredient = RecipePartIngredient.builder()
                        .ingredient(recipeIngredient.getIngredient())
                        .quantity(recipeIngredient.getQuantity())
                        .recipePart(copiedPart)
                        .build();
                copiedPart.addIngredient(copiedRecipeIngredient);
            }

            copy.addPart(copiedPart);
        }
        return copy;
    }

    public void changeFavorite() {
        this.favorite = !this.favorite;
    }
}
