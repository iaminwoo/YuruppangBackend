package com.ll.Yuruppang.domain.recipe;

import com.ll.Yuruppang.global.openFeign.youtube.YoutubeUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
@ActiveProfiles("test")
public class YoutubeTest {
    @Test
    @DisplayName("url 에서 id 추출")
    void getVideoId() {
        String url = "https://youtu.be/IpI6ST6C_Dk?feature=shared";
        String s = YoutubeUtils.extractVideoId(url);
        assertEquals("IpI6ST6C_Dk", s);
    }
}
