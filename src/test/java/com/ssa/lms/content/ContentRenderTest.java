package com.ssa.lms.content;

import com.ssa.lms.content.entity.Content;
import com.ssa.lms.content.repository.ContentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 콘텐츠/진도 화면이 실제로 Thymeleaf 렌더링되는지 검증 (뷰 파싱 오류 조기 탐지).
 * 컨트롤러가 {@code @AuthenticationPrincipal LoginUser} 를 쓰므로 시드 계정으로 인증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ContentRenderTest {

    @Autowired MockMvc mvc;
    @Autowired ContentRepository contentRepository;

    @Test
    @DisplayName("강사 콘텐츠 목록/등록 폼 렌더링 — 강사 레이아웃")
    @WithUserDetails("instructor1")
    void instructorContentPages() throws Exception {
        mvc.perform(get("/instructor/contents")).andExpect(status().isOk())
                .andExpect(content().string(containsString("AI 학습진단")))   // 강사 사이드바 항목
                .andExpect(content().string(not(containsString("이탈 예측")))) // 관리자 사이드바 항목 없음
                .andExpect(content().string(containsString("</html>")));
        mvc.perform(get("/instructor/contents/new").param("type", "VIDEO")).andExpect(status().isOk());
        mvc.perform(get("/instructor/contents/new").param("type", "DOCUMENT")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 접근 시 콘텐츠 화면이 관리자 레이아웃으로 렌더링")
    @WithUserDetails("admin")
    void adminContentPagesUseAdminLayout() throws Exception {
        mvc.perform(get("/instructor/contents")).andExpect(status().isOk())
                .andExpect(content().string(containsString("이탈 예측")))          // 관리자 사이드바 항목
                .andExpect(content().string(not(containsString("AI 학습진단"))))    // 강사 사이드바 항목 없음
                .andExpect(content().string(containsString("data-user-role=\"admin\"")))
                .andExpect(content().string(containsString("</html>")));
        mvc.perform(get("/instructor/contents/new").param("type", "VIDEO")).andExpect(status().isOk())
                .andExpect(content().string(containsString("이탈 예측")))
                .andExpect(content().string(containsString("</html>")));
        mvc.perform(get("/instructor/contents/new").param("type", "DOCUMENT")).andExpect(status().isOk())
                .andExpect(content().string(containsString("이탈 예측")))
                .andExpect(content().string(containsString("</html>")));
    }

    @Test
    @DisplayName("강사 콘텐츠 수정 폼 렌더링 (시드 콘텐츠)")
    @WithUserDetails("instructor1")
    void instructorEditPage() throws Exception {
        List<Content> all = contentRepository.findAll();
        if (!all.isEmpty()) {
            mvc.perform(get("/instructor/contents/{id}/edit", all.get(0).getId()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("훈련생 학습 콘텐츠 목록/차시별 학습/시청 화면 렌더링")
    @WithUserDetails("trainee1")
    void traineeLearningPages() throws Exception {
        mvc.perform(get("/trainee/contents")).andExpect(status().isOk());
        mvc.perform(get("/trainee/learning")).andExpect(status().isOk());

        List<Content> all = contentRepository.findAll();
        for (Content c : all) {
            // VOD 와 문서 재생 화면 모두 파싱되는지
            mvc.perform(get("/trainee/contents/{id}/play", c.getId())).andExpect(status().isOk());
        }
    }
}
