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
    private final IngredientService ingredientService;
    private final LogRepository logRepository;

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
        Ingredient newIngredient = ingredientService.findIngredientByName(ingredientName);

        IngredientLog log = logRepository.findById(logId)
                .orElseThrow(ErrorCode.INGREDIENT_LOG_NOT_FOUND::throwServiceException);
        ingredientService.applyLogEffect(log.getIngredient(), log.getType(), log.getQuantity(), log.getTotalPrice(), true);

        ingredientService.applyLogEffect(newIngredient, newType, newQuantity, newPrice, false);

        log.update(newType, description, newIngredient, newQuantity, newPrice, actualAt);
        return makeGetResponse(log);
    }

    @Transactional
    public void deleteLog(Long logId) {
        IngredientLog log = logRepository.findById(logId)
                .orElseThrow(ErrorCode.INGREDIENT_LOG_NOT_FOUND::throwServiceException);
        ingredientService.applyLogEffect(log.getIngredient(), log.getType(), log.getQuantity(), log.getTotalPrice(), true);

        logRepository.delete(log);
    }
}
