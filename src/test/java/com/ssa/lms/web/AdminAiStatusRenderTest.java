package com.ssa.lms.web;

import com.ssa.lms.ai.client.AiFailReason;
import com.ssa.lms.ai.dto.AiStatusView;
import com.ssa.lms.ai.service.AiStatusService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [관리자] AI 상태 배너가 실제로 그려지는지 고정한다.
 *
 * <p><b>서비스 단위 테스트만으로는 부족하다.</b> 판정이 맞아도 Thymeleaf 표현식이 하나
 * 틀리면 렌더 도중 예외가 나는데, 응답 헤더는 이미 나간 뒤라 <b>200 인 채로 HTML 이
 * 잘린다</b> (CLAUDE.md 규칙 3). 그래서 {@code </html>} 까지 왔는지 함께 본다.</p>
 *
 * <p>로컬 프로필은 AI 가 꺼져 있어 <b>경고 배너 경로가 한 번도 안 그려진다.</b>
 * 정작 필요한 순간(크레딧 소진)에 처음 렌더되면 그때 깨진 걸 알게 되므로,
 * 상태를 주입해 두 경로를 모두 지나가게 한다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminAiStatusRenderTest {

    @Autowired MockMvc mvc;

    @MockitoBean AiStatusService aiStatusService;

    private String renderAdminHome() throws Exception {
        MvcResult res = mvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andReturn();
        String html = res.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(html)
                .as("관리자 대시보드 렌더가 도중에 끊겼다 — 200 이어도 HTML 이 잘리면 화면이 깨진다")
                .contains("</html>");
        return html;
    }

    @Test
    @WithUserDetails("admin")
    @DisplayName("크레딧이 소진되면 배너와 콘솔 링크가 뜬다")
    void creditExhausted_rendersBannerWithConsoleLink() throws Exception {
        when(aiStatusService.current()).thenReturn(
                AiStatusView.failed(AiFailReason.CREDIT_EXHAUSTED, LocalDateTime.now(), 200));

        String html = renderAdminHome();

        assertThat(html).contains("ai-status");            // 배너가 실제로 그려졌다
        assertThat(html).contains("크레딧");
        // 관리자가 바로 갈 곳이 없으면 배너는 그냥 나쁜 소식일 뿐이다
        assertThat(html).contains("console.anthropic.com/settings/billing");
        assertThat(html).contains("조치 필요");             // 헤더 배지
    }

    @Test
    @WithUserDetails("admin")
    @DisplayName("정상일 때는 배너를 그리지 않는다 — 늘 떠 있으면 아무도 안 본다")
    void healthy_rendersBadgeOnly() throws Exception {
        when(aiStatusService.current()).thenReturn(AiStatusView.healthy(LocalDateTime.now()));

        String html = renderAdminHome();

        assertThat(html).doesNotContain("ai-status");
        assertThat(html).contains("정상");
    }

    @Test
    @WithUserDetails("admin")
    @DisplayName("꺼져 있을 때도 화면은 멀쩡히 끝까지 뜬다")
    void disabled_stillRendersFully() throws Exception {
        when(aiStatusService.current()).thenReturn(AiStatusView.disabled());

        String html = renderAdminHome();

        // 설정으로 꺼둔 것은 장애가 아니다 — 경고 배너를 띄우지 않는다
        assertThat(html).doesNotContain("ai-status");
        assertThat(html).contains("꺼짐");
    }

    @Test
    @WithUserDetails("admin")
    @DisplayName("호출 이력이 없어도 렌더가 깨지지 않는다 — 시각이 null 인 경로")
    void neverCalled_handlesNullTimestamp() throws Exception {
        // at 이 null 인 상태를 안 지나가면, 배포 직후 첫 화면에서 처음 터진다
        when(aiStatusService.current()).thenReturn(AiStatusView.noKey());

        String html = renderAdminHome();

        assertThat(html).contains("ai-status");
        assertThat(html).contains("API 키");
        assertThat(html).doesNotContain("마지막 호출");   // 시각이 없으면 표기하지 않는다
    }
}
