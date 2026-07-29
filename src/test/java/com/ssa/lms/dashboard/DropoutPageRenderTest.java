package com.ssa.lms.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 이탈 예측 대시보드 렌더링 검증.
 *
 * <p>status 200 만으로는 부족하다 — Thymeleaf 가 렌더 도중 죽어도 응답은 이미 200 이라
 * HTML 이 조용히 잘린다. {@code </html>} 포함까지 확인한다 (CLAUDE.md 규칙 3).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class DropoutPageRenderTest {

    @Autowired MockMvc mvc;

    @Test
    @WithUserDetails("admin")
    @DisplayName("관리자 이탈 예측 화면 — 표본 데이터로 끝까지 렌더링된다")
    void 관리자_이탈예측_렌더링() throws Exception {
        String html = mvc.perform(get("/admin/analytics/dropout"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("prediction"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("</html>");
        assertThat(html).contains("학습자 이탈 예측");
        // 모델 연동 전에는 표본임이 화면에 명시돼야 한다 — 표본을 실수치로 오해하면 안 된다
        assertThat(html).contains("표본");
        // 위험 학습자 표가 실제로 채워졌는지 (표본 1행)
        assertThat(html).contains("고위험");
    }

    @Test
    @WithUserDetails("instructor1")
    @DisplayName("강사는 /admin/** 이라 접근할 수 없다")
    void 강사_접근불가() throws Exception {
        mvc.perform(get("/admin/analytics/dropout"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("trainee1")
    @DisplayName("훈련생도 접근할 수 없다")
    void 훈련생_접근불가() throws Exception {
        mvc.perform(get("/admin/analytics/dropout"))
                .andExpect(status().isForbidden());
    }
}
