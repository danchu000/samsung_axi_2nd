package com.ssa.lms.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 역할별 진입점이 기존 index 화면을 실제로 렌더링(Thymeleaf 파싱)하는지 검증.
 * 뷰 이름 해석뿐 아니라 템플릿 파싱까지 확인하기 위해 렌더링 결과 상태를 본다.
 *
 * <p>대시보드 전환 후 컨트롤러가 {@code @AuthenticationPrincipal LoginUser} 를 쓴다.
 * {@code @WithMockUser} 는 principal 이 스프링 기본 {@code User} 라 주입이 null 이 되므로,
 * 시드 계정으로 인증하는 {@code @WithUserDetails} 로 바꿨다 (ContentRenderTest 와 같은 방식).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ModuleHomeRenderTest {

    @Autowired MockMvc mvc;

    @Test
    @WithUserDetails("admin")
    void 관리자_진입화면_렌더링() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("dashboard"));
    }

    @Test
    @WithUserDetails("instructor1")
    void 강사_진입화면_렌더링() throws Exception {
        mvc.perform(get("/instructor"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("dashboard"));
    }

    @Test
    @WithUserDetails("trainee1")
    void 훈련생_진입화면_렌더링() throws Exception {
        mvc.perform(get("/trainee"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("dashboard"));
    }
}
