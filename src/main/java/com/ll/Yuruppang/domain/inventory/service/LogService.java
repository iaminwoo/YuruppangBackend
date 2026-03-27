package com.ll.Yuruppang.domain.inventory.service;

import com.ll.Yuruppang.domain.inventory.entity.Ingredient;
import com.ll.Yuruppang.domain.inventory.entity.IngredientLog;
import com.ll.Yuruppang.domain.inventory.entity.LogType;
import com.ll.Yuruppang.domain.inventory.entity.dto.response.LogGetResponse;
import com.ll.Yuruppang.domain.inventory.repository.LogRepository;
import com.ll.Yuruppang.global.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class LogService {
    private final LogRepository logRepository;

    @Transactional(readOnly = true)
    public IngredientLog findById(Long id) {
        return logRepository.findById(id).orElseThrow(ErrorCode.INGREDIENT_LOG_NOT_FOUND::throwServiceException);
    }

    @Transactional
    public void createLog(LogType type, Ingredient ingredient, String description, LocalDate actualAt, BigDecimal totalPrice, BigDecimal totalQuantity) {
        IngredientLog log = IngredientLog.builder()
                .type(type)
                .description(description)
                .ingredient(ingredient)
                .actualAt(actualAt)
                .totalPrice(totalPrice)
                .quantity(totalQuantity)
                .build();
        logRepository.save(log);
    }

    @Transactional(readOnly = true)
    public LogGetResponse getLogDetail(Long logId) {
        IngredientLog log = logRepository.findById(logId)
                .orElseThrow(ErrorCode.INGREDIENT_LOG_NOT_FOUND::throwServiceException);

        return makeGetResponse(log);
    }

    private LogGetResponse makeGetResponse(IngredientLog log) {
        Ingredient ingredient = log.getIngredient();
        return new LogGetResponse(
                log.getId(),
                log.getActualAt(), log.getType(), log.getDescription(),
                ingredient.getId(), ingredient.getName(),
                log.getQuantity(), ingredient.getUnit().getValue(), log.getTotalPrice()
        );
    }

    @Transactional(readOnly = true)
    public Page<LogGetResponse> getLogs(Pageable pageable) {
        return logRepository.findAll(pageable)
                .map(this::makeGetResponse);
    }

    public LogGetResponse modifyLog(IngredientLog log, LogType newType, String description, Ingredient newIngredient, BigDecimal newQuantity, BigDecimal newPrice, LocalDate actualAt) {
        log.update(newType, description, newIngredient, newQuantity, newPrice, actualAt);
        return makeGetResponse(log);
    }

    public boolean existsByIngredient(Ingredient ingredient) {
        return logRepository.existsByIngredient(ingredient);
    }

    public void delete(IngredientLog log) {
        logRepository.delete(log);
    }
}
