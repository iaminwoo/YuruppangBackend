package com.ll.Yuruppang.global.openFeign.youtube.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VideoListResponse {
    private List<VideoItem> items;
}

