package com.ssa.lms.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 역할별 진입점이 기존 index 화면을 실제로 렌더링(Thymeleaf 파싱)하는지 검증.
 * 뷰 이름 해석뿐 아니라 템플릿 파싱까지 확인하기 위해 렌더링 결과 상태를 본다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ModuleHomeRenderTest {

    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void 관리자_진입화면_렌더링() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void 강사_진입화면_렌더링() throws Exception {
        mvc.perform(get("/instructor")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TRAINEE")
    void 훈련생_진입화면_렌더링() throws Exception {
        mvc.perform(get("/trainee")).andExpect(status().isOk());
    }
}
