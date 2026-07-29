package com.ssa.lms.web.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 화면(프론트엔드)이 끝까지 렌더되는지 고정한다.
 *
 * <p><b>status 200 만으로는 부족하다.</b> Thymeleaf 가 렌더 도중 예외를 만나면 응답 헤더는
 * 이미 나간 뒤라 200 인 채로 HTML 이 잘린다. 그래서 {@code </html>} 까지 왔는지 확인한다
 * (CLAUDE.md 규칙 3).</p>
 *
 * <p>대시보드 3종은 컨트롤러가 {@code @AuthenticationPrincipal LoginUser} 를 받으므로
 * {@code @WithMockUser} 로는 principal 이 null 이 된다 — 시드 계정을 쓰는
 * {@code @WithUserDetails} 가 필요하다.</p>
 *
 * <p>지금 이 화면들은 서버 데이터 없이 각자 JS 더미로 그리는 단계다. 그래도 fragment 호출
 * (사이드바·헤더)과 active 키가 틀리면 여기서 잡힌다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class AiScreenRenderTest {

    @Autowired MockMvc mvc;

    private void assertFullyRendered(String url, String mustContain) throws Exception {
        MvcResult res = mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String html = res.getResponse().getContentAsString();

        assertThat(html)
                .as("%s 렌더가 도중에 끊겼다 — 200 이어도 HTML 이 잘리면 화면이 깨진다", url)
                .contains("</html>");
        assertThat(html)
                .as("%s 에 화면 고유 요소가 없다", url)
                .contains(mustContain);
    }

    @Test
    @WithMockUser(username = "trainee1", roles = "TRAINEE")
    @DisplayName("훈련생 AI 화면 3종이 끝까지 렌더된다")
    void 훈련생_AI화면() throws Exception {
        assertFullyRendered("/trainee/ai/qna", "chatLog");
        assertFullyRendered("/trainee/ai/curriculum", "recommendList");
        assertFullyRendered("/trainee/ai/roadmap", "roadmapList");
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    @DisplayName("강사 AI 학습진단 화면이 끝까지 렌더된다")
    void 강사_AI진단화면() throws Exception {
        assertFullyRendered("/instructor/ai/diagnosis", "diagBody");
    }

    @Test
    @WithUserDetails("trainee1")
    @DisplayName("훈련생 대시보드에 AI 위젯 자리가 있다")
    void 훈련생_대시보드_위젯() throws Exception {
        assertFullyRendered("/trainee", "hpAiCards");
    }

    @Test
    @WithUserDetails("instructor1")
    @DisplayName("강사 대시보드에 AI 위젯 자리가 있다")
    void 강사_대시보드_위젯() throws Exception {
        assertFullyRendered("/instructor", "instAiCards");
    }

    @Test
    @WithUserDetails("admin")
    @DisplayName("관리자 대시보드에 AI 위젯 자리가 있다")
    void 관리자_대시보드_위젯() throws Exception {
        assertFullyRendered("/admin", "adminAiCards");
    }

    @Test
    @WithMockUser(username = "trainee1", roles = "TRAINEE")
    @DisplayName("훈련생은 강사 AI 진단 화면에 들어갈 수 없다")
    void 권한_경계() throws Exception {
        mvc.perform(get("/instructor/ai/diagnosis")).andExpect(status().isForbidden());
    }
}
