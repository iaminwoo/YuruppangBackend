package com.ll.Yuruppang.global.openFeign.youtube;

import com.ll.Yuruppang.global.openFeign.youtube.dto.VideoListResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "youtubeApiClient", url = "https://www.googleapis.com/youtube/v3/videos?part=snippet")
public interface YoutubeApiClient {

    @GetMapping(consumes = "application/json")
    VideoListResponse getVideoInfo(
            @RequestParam("id") String id,
            @RequestParam("key") String apiKey
    );
}
