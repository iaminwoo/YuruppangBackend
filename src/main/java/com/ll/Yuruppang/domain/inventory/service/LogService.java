package com.ll.Yuruppang.domain.inventory.service;

import com.ll.Yuruppang.domain.inventory.entity.Ingredient;
import com.ll.Yuruppang.domain.inventory.entity.IngredientLog;
import com.ll.Yuruppang.domain.inventory.entity.LogType;
import com.ll.Yuruppang.domain.inventory.entity.dto.response.LogGetResponse;
import com.ll.Yuruppang.domain.inventory.repository.IngredientRepository;
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
    private final IngredientRepository ingredientRepository;

    @Transactional(readOnly = true)
    public LogGetResponse getLogDetail(Long logId) {
        IngredientLog log = logRepository.findById(logId)
                .orElseThrow(ErrorCode.INGREDIENT_LOG_NOT_FOUND::throwServiceException);

        return makeGetResponse(log);
    }
    @Transactional(readOnly = true)
    public Page<LogGetResponse> getLogs(Pageable pageable) {
        return logRepository.findAll(pageable)
                .map(this::makeGetResponse);
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

    @Transactional
    public LogGetResponse modifyLog(Long logId, LogType newType, String description,
                                    String ingredientName, BigDecimal newQuantity,
                                    BigDecimal newPrice, LocalDate actualAt) {
        Ingredient newIngredient = ingredientRepository.findByName(ingredientName)
                .orElseThrow(ErrorCode.INGREDIENT_NOT_FOUND::throwServiceException);

        IngredientLog log = rollbackLog(logId);

        if (newType == LogType.PURCHASE) {
            newIngredient.changeUnitPrice(newPrice, newQuantity);
            newIngredient.addTotalQuantity(newQuantity);
        } else {
            newIngredient.addTotalQuantity(newQuantity.negate());
        }

        log.update(newType, description, newIngredient, newQuantity, newPrice, actualAt);
        return makeGetResponse(log);
    }

    private IngredientLog rollbackLog(long logId) {
        IngredientLog log = logRepository.findById(logId)
                .orElseThrow(ErrorCode.INGREDIENT_LOG_NOT_FOUND::throwServiceException);
        Ingredient oldIngredient = log.getIngredient();
        LogType oldType = log.getType();
        BigDecimal oldQuantity = log.getQuantity();
        BigDecimal oldPrice = log.getTotalPrice();

        if (oldType == LogType.PURCHASE) {
            oldIngredient.subtractUnitPrice(oldPrice, oldQuantity);
            oldIngredient.addTotalQuantity(oldQuantity.negate());
        } else {
            oldIngredient.addTotalQuantity(oldQuantity);
        }

        return log;
    }

    @Transactional
    public void deleteLog(Long logId) {
        IngredientLog log = rollbackLog(logId);
        logRepository.delete(log);
    }
}
