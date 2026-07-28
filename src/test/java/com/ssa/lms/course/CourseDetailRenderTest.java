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
 * 과정 상세 화면이 끝까지 렌더되는지 검증.
 *
 * <p>2026-07-28 실기동에서 /admin/courses/1 응답이 차시 목록 렌더 중 200인 채로 잘려
 * (커밋 전 스트리밍이라 상태코드 변경 불가) 브라우저가 빈 화면을 그리는 문제가 있었다.
 * 잘린 응답은 status 200 이라 status 검사만으로는 못 잡는다 — 반드시 닫는 태그까지 확인.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class CourseDetailRenderTest {

    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void 시드_과정_상세가_끝까지_렌더된다() throws Exception {
        String html = mvc.perform(get("/admin/courses/1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("차시 목록");
        assertThat(html).contains("</html>");
    }
}
