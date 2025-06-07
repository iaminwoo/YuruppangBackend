package com.ll.Yuruppang.domain.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogType type;

    @Column(nullable = false)
    private BigDecimal quantity;

    private BigDecimal totalPrice;

    private LocalDate actualAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public void update(LogType type, String description, Ingredient ingredient,
                       BigDecimal quantity, BigDecimal price, LocalDate actualAt) {
        this.type = type;
        this.description = description;
        this.ingredient = ingredient;
        this.quantity = quantity;
        this.totalPrice = price;
        this.actualAt = actualAt;
    }
}