package com.ll.Yuruppang.global.openFeign.youtube;

import com.ll.Yuruppang.global.exceptions.ErrorCode;
import com.ll.Yuruppang.global.openFeign.youtube.dto.VideoListResponse;
import com.ll.Yuruppang.global.openFeign.youtube.dto.VideoSnippet;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class YoutubeUtils {
    public static String extractVideoId(String youtubeUrl) {
        try {
            URI uri = new URI(youtubeUrl);
            String host = uri.getHost();

            // youtu.be 단축 URL
            if (host != null && host.contains("youtu.be")) {
                String path = uri.getPath();
                if (path != null && path.length() > 1) {
                    String videoId = path.substring(1); // /VIDEO_ID
                    int qIndex = videoId.indexOf("?");
                    if (qIndex != -1) {
                        videoId = videoId.substring(0, qIndex);
                    }
                    return videoId;
                }
            }

            // youtube.com URL
            if (host != null && (host.contains("youtube.com"))) {
                Map<String, String> queryParams = splitQuery(uri.getQuery());
                return queryParams.getOrDefault("v", "");
            }

        } catch (URISyntaxException e) {
            throw ErrorCode.VIDEO_ID_NOT_FOUND.throwServiceException();
        }
        return "";
    }

    private static Map<String, String> splitQuery(String query) {
        Map<String, String> queryPairs = new HashMap<>();
        if (query == null || query.isEmpty()) return queryPairs;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            queryPairs.put(key, value);
        }
        return queryPairs;
    }

    public static String getVideoInfoText(VideoListResponse videoInfo) {
        VideoSnippet snippet = videoInfo.getItems().getFirst().getSnippet();

        String channelTitle = snippet.getChannelTitle();
        String title = snippet.getTitle();
        String description = snippet.getDescription();

        return """
            {
                "channelTitle": "%s",
                "title": "%s",
                "description": "%s"
            }
            """.formatted(channelTitle, title, description);
    }
}
