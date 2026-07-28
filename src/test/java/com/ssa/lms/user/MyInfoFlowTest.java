package com.ssa.lms.user;

import com.ssa.lms.auth.LoginUser;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 내 정보(본인 프로필) 조회/수정 + 비밀번호 변경 — 3역할 렌더 및 플로우.
 * 시드 계정을 오염시키지 않도록 테스트마다 전용 계정을 만들어 principal 로 주입한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MyInfoFlowTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User newUser(String loginId, Role role) {
        return userRepository.save(User.builder()
                .loginId(loginId).password(passwordEncoder.encode("1234"))
                .name("원본").role(role).status(UserStatus.ACTIVE)
                .email(loginId + "@ssa.local").phone("010-0000-0000").birthDate("1990-01-01")
                .gender("male").education("고등학교 졸업")
                .build());
    }

    @Test
    @DisplayName("3역할 내 정보 화면이 완전한 HTML 로 렌더링된다")
    void 내정보_렌더() throws Exception {
        User admin = newUser("mi_admin", Role.ADMIN);
        User inst = newUser("mi_inst", Role.INSTRUCTOR);
        User trainee = newUser("mi_trainee", Role.TRAINEE);

        mvc.perform(get("/admin/my-info").with(user(new LoginUser(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("</html>")))
                .andExpect(content().string(containsString("내 정보")));
        mvc.perform(get("/instructor/my-info").with(user(new LoginUser(inst))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("</html>")));
        mvc.perform(get("/trainee/my-info").with(user(new LoginUser(trainee))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("</html>")));
    }

    @Test
    @DisplayName("본인 프로필을 수정하면 실제 필드가 갱신된다")
    void 프로필_수정() throws Exception {
        User u = newUser("mi_edit", Role.TRAINEE);
        mvc.perform(post("/trainee/my-info").with(user(new LoginUser(u))).with(csrf())
                        .param("name", "수정이름")
                        .param("email", "new@ssa.local")
                        .param("phone", "010-1111-2222")
                        .param("birthDate", "1995-05-05")
                        .param("gender", "female")
                        .param("education", "4년제 대학 졸업"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trainee/my-info"));

        User saved = userRepository.findById(u.getId()).orElseThrow();
        assertThat(saved.getName()).isEqualTo("수정이름");
        assertThat(saved.getEmail()).isEqualTo("new@ssa.local");
        assertThat(saved.getGender()).isEqualTo("female");
    }

    @Test
    @DisplayName("이름이 공백이면 폼으로 되돌아온다")
    void 이름_공백_검증() throws Exception {
        User u = newUser("mi_blank", Role.ADMIN);
        mvc.perform(post("/admin/my-info").with(user(new LoginUser(u))).with(csrf())
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/my-info"))
                .andExpect(model().attributeHasFieldErrors("form", "name"));
    }

    @Test
    @DisplayName("현재 비밀번호가 맞으면 변경된다")
    void 비밀번호_변경_성공() throws Exception {
        User u = newUser("mi_pw_ok", Role.INSTRUCTOR);
        mvc.perform(post("/instructor/my-info/password").with(user(new LoginUser(u))).with(csrf())
                        .param("currentPassword", "1234")
                        .param("newPassword", "newpass1")
                        .param("confirmPassword", "newpass1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/instructor/my-info"));

        User saved = userRepository.findById(u.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("newpass1", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 변경되지 않는다")
    void 비밀번호_변경_현재틀림() throws Exception {
        User u = newUser("mi_pw_bad", Role.INSTRUCTOR);
        mvc.perform(post("/instructor/my-info/password").with(user(new LoginUser(u))).with(csrf())
                        .param("currentPassword", "wrong")
                        .param("newPassword", "newpass1")
                        .param("confirmPassword", "newpass1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("pwError"));

        User saved = userRepository.findById(u.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("1234", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("새 비밀번호와 확인이 다르면 폼으로 되돌아온다")
    void 비밀번호_불일치() throws Exception {
        User u = newUser("mi_pw_mismatch", Role.TRAINEE);
        mvc.perform(post("/trainee/my-info/password").with(user(new LoginUser(u))).with(csrf())
                        .param("currentPassword", "1234")
                        .param("newPassword", "aaaa1111")
                        .param("confirmPassword", "bbbb2222"))
                .andExpect(status().isOk())
                .andExpect(view().name("trainee/my-info"))
                .andExpect(model().attributeHasFieldErrors("pwForm", "confirmPassword"));
    }
}
