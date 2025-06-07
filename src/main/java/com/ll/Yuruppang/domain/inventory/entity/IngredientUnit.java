package com.ll.Yuruppang.domain.inventory.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum IngredientUnit {
    G("g"),
    ML("ml"),
    EA("개");

    private final String value;

    IngredientUnit(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static IngredientUnit fromValue(String value) {
        for (IngredientUnit unit : IngredientUnit.values()) {
            if (unit.value.equalsIgnoreCase(value)) {
                return unit;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 단위입니다: " + value);
    }
}
