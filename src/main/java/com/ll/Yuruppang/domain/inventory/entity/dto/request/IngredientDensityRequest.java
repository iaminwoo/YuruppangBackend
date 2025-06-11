package com.ll.Yuruppang.domain.inventory.entity.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record IngredientDensityRequest(
        @NotNull(message = "기준 부피(unitVolume)는 필수입니다.")
        @DecimalMin(value = "0.0001", inclusive = false, message = "기준 부피는 0보다 커야 합니다.")
        BigDecimal unitVolume,

        @NotNull(message = "측정 무게(unitWeight)는 필수입니다.")
        @DecimalMin(value = "0.0001", inclusive = false, message = "측정 무게는 0보다 커야 합니다.")
        BigDecimal unitWeight
) {
}
