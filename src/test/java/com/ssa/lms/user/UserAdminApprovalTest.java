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
 * 관리자 가입 승인 슬라이스 통합 테스트: PENDING → ACTIVE(승인) / soft delete(반려).
 * 승인이 되어야 신규 가입 계정이 로그인 가능해진다(로그인 블로커 해소).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class UserAdminApprovalTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User newPending(String loginId) {
        return userRepository.save(User.builder()
                .loginId(loginId).password(passwordEncoder.encode("1234"))
                .name("대기자").role(Role.TRAINEE).status(UserStatus.PENDING)
                .email(loginId + "@example.com").phone("010-0000-0000").birthDate("2000-01-01")
                .privacyConsentAt(LocalDateTime.now()).thirdPartyConsentAt(LocalDateTime.now())
                .build());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 승인_대기_목록에_PENDING_계정이_노출된다() throws Exception {
        newPending("pending_list");
        mvc.perform(get("/admin/users/pending"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin-02-user/admin-user-approval"))
                .andExpect(model().attributeExists("pendingUsers"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 승인하면_ACTIVE로_전환된다() throws Exception {
        User u = newPending("approve_me");

        mvc.perform(post("/admin/users/{id}/approve", u.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/pending"));

        assertThat(userRepository.findById(u.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 반려하면_soft_delete되어_목록에서_사라진다() throws Exception {
        User u = newPending("reject_me");

        mvc.perform(post("/admin/users/{id}/reject", u.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/pending"));

        // @SQLRestriction(is_deleted = false) 로 인해 일반 조회에서 제외
        assertThat(userRepository.findById(u.getId())).isEmpty();
        assertThat(userRepository.findByLoginId("reject_me")).isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 이미_ACTIVE인_계정은_승인해도_상태가_바뀌지_않는다() throws Exception {
        User u = newPending("already_active");
        u.changeStatus(UserStatus.ACTIVE);
        userRepository.save(u);

        mvc.perform(post("/admin/users/{id}/approve", u.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        assertThat(userRepository.findById(u.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @WithMockUser(roles = "TRAINEE")
    void 훈련생은_승인_화면에_접근할_수_없다() throws Exception {
        mvc.perform(get("/admin/users/pending"))
                .andExpect(status().isForbidden());
    }
}
