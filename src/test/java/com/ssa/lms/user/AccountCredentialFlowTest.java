package com.ssa.lms.user;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.user.entity.AccessLog;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.AccessLogRepository;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
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
 * 최고 관리자 전용 계정정보(아이디·이메일·비밀번호) 변경.
 *
 * <p>최고 관리자는 loginId {@code admin} 계정 하나뿐이다({@code lms.admin.super-login-id} 기본값) —
 * local 시드가 만들어 두므로 그 계정을 principal 로 주입해 테스트한다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AccountCredentialFlowTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired AccessLogRepository accessLogRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private LoginUser superAdmin() {
        return new LoginUser(userRepository.findByLoginId("admin").orElseThrow());
    }

    private User newUser(String loginId, Role role) {
        return userRepository.save(User.builder()
                .loginId(loginId).password(passwordEncoder.encode("1234"))
                .name("대상" + loginId).role(role).status(UserStatus.ACTIVE)
                .email(loginId + "@ssa.local").phone("010-0000-0000").birthDate("1990-01-01")
                .build());
    }

    @Test
    @DisplayName("계정정보 변경 화면이 완전한 HTML 로 렌더링된다")
    void 화면_렌더() throws Exception {
        User target = newUser("cred_render", Role.TRAINEE);
        mvc.perform(get("/admin/accounts/{id}/credentials", target.getId()).with(user(superAdmin())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin-02-user/admin-user-credentials"))
                .andExpect(content().string(containsString("</html>")))
                .andExpect(content().string(containsString("계정정보 변경")));
    }

    @Test
    @DisplayName("최고 관리자가 훈련생의 아이디·이메일·비밀번호를 한 번에 바꾼다")
    void 변경_성공() throws Exception {
        User target = newUser("cred_t1", Role.TRAINEE);

        mvc.perform(post("/admin/accounts/{id}/credentials", target.getId())
                        .with(user(superAdmin())).with(csrf())
                        .param("loginId", "cred_t1_new")
                        .param("email", "changed@ssa.local")
                        .param("newPassword", "newpw12345")
                        .param("confirmPassword", "newpw12345"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/trainees?selected=" + target.getId()));

        User saved = userRepository.findById(target.getId()).orElseThrow();
        assertThat(saved.getLoginId()).isEqualTo("cred_t1_new");
        assertThat(saved.getEmail()).isEqualTo("changed@ssa.local");
        assertThat(passwordEncoder.matches("newpw12345", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("변경 사실이 대상 계정의 접속 이력에 남는다")
    void 변경_이력기록() throws Exception {
        User target = newUser("cred_log", Role.INSTRUCTOR);

        mvc.perform(post("/admin/accounts/{id}/credentials", target.getId())
                        .with(user(superAdmin())).with(csrf())
                        .param("loginId", "cred_log")
                        .param("email", "cred_log@ssa.local")
                        .param("newPassword", "newpw12345")
                        .param("confirmPassword", "newpw12345"))
                .andExpect(status().is3xxRedirection());

        assertThat(accessLogRepository.findByUserIdOrderByOccurredAtDesc(target.getId(), PageRequest.of(0, 5))
                .getContent())
                .extracting(AccessLog::getType)
                .contains(AccessLog.Type.CREDENTIAL_CHANGE);
    }

    @Test
    @DisplayName("비밀번호를 비워 두면 기존 비밀번호가 유지된다")
    void 비밀번호_빈칸이면_유지() throws Exception {
        User target = newUser("cred_keep", Role.TRAINEE);

        mvc.perform(post("/admin/accounts/{id}/credentials", target.getId())
                        .with(user(superAdmin())).with(csrf())
                        .param("loginId", "cred_keep")
                        .param("email", "cred_keep@ssa.local")
                        .param("newPassword", "")
                        .param("confirmPassword", ""))
                .andExpect(status().is3xxRedirection());

        User saved = userRepository.findById(target.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("1234", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("새 비밀번호 확인이 다르면 저장되지 않는다")
    void 비밀번호_불일치() throws Exception {
        User target = newUser("cred_mismatch", Role.TRAINEE);

        mvc.perform(post("/admin/accounts/{id}/credentials", target.getId())
                        .with(user(superAdmin())).with(csrf())
                        .param("loginId", "cred_mismatch")
                        .param("email", "cred_mismatch@ssa.local")
                        .param("newPassword", "newpw12345")
                        .param("confirmPassword", "newpw99999"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "confirmPassword"));

        assertThat(passwordEncoder.matches("1234",
                userRepository.findById(target.getId()).orElseThrow().getPassword())).isTrue();
    }

    @Test
    @DisplayName("이미 쓰는 아이디로는 바꿀 수 없다")
    void 아이디_중복() throws Exception {
        newUser("cred_taken", Role.TRAINEE);
        User target = newUser("cred_dup", Role.TRAINEE);

        mvc.perform(post("/admin/accounts/{id}/credentials", target.getId())
                        .with(user(superAdmin())).with(csrf())
                        .param("loginId", "cred_taken")
                        .param("email", "cred_dup@ssa.local"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "loginId"));

        assertThat(userRepository.findById(target.getId()).orElseThrow().getLoginId())
                .isEqualTo("cred_dup");
    }

    @Test
    @DisplayName("최고 관리자 계정의 아이디는 바꿀 수 없다 — 판별 기준이 아이디라서")
    void 최고관리자_아이디_고정() throws Exception {
        User admin = userRepository.findByLoginId("admin").orElseThrow();

        mvc.perform(post("/admin/accounts/{id}/credentials", admin.getId())
                        .with(user(superAdmin())).with(csrf())
                        .param("loginId", "admin_renamed")
                        .param("email", "admin@ssa.local"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "loginId"));

        assertThat(userRepository.findById(admin.getId()).orElseThrow().getLoginId()).isEqualTo("admin");
    }

    @Test
    @DisplayName("다른 계정이 최고 관리자 아이디를 가져갈 수 없다")
    void 최고관리자_아이디_선점불가() throws Exception {
        User target = newUser("cred_steal", Role.ADMIN);

        mvc.perform(post("/admin/accounts/{id}/credentials", target.getId())
                        .with(user(superAdmin())).with(csrf())
                        .param("loginId", "admin")
                        .param("email", "cred_steal@ssa.local"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "loginId"));

        assertThat(userRepository.findById(target.getId()).orElseThrow().getLoginId())
                .isEqualTo("cred_steal");
    }

    @Test
    @DisplayName("최고 관리자가 아닌 일반 관리자는 화면·저장 모두 403")
    void 일반관리자_차단() throws Exception {
        User plainAdmin = newUser("cred_plain_admin", Role.ADMIN);
        User target = newUser("cred_victim", Role.TRAINEE);
        LoginUser actor = new LoginUser(plainAdmin);

        mvc.perform(get("/admin/accounts/{id}/credentials", target.getId()).with(user(actor)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/admin/accounts/{id}/credentials", target.getId())
                        .with(user(actor)).with(csrf())
                        .param("loginId", "cred_victim")
                        .param("newPassword", "newpw12345")
                        .param("confirmPassword", "newpw12345"))
                .andExpect(status().isForbidden());

        assertThat(passwordEncoder.matches("1234",
                userRepository.findById(target.getId()).orElseThrow().getPassword())).isTrue();
    }

    @Test
    @DisplayName("최고 관리자에게만 목록·수정 화면에 진입 버튼이 보인다")
    void 진입버튼_노출() throws Exception {
        User target = newUser("cred_btn", Role.TRAINEE);
        LoginUser plainAdmin = new LoginUser(newUser("cred_btn_admin", Role.ADMIN));

        mvc.perform(get("/admin/users/{id}/edit", target.getId()).with(user(superAdmin())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/admin/accounts/" + target.getId() + "/credentials")));

        mvc.perform(get("/admin/users/{id}/edit", target.getId()).with(user(plainAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("/admin/accounts/" + target.getId() + "/credentials"))));
    }
}
