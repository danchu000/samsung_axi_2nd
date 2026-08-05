package com.ssa.lms.legal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 이용약관 / 개인정보처리방침이 <b>대외 공개 문서로서</b> 성립하는지 고정한다.
 *
 * <p><b>배경:</b> 두 문서는 SecurityConfig 에서 permitAll 이고, 회원가입 필수 동의의
 * "전문 보기" 링크라 로그인 없이 누구나 본다. 개인정보처리방침은 법정 공개 의무 문서다.
 * 그런데 한동안 {@code [기관명]}·{@code [성명]}·{@code YYYY-MM-DD} 같은 자리표시자와
 * 개발용 주석이 운영에 그대로 노출되고 있었다.</p>
 *
 * <p>여기서 보는 것은 세 가지다 —
 * <ol>
 *   <li>로그인 없이 열리고, 응답이 {@code </html>} 까지 온다 (200 만으로는 부족, CLAUDE.md 규칙 3)</li>
 *   <li>기관이 채운 값이 실제로 렌더된다</li>
 *   <li>자리표시자와 개발 메모가 <b>화면에 새지 않는다</b></li>
 * </ol>
 *
 * <p>남은 자리표시자(제4조 추가 제공처, 제5조 위탁)는 기관 회신 대기 중이라
 * 일부러 검사하지 않는다 — 회신이 오면 이 테스트에 함께 잠글 것.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LegalDocumentRenderTest {

    private static final String ORG = "(주)삼성에이엑스아이";

    /** 개발 메모를 Thymeleaf 파서 주석으로 감쌌는지 확인하는 표식 — 본문에 없는 문구여야 한다. */
    private static final String DEV_NOTE_MARKER = "구현이 바뀌면 이 문서도 같이 고쳐야 한다";
    private static final String DEV_NOTE_MARKER_TERMS = "동작이 바뀌면 이 문서도 같이 고쳐야 한다";

    @Autowired MockMvc mvc;

    private String render(String path) throws Exception {
        return mvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("이용약관은 로그인 없이 끝까지 렌더되고 기관명·시행일자가 채워져 있다")
    void 약관_렌더_및_기관정보() throws Exception {
        String html = render("/terms");

        assertThat(html).contains("</html>");
        assertThat(html).contains(ORG);
        assertThat(html).contains("2026-08-05");

        // 자리표시자가 하나도 남으면 안 된다 (약관은 전부 채웠다)
        assertThat(html).doesNotContain("legal-fill");
        assertThat(html).doesNotContain("YYYY-MM-DD");
        assertThat(html).doesNotContain("[기관명]");
    }

    @Test
    @DisplayName("개인정보처리방침에 기관명·시행일자·개인정보 보호책임자가 채워져 있다")
    void 방침_렌더_및_보호책임자() throws Exception {
        String html = render("/privacy");

        assertThat(html).contains("</html>");
        assertThat(html).contains(ORG);
        assertThat(html).contains("2026-08-05");

        // 제9조 개인정보 보호책임자 — 법정 필수 기재사항
        assertThat(html).contains("최형재");
        assertThat(html).contains("대표이사");
        assertThat(html).contains("031-754-9003");
        assertThat(html).contains("edu@samsungaxi.com");

        assertThat(html).doesNotContain("YYYY-MM-DD");
        assertThat(html).doesNotContain("[기관명]");
        assertThat(html).doesNotContain("[성명]");
        assertThat(html).doesNotContain("[직책]");
        assertThat(html).doesNotContain("[전화번호]");
        assertThat(html).doesNotContain("[이메일]");
    }

    @Test
    @DisplayName("개발용 주석이 대외 공개 문서의 소스에 노출되지 않는다")
    void 개발메모_비노출() throws Exception {
        // <!-- --> 로 두면 브라우저에 안 보여도 페이지 소스에는 그대로 나간다.
        // Thymeleaf 파서 주석(<!--/* */-->) 은 렌더 단계에서 제거된다.
        assertThat(render("/terms")).doesNotContain(DEV_NOTE_MARKER_TERMS);
        assertThat(render("/privacy")).doesNotContain(DEV_NOTE_MARKER);

        // 구현 경로가 적힌 줄도 같이 사라져야 한다 (내부 패키지 구조 노출 방지)
        assertThat(render("/privacy")).doesNotContain("CryptoConverter");
        assertThat(render("/terms")).doesNotContain("EnrollmentService");
    }
}
