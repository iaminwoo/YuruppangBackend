package com.ll.Yuruppang.domain.inventory.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record IngredientUseListRequest(
        @NotBlank String description,
        @NotNull LocalDate actualAt,
        @NotNull List<IngredientUseRequest> requestList
        ) {}
