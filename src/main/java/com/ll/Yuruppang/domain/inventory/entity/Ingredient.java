package com.ll.Yuruppang.domain.inventory.entity;

import com.ll.Yuruppang.domain.recipe.entity.RecipePartIngredient;
import com.ll.Yuruppang.global.exceptions.ErrorCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // 예: 밀가루

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IngredientUnit unit;

    @Column(nullable = false)
    private BigDecimal unitPrice; // 단위당 가격

    @Column(nullable = false)
    private BigDecimal totalStock; // 현재 남은 수량

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "ingredient", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RecipePartIngredient> partIngredients = new HashSet<>();

    public void addTotalQuantity(BigDecimal quantity) {
        if (quantity == null) throw ErrorCode.ILLEGAL_INGREDIENT_QUANTITY.throwServiceException();
        this.totalStock = this.totalStock.add(quantity);
    }

    public void changeUnitPrice(BigDecimal addedTotalPrice, BigDecimal addedQuantity) {
        if (addedQuantity == null) {
            throw ErrorCode.ILLEGAL_INGREDIENT_QUANTITY.throwServiceException();
        }
        BigDecimal currentTotal = this.unitPrice.multiply(this.totalStock);
        BigDecimal newTotalQuantity;
        if(this.totalStock.compareTo(BigDecimal.ZERO) <= 0) {
            newTotalQuantity = addedQuantity;
        } else {
            newTotalQuantity = this.totalStock.add(addedQuantity);
        }
        BigDecimal newTotalPrice = currentTotal.add(addedTotalPrice);

        this.unitPrice = newTotalPrice.divide(newTotalQuantity, 2, RoundingMode.HALF_UP);
    }

    public void subtractUnitPrice(BigDecimal removedPrice, BigDecimal removedQuantity) {
        if (removedQuantity == null || removedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw ErrorCode.ILLEGAL_INGREDIENT_QUANTITY.throwServiceException();
        }

        BigDecimal currentTotal = this.unitPrice.multiply(this.totalStock);
        BigDecimal newTotalQuantity = this.totalStock.subtract(removedQuantity);
        BigDecimal newTotalPrice = currentTotal.subtract(removedPrice);

        if(newTotalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            this.unitPrice = BigDecimal.ZERO;
        } else {
            this.unitPrice = newTotalPrice.divide(newTotalQuantity, 2, RoundingMode.HALF_UP);
        }
    }

    public void addPartIngredient(RecipePartIngredient partIngredient) {
        this.getPartIngredients().add(partIngredient);
        partIngredient.setIngredient(this);
    }
}