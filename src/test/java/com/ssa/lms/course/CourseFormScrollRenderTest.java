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
 * 과정 등록/수정/상세 화면의 본문이 스크롤 가능한지 검증.
 *
 * <p>2026-07-31 관리자 "과정 추가" 화면에서 하단의 [과정 등록] 버튼에 도달할 수 없는 문제가 있었다.
 * common-style.css 의 {@code body{overflow:hidden}} + {@code .main-content{height:calc(100vh-70px);overflow:hidden}}
 * 구조라 본문 래퍼가 {@code flex:1 + overflow-y:auto} 를 가져야만 스크롤된다.
 * 잘려도 HTML 자체는 정상이라 렌더 테스트로는 못 잡고, 스타일 선언 자체를 확인해야 한다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class CourseFormScrollRenderTest {

    /** {@code .form-wrap { ... overflow-y: auto ... }} 선언이 있는지 검사하는 패턴. */
    private static final String SCROLLABLE_WRAP = "\\.form-wrap\\s*\\{[^}]*overflow-y\\s*:\\s*auto";

    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void 과정_등록_폼이_스크롤된다() throws Exception {
        assertScrollable("/admin/courses/new", "과정 등록");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 과정_수정_폼이_스크롤된다() throws Exception {
        assertScrollable("/admin/courses/1/edit", "저장하기");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 과정_상세가_스크롤된다() throws Exception {
        assertScrollable("/admin/courses/1", "수정하기");
    }

    private void assertScrollable(String url, String bottomButtonText) throws Exception {
        String html = mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("</html>");
        // 하단 버튼이 렌더는 되지만, 래퍼가 스크롤되지 않으면 화면 밖에 갇힌다.
        assertThat(html).contains(bottomButtonText);
        assertThat(html).containsPattern(SCROLLABLE_WRAP);
    }
}
