package com.ll.Yuruppang.domain.inventory.repository;

import com.ll.Yuruppang.domain.inventory.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    Optional<Ingredient> findByName(String name);

    List<Ingredient> findByNameContainingIgnoreCaseOrderByNameAsc(String keyword);
}
