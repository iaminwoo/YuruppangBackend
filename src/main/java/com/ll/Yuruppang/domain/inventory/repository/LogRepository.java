package com.ll.Yuruppang.domain.inventory.repository;

import com.ll.Yuruppang.domain.inventory.entity.Ingredient;
import com.ll.Yuruppang.domain.inventory.entity.IngredientLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogRepository extends JpaRepository<IngredientLog, Long> {
    List<IngredientLog> findByDescription(String description);

    boolean existsByIngredient(Ingredient ingredient);
}
