package com.ll.Yuruppang.domain.recipe.contorller;

import com.ll.Yuruppang.domain.recipe.dto.pan.PanRequest;
import com.ll.Yuruppang.domain.recipe.dto.pan.PanResponse;
import com.ll.Yuruppang.domain.recipe.entity.PanType;
import com.ll.Yuruppang.domain.recipe.service.PanService;
import com.ll.Yuruppang.global.response.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pans")
@RequiredArgsConstructor
public class PanController {
    private final PanService panService;

    @PostMapping
    public RsData<PanResponse> createPan(@Valid @RequestBody PanRequest request) {
        if(request.panType().equals(PanType.ROUND)) {
            return RsData.success(HttpStatus.OK, panService.createRoundPan(
                    request.radius(), request.height()
            ));
        } else if (request.panType().equals(PanType.SQUARE)) {
            return RsData.success(HttpStatus.OK, panService.createSquarePan(
                    request.width(), request.length(), request.height()
            ));
        } else {
            return RsData.success(HttpStatus.OK, panService.createCustomPan(
                    request.volume()
            ));
        }
    }

    @GetMapping
    public RsData<List<PanResponse>> getPans(@RequestParam(required = false) PanType panType) {
        if (panType == null) {
            return RsData.success(HttpStatus.OK, panService.getAllPans());
        }
        return RsData.success(HttpStatus.OK, panService.getPansByPanType(panType));
    }

    @GetMapping("/{panId}")
    public RsData<PanResponse> getPanDetail(@PathVariable Long panId) {
        return RsData.success(HttpStatus.OK, panService.getPanDetail(panId));
    }
}
