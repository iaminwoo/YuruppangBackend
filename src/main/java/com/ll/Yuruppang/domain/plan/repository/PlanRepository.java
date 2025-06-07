package com.ll.Yuruppang.domain.plan.repository;

import com.ll.Yuruppang.domain.plan.entity.BakingPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<BakingPlan, Long> {
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"recipes", "recipes.originalRecipe",})
    Page<BakingPlan> findAll(@NonNull Pageable pageable);

    @Query("""
        SELECT DISTINCT p
        FROM BakingPlan p
        LEFT JOIN FETCH p.recipes pr
        LEFT JOIN FETCH pr.originalRecipe orr
        LEFT JOIN FETCH pr.customizedRecipe cur
        LEFT JOIN FETCH orr.parts orp
        LEFT JOIN FETCH orp.ingredients oriIng
        LEFT JOIN FETCH oriIng.ingredient oriIngI
        LEFT JOIN FETCH cur.parts curp
        LEFT JOIN FETCH curp.ingredients curIng
        LEFT JOIN FETCH curIng.ingredient curIngI
        WHERE p.id = :id
    """)
    Optional<BakingPlan> findWithAllById(@Param("id") Long id);
}

