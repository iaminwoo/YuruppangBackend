package com.ll.Yuruppang.domain.inventory.contorller;

import com.ll.Yuruppang.domain.inventory.entity.dto.request.IngredientPurchaseRequest;
import com.ll.Yuruppang.domain.inventory.entity.dto.request.IngredientUseListRequest;
import com.ll.Yuruppang.domain.inventory.entity.dto.request.LogModifyRequest;
import com.ll.Yuruppang.domain.inventory.entity.dto.response.LogGetResponse;
import com.ll.Yuruppang.domain.inventory.service.IngredientService;
import com.ll.Yuruppang.domain.inventory.service.LogService;
import com.ll.Yuruppang.global.response.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ingredientLogs")
@RequiredArgsConstructor
public class IngredientLogController {

    private final IngredientService ingredientService;
    private final LogService logService;

    // 재료 구매 기록
    @PostMapping("/purchase")
    public RsData<String> purchaseIngredient(@Valid @RequestBody IngredientPurchaseRequest request) {
        ingredientService.purchaseIngredient(
                request.description(), request.requestList(), request.actualAt()
        );
        return RsData.success(HttpStatus.OK, "구매가 성공적으로 등록 되었습니다.");
    }

    // 재료 소비 기록
    @PostMapping("/use")
    public RsData<String> useIngredient(@Valid @RequestBody IngredientUseListRequest request) {
        ingredientService.useIngredient(
                request.description(), request.requestList(), request.actualAt()
        );
        return RsData.success(HttpStatus.OK, "소비가 성공적으로 등록 되었습니다.");
    }

    // 기록 단건 조회
    @GetMapping("/{logId}")
    public RsData<LogGetResponse> getLogDetail(@PathVariable Long logId) {
        return RsData.success(HttpStatus.OK, logService.getLogDetail(logId));
    }

    // 기록 전체 조회 (페이징)
    @GetMapping
    public RsData<Page<LogGetResponse>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return RsData.success(HttpStatus.OK, logService.getLogs(pageable));
    }

    // 재료 기록 수정
    @PutMapping("/{logId}")
    public RsData<LogGetResponse> modifyLog(@PathVariable Long logId, @Valid @RequestBody LogModifyRequest request){
        return RsData.success(HttpStatus.OK,logService.modifyLog(
                logId, request.type(), request.description(),
                request.ingredientName(), request.quantity(), request.price(), request.actualAt()
                ));
    }

    // 재료 기록 삭제
    @DeleteMapping("/{logId}")
    public RsData<String> deleteLog(@PathVariable Long logId){
        logService.deleteLog(logId);
        return RsData.success(HttpStatus.OK, "성공적으로 삭제되었습니다.");
    }

}
