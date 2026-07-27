package com.ssa.lms.content;

import com.ssa.lms.content.service.ProgressQueryService;
import com.ssa.lms.content.service.ProgressQueryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 부팅 스모크 + 계약 검증 — 컨텍스트가 뜨고, P2-A 실제 구현체가 base fallback 을 대체했는지 확인.
 */
@SpringBootTest
@ActiveProfiles("local")
class ContentContextTest {

    @Autowired
    ProgressQueryService progressQueryService;

    @Test
    @DisplayName("ProgressQueryService 실제 구현체가 base fallback 을 대체한다")
    void realImplReplacesFallback() {
        assertThat(progressQueryService).isInstanceOf(ProgressQueryServiceImpl.class);
    }
}
