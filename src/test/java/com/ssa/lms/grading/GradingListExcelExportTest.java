package com.ssa.lms.grading;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 시험 채점 목록 xlsx 다운로드 검증 — local 시더가 채점 대상 시험을 심어 둔다.
 *
 * <p>컨트롤러가 {@code @AuthenticationPrincipal LoginUser} 를 쓰므로 시드 계정으로 인증한다
 * ({@code @WithMockUser} 로는 principal 이 LoginUser 가 아니라 NPE 가 난다).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class GradingListExcelExportTest {

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String URL = "/admin/evaluation/grading/export.xlsx";

    @Autowired MockMvc mvc;

    @Test
    @DisplayName("관리자 시험 채점 목록 엑셀 다운로드는 200 + xlsx 첨부이고 본문이 실제 xlsx(PK) 다")
    @WithUserDetails("admin")
    void adminDownloadsGradingListExcel() throws Exception {
        MvcResult result = mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX_MIME))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andReturn();

        byte[] body = result.getResponse().getContentAsByteArray();
        assertThat(body).isNotEmpty();
        // xlsx 는 zip 컨테이너 — 시그니처가 'P','K' 여야 엑셀이 연다.
        assertThat(body[0]).isEqualTo((byte) 'P');
        assertThat(body[1]).isEqualTo((byte) 'K');
    }

    @Test
    @DisplayName("조건에 맞는 시험이 없어도 200(안내 행 포함 빈 표) — 다운로드가 깨진 것과 구분")
    @WithUserDetails("admin")
    void emptyFilterStillDownloads() throws Exception {
        mvc.perform(get(URL).param("keyword", "존재하지-않는-검색어-xlsx"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX_MIME));
    }

    @Test
    @DisplayName("강사도 목록 엑셀을 내려받을 수 있다 (담당 과정만 — 접근 자체는 200)")
    @WithUserDetails("instructor1")
    void instructorCanDownload() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX_MIME));
    }

    @Test
    @DisplayName("시험 채점 목록 화면에 엑셀 다운로드 버튼이 렌더된다 (200 이어도 잘린 HTML 방지 — </html> 확인)")
    @WithUserDetails("admin")
    void listPageRendersDownloadButton() throws Exception {
        mvc.perform(get("/admin/evaluation/grading"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("엑셀로 다운로드")))
                .andExpect(content().string(containsString("/admin/evaluation/grading/export.xlsx")))
                .andExpect(content().string(containsString("</html>")));
    }

    @Test
    @DisplayName("훈련생은 시험 채점 목록 엑셀을 내려받을 수 없다")
    @WithUserDetails("trainee1")
    void traineeCannotDownload() throws Exception {
        int status = mvc.perform(get(URL)).andReturn().getResponse().getStatus();
        // SecurityConfig 가 /admin/** 을 막는다 — 403 또는 로그인/홈으로 3xx 리다이렉트 둘 다 차단이다.
        assertThat(status).isIn(403, 302);
    }
}
