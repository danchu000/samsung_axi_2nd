package com.ssa.lms.course;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 수강신청 승인 화면(적합성 판정 포함)이 끝까지 렌더되는지 검증.
 *
 * <p>Thymeleaf 렌더 도중 예외가 나도 응답은 이미 200 이라 잘린 HTML 이 내려간다 (CLAUDE.md 규칙 3).
 * 반드시 닫는 태그까지 확인한다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class EnrollmentApprovalRenderTest {

    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void 수강신청_승인_화면이_적합도까지_렌더된다() throws Exception {
        String html = mvc.perform(get("/admin/enrollments/pending"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 4단계 배지가 모두 나온다
        assertThat(html).contains("아주 적합", "적합", "미적합", "부적합");
        // 편람 항목이 상세 패널에 렌더된다
        assertThat(html).contains("훈련생 선발 서류전형 편람", "훈련생 선발 면접전형 편람");
        assertThat(html).contains("경력 및 자격증", "훈련의욕 및 직종에 대한 이해도");
        // 실제 승인 대기 영역이 남아 있다
        assertThat(html).contains("승인 대기 신청");
        assertThat(html).contains("</html>");
    }
}
