package com.ssa.lms.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 훈련생 화면의 "빈 화면 / 흰 화면" 재발 방지 테스트.
 *
 * <p>실제로 났던 사고 두 가지를 고정한다:
 * <ol>
 *   <li><b>Whitelabel Error Page</b> — 예전 정적 화면 경로({@code /templates/...})가 링크에
 *       남아 있어 누르면 404 가 났고, {@code /error} 매핑이 없어 흰 화면이 떴다.</li>
 *   <li><b>전부 비어 있는 화면</b> — 배정된 항목이 없으면 과제/시험/출결/이수/설문이
 *       모두 "없습니다" 만 나와 화면정의서 캡처에 쓸 수 없었다.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TraineeScreenFallbackRenderTest {

    @Autowired MockMvc mvc;

    private static final String[] TRAINEE_PAGES = {
            "/trainee/assignment", "/trainee/exam", "/trainee/attendance",
            "/trainee/completion-management", "/trainee/survey"
    };

    @Test
    @DisplayName("훈련생 화면 어디에도 옛 정적 경로(/templates/...) 링크가 남아 있지 않다")
    @WithUserDetails("trainee1")
    void noStaleStaticLinks() throws Exception {
        for (String page : TRAINEE_PAGES) {
            mvc.perform(get(page)).andExpect(status().isOk())
                    // 눌렀을 때 404 Whitelabel 로 가던 링크
                    .andExpect(content().string(not(containsString("href=\"/templates/"))))
                    // 응답이 잘리지 않았는지 (200 만으로는 부족 — CLAUDE.md 규칙 3)
                    .andExpect(content().string(containsString("</html>")));
        }
        mvc.perform(get("/trainee/alarm")).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("href=\"/templates/"))))
                .andExpect(content().string(containsString("</html>")));
    }

    @Test
    @DisplayName("배정된 항목이 없는 계정은 예시 데이터와 안내 배너를 본다")
    @WithUserDetails("admin")   // 시드 관리자에게는 수강/출결/이수 데이터가 없다
    void emptyAccountSeesSampleData() throws Exception {
        for (String page : TRAINEE_PAGES) {
            mvc.perform(get(page)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("화면 예시용 샘플 데이터")))
                    .andExpect(content().string(containsString("</html>")));
        }
        // 과제/시험/설문은 인라인 JS(JSON)로 내려간다 — Thymeleaf 가 한글을 \\uXXXX 로 이스케이프하므로
        // 한글로 단언하면 항상 실패한다. 이스케이프되지 않는 id 로 확인한다.
        mvc.perform(get("/trainee/assignment"))
                .andExpect(content().string(containsString("\"id\":\"900001\"")));
        mvc.perform(get("/trainee/exam"))
                .andExpect(content().string(containsString("\"id\":\"900101\"")));
        mvc.perform(get("/trainee/survey"))
                .andExpect(content().string(containsString("\"id\":\"900301\"")));
        // 출결/이수는 서버 렌더 테이블이라 한글 그대로 나온다.
        mvc.perform(get("/trainee/attendance"))
                .andExpect(content().string(containsString("오리엔테이션 및 개발환경 구축")));
        mvc.perform(get("/trainee/completion-management"))
                .andExpect(content().string(containsString("이수증 다운로드")));
    }

    @Test
    @DisplayName("본인 데이터가 있으면 예시가 아니라 실제 데이터가 나온다")
    @WithUserDetails("trainee1")
    void realDataWinsOverSample() throws Exception {
        mvc.perform(get("/trainee/assignment")).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("화면 예시용 샘플 데이터"))))
                .andExpect(content().string(not(containsString("\"id\":\"900001\""))));
    }

    @Test
    @DisplayName("오류 화면은 Whitelabel 대신 안내 페이지 — 스택트레이스를 노출하지 않는다")
    @WithUserDetails("trainee1")
    void errorPageReplacesWhitelabel() throws Exception {
        mvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr("jakarta.servlet.error.status_code", 404))
                .andExpect(content().string(containsString("페이지를 찾을 수 없습니다")))
                .andExpect(content().string(containsString("내 홈으로")))
                .andExpect(content().string(not(containsString("Whitelabel"))))
                .andExpect(content().string(not(containsString("org.springframework.web"))));
    }

    @Test
    @DisplayName("예시 항목은 제출·열람을 시도해도 500 이 아니라 안내로 되돌아온다")
    @WithUserDetails("trainee1")
    void sampleRowsAreGuarded() throws Exception {
        mvc.perform(get("/trainee/survey/900301"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/trainee/completion-management/900201/certificate"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("화면 예시용 데이터입니다")));
    }

    @Test
    @DisplayName("관리자는 훈련생 화면을 열람할 수 있지만 제출은 못 한다")
    @WithUserDetails("admin")
    void adminCanViewButNotSubmit() throws Exception {
        mvc.perform(get("/trainee/assignment")).andExpect(status().isOk());
        mvc.perform(get("/trainee/exam")).andExpect(status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/trainee/assignment/1/submit").with(
                                org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
    }
}
