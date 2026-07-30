package com.ssa.lms.user;

import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 기본 프로필 아바타(/avatar/{id}.svg) — 동적 SVG 렌더 + 가입/백필 연동 검증.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AvatarControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired ProfileImageBackfillRunner backfillRunner;

    @Test
    void 아바타는_이름_이니셜을_담은_SVG로_렌더된다() throws Exception {
        User trainee = userRepository.findByLoginId("trainee1").orElseThrow();
        String initial = trainee.getName().substring(0, 1);

        mvc.perform(get("/avatar/" + trainee.getId() + ".svg")
                        .with(user("trainee1").roles("TRAINEE")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
                .andExpect(content().string(containsString("<svg")))
                .andExpect(content().string(containsString(">" + initial + "</text>")));
    }

    @Test
    void 없는_사용자는_물음표_아바타로_대체된다() throws Exception {
        mvc.perform(get("/avatar/999999.svg")
                        .with(user("trainee1").roles("TRAINEE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">?</text>")));
    }

    @Test
    void 비로그인_접근은_로그인으로_리다이렉트된다() throws Exception {
        mvc.perform(get("/avatar/1.svg"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void 가입하면_기본_아바타_URL이_자동_지정된다() throws Exception {
        mvc.perform(post("/signup/trainee").with(csrf())
                        .param("loginId", "avatar_signup_t")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123")
                        .param("name", "아바타")
                        .param("email", "avatar@test.local")
                        .param("phone", "010-1234-5678")
                        .param("birthDate", "2000-01-01")
                        .param("privacyConsent", "true")
                        .param("thirdPartyConsent", "true"))
                .andExpect(status().is3xxRedirection());

        User user = userRepository.findByLoginId("avatar_signup_t").orElseThrow();
        assertThat(user.getProfileImageUrl()).isEqualTo("/avatar/" + user.getId() + ".svg");
    }

    @Test
    void 백필_러너를_돌리면_프로필_이미지_없는_계정이_사라진다() {
        // 컨텍스트 공유로 다른 테스트가 만든 계정도 섞이므로, 러너를 직접 실행해 멱등 백필을 검증한다
        backfillRunner.run();
        assertThat(userRepository.findByProfileImageUrlIsNull()).isEmpty();
    }
}
