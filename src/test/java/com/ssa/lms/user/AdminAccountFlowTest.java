package com.ssa.lms.user;

import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 관리자 계정 관리 — 목록/등록/수정/활성·비활성 화면 및 플로우.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminAccountFlowTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User newAdmin(String loginId) {
        return userRepository.save(User.builder()
                .loginId(loginId).password(passwordEncoder.encode("1234"))
                .name("관리자" + loginId).role(Role.ADMIN).status(UserStatus.ACTIVE)
                .email(loginId + "@ssa.local").phone("010-0000-0000").birthDate("1985-01-01")
                .build());
    }

    @Test
    @DisplayName("관리자 목록/등록/수정 화면이 완전한 HTML 로 렌더링된다")
    @WithMockUser(roles = "ADMIN")
    void 화면_렌더() throws Exception {
        User a = newAdmin("au_render");
        mvc.perform(get("/admin/admins"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin-02-user/admin-user"))
                .andExpect(content().string(containsString("</html>")))
                .andExpect(model().attributeExists("admins"));
        mvc.perform(get("/admin/admins/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("</html>")));
        mvc.perform(get("/admin/admins/{id}/edit", a.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("</html>")));
    }

    @Test
    @DisplayName("신규 관리자를 등록한다")
    @WithMockUser(roles = "ADMIN")
    void 등록() throws Exception {
        mvc.perform(post("/admin/admins").with(csrf())
                        .param("loginId", "au_new").param("password", "pw12345")
                        .param("name", "새관리자").param("email", "au_new@ssa.local")
                        .param("phone", "010-1234-5678").param("birthDate", "1990-03-03")
                        .param("gender", "female").param("education", "4년제 대학 졸업"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/admins"));

        User saved = userRepository.findByLoginId("au_new").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(passwordEncoder.matches("pw12345", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("아이디/비밀번호 누락 시 폼으로 되돌아온다")
    @WithMockUser(roles = "ADMIN")
    void 등록_필수누락() throws Exception {
        mvc.perform(post("/admin/admins").with(csrf())
                        .param("loginId", "").param("password", "")
                        .param("name", "이름있음"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin-02-user/admin-user-add"))
                .andExpect(model().attributeHasFieldErrors("form", "loginId", "password"));
    }

    @Test
    @DisplayName("중복 아이디 등록은 거부된다")
    @WithMockUser(roles = "ADMIN")
    void 등록_중복() throws Exception {
        newAdmin("au_dup");
        mvc.perform(post("/admin/admins").with(csrf())
                        .param("loginId", "au_dup").param("password", "pw12345")
                        .param("name", "중복시도"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "loginId"));
    }

    @Test
    @DisplayName("관리자 정보를 수정한다")
    @WithMockUser(roles = "ADMIN")
    void 수정() throws Exception {
        User a = newAdmin("au_edit");
        mvc.perform(post("/admin/admins/{id}", a.getId()).with(csrf())
                        .param("name", "수정된관리자").param("email", "edited@ssa.local")
                        .param("phone", "010-9999-9999").param("birthDate", "1980-12-12")
                        .param("gender", "male").param("education", "대학원 졸업"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/admins"));

        User saved = userRepository.findById(a.getId()).orElseThrow();
        assertThat(saved.getName()).isEqualTo("수정된관리자");
        assertThat(saved.getEmail()).isEqualTo("edited@ssa.local");
    }

    @Test
    @DisplayName("활성 관리자가 여럿이면 비활성화/활성화가 동작한다")
    @WithMockUser(roles = "ADMIN")
    void 비활성_활성_왕복() throws Exception {
        // 보호 로직에 걸리지 않도록 활성 관리자를 2명 이상 보장
        newAdmin("au_keep_active");
        User target = newAdmin("au_toggle");

        mvc.perform(post("/admin/admins/{id}/deactivate", target.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(userRepository.findById(target.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatus.SUSPENDED);

        mvc.perform(post("/admin/admins/{id}/activate", target.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(userRepository.findById(target.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatus.ACTIVE);
    }
}
