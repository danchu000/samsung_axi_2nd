package com.ssa.lms.user;

import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 강사/훈련생 목록·상세·수정·접속이력 슬라이스 통합 테스트.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class UserManagementTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User newUser(String loginId, Role role) {
        return userRepository.save(User.builder()
                .loginId(loginId).password(passwordEncoder.encode("1234"))
                .name("원본이름").role(role).status(UserStatus.ACTIVE)
                .email(loginId + "@example.com").phone("010-0000-0000").birthDate("1990-01-01")
                .privacyConsentAt(LocalDateTime.now()).thirdPartyConsentAt(LocalDateTime.now())
                .build());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 훈련생_목록이_렌더링된다() throws Exception {
        newUser("mgmt_trainee", Role.TRAINEE);
        mvc.perform(get("/admin/users/trainees"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin-02-user/admin-user-trainee"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 상세_선택시_selected가_채워진다() throws Exception {
        User u = newUser("mgmt_detail", Role.INSTRUCTOR);
        mvc.perform(get("/admin/users/instructors").param("selected", String.valueOf(u.getId())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selected", org.hamcrest.Matchers.notNullValue()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 수정하면_User_필드가_갱신된다() throws Exception {
        User u = newUser("mgmt_edit", Role.TRAINEE);

        mvc.perform(post("/admin/users/{id}", u.getId()).with(csrf())
                        .param("name", "수정된이름")
                        .param("email", "changed@example.com")
                        .param("phone", "010-9999-8888")
                        .param("birthDate", "1995-05-05")
                        .param("gender", "female")
                        .param("education", "4년제 대학 졸업")
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/trainees?selected=" + u.getId()));

        User saved = userRepository.findById(u.getId()).orElseThrow();
        assertThat(saved.getName()).isEqualTo("수정된이름");
        assertThat(saved.getEmail()).isEqualTo("changed@example.com");
        assertThat(saved.getPhone()).isEqualTo("010-9999-8888");
        assertThat(saved.getGender()).isEqualTo("female");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 이름_공백이면_수정_폼으로_되돌아온다() throws Exception {
        User u = newUser("mgmt_blank", Role.TRAINEE);
        mvc.perform(post("/admin/users/{id}", u.getId()).with(csrf())
                        .param("name", "")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin-02-user/admin-user-edit"))
                .andExpect(model().attributeHasFieldErrors("form", "name"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 계정_상태_변경과_삭제가_동작한다() throws Exception {
        User u = newUser("mgmt_status", Role.TRAINEE);

        mvc.perform(post("/admin/users/{id}/status", u.getId()).with(csrf()).param("status", "SUSPENDED"))
                .andExpect(status().is3xxRedirection());
        assertThat(userRepository.findById(u.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatus.SUSPENDED);

        mvc.perform(post("/admin/users/{id}/delete", u.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(userRepository.findById(u.getId())).isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 접속_이력_화면이_렌더링된다() throws Exception {
        User u = newUser("mgmt_log", Role.TRAINEE);
        mvc.perform(get("/admin/users/{id}/access-history", u.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin-02-user/admin-user-access-log"))
                .andExpect(model().attributeExists("logs"));
    }
}
