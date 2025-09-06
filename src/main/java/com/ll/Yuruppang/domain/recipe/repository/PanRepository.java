package com.ll.Yuruppang.domain.recipe.repository;

import com.ll.Yuruppang.domain.recipe.entity.Pan;
import com.ll.Yuruppang.domain.recipe.entity.PanType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface PanRepository extends JpaRepository<Pan, Long> {
    List<Pan> findAllByPanType(PanType panType);
}
