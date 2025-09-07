package com.ll.Yuruppang.domain.recipe.service;

import com.ll.Yuruppang.domain.recipe.dto.pan.PanResponse;
import com.ll.Yuruppang.domain.recipe.entity.Pan;
import com.ll.Yuruppang.domain.recipe.entity.PanType;
import com.ll.Yuruppang.domain.recipe.repository.PanRepository;
import com.ll.Yuruppang.global.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PanService {
    private final PanRepository panRepository;

    public Optional<Pan> findByIdOptional(Long panId) {
        return panRepository.findById(panId);
    }

    public Pan findById(Long panId) {
        return panRepository.findById(panId)
                .orElseThrow(ErrorCode.PAN_NOT_FOUND::throwServiceException);
    }

    @Transactional
    public PanResponse createRoundPan(BigDecimal radius, BigDecimal height) {
        BigDecimal volume = radius.pow(2).multiply(height).multiply(BigDecimal.valueOf(Math.PI));
        volume = volume.setScale(2, RoundingMode.HALF_UP);

        String measurements = "반지름: " + radius + "cm / 높이: " + height + "cm";

        return createPan(PanType.ROUND, measurements, volume);
    }
    @Transactional
    public PanResponse createSquarePan(BigDecimal width, BigDecimal length, BigDecimal height) {
        BigDecimal volume = width.multiply(length).multiply(height);
        volume = volume.setScale(2, RoundingMode.HALF_UP);

        String measurements = "가로: " + width + "cm / 세로: " + length +  "cm / 높이: " + height + "cm";

        return createPan(PanType.SQUARE, measurements, volume);
    }
    @Transactional
    public PanResponse createCustomPan(BigDecimal volume) {
        return createPan(PanType.CUSTOM, "", volume);
    }

    private PanResponse createPan(PanType panType, String measurements, BigDecimal volume) {
        Pan newPan = panRepository.save(Pan.builder()
                .panType(panType)
                .measurements(measurements)
                .volume(volume)
                .build());
        return new PanResponse(newPan.getId(), newPan.getPanType(), newPan.getMeasurements(), newPan.getVolume());
    }

    @Transactional
    public List<PanResponse> getPansByPanType(PanType panType) {
        return panRepository.findAllByPanType(panType).stream()
                .map(pan -> new PanResponse(
                        pan.getId(), pan.getPanType(), pan.getMeasurements(), pan.getVolume()
                )).toList();
    }

    @Transactional
    public PanResponse getPanDetail(Long panId) {
        Pan pan = findByIdOptional(panId).orElseThrow(ErrorCode.PAN_NOT_FOUND::throwServiceException);
        return makePanResponse(pan);
    }

    public PanResponse makePanResponse(Pan pan) {
        if(pan != null) {
            return new PanResponse(
                    pan.getId(), pan.getPanType(), pan.getMeasurements(), pan.getVolume()
            );
        } else {
            return new PanResponse(0L, PanType.ROUND, "", BigDecimal.ZERO);
        }
    }

    @Transactional
    public List<PanResponse> getAllPans() {
        return panRepository.findAll().stream()
                .map(pan -> new PanResponse(
                        pan.getId(), pan.getPanType(), pan.getMeasurements(), pan.getVolume()
                )).toList();
    }

}
