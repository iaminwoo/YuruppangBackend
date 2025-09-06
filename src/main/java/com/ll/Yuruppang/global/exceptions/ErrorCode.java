package com.ll.Yuruppang.global.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // Inventory
    INGREDIENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "재료가 이미 등록되어 있습니다."),
    INGREDIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "재료를 찾을 수 없습니다."),
    INGREDIENT_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "기록을 찾을 수 없습니다."),
    ILLEGAL_INGREDIENT_QUANTITY(HttpStatus.BAD_REQUEST, "입력된 수량에 문제가 있습니다."),
    STOCK_NOT_ENOUGH(HttpStatus.CONFLICT, "재료가 충분하지 않습니다."),

    // Log
    LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "로그를 찾을 수 없습니다."),

    // Recipe
    RECIPE_NOT_FOUND(HttpStatus.NOT_FOUND, "레시피를 찾을 수 없습니다."),
    RECIPE_NOT_TEMP(HttpStatus.BAD_REQUEST, "임시 레시피가 아닙니다."),
    TEMP_RECIPE_NOT_REGISTERED(HttpStatus.BAD_REQUEST, "임시 레시피가 등록되지 않았습니다."),
    PLACEHOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "플레이스홀더 레시피를 찾을 수 없습니다."),
    PAN_NOT_FOUND(HttpStatus.NOT_FOUND, "틀을 찾을 수 없습니다."),

    // Category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    CATEGORY_ALREADY_EXIST(HttpStatus.CONFLICT, "이미 생성된 카테고리입니다."),

    // Plan
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "오늘의 유루빵을 찾을 수 없습니다."),
    RECIPE_NOT_FOUND_IN_PLAN(HttpStatus.NOT_FOUND, "해당 레시피는 포함되어 있지 않습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public ServiceException throwServiceException() {
        throw new ServiceException(httpStatus, message);
    }

    public ServiceException throwServiceException(Throwable cause) {
        throw new ServiceException(httpStatus, message, cause);
    }
}