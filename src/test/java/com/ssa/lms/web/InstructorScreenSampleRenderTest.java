package com.ssa.lms.web;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.demo.SampleInstructorData;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 강사 화면 예시 데이터 렌더 테스트.
 *
 * <p><b>왜 필요한가:</b> 예시 배너와 예시 행은 <b>데이터가 0건일 때만</b> 나오는 경로라서,
 * 데이터가 있는 시드 계정으로 도는 일반 렌더 테스트로는 한 줄도 실행되지 않는다.
 * 잘못된 fragment 이름이나 모델 타입 불일치는 딱 이 경로에서만 터지므로,
 * <b>담당 과정이 없는 강사</b>를 만들어 전 화면을 훑는다.</p>
 *
 * <p>렌더 예외가 나도 응답은 200 이므로 {@code </html>} 까지 왔는지 반드시 확인한다
 * (CLAUDE.md 규칙 3).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class InstructorScreenSampleRenderTest {

    /**
     * <b>강사 단위</b>로 좁혀지는 화면들 — 담당 과정이 없는 강사에게는 실제 데이터가 0건이라
     * 예시 경로를 탄다.
     */
    private static final String[] SAMPLE_PAGES = {
            "/instructor/grading",
            "/instructor/graduate",
            "/instructor/trainees",
            "/instructor/attendance",
            "/instructor/courses",
            "/instructor/scheduler",
            "/instructor/proctor/exams",
            "/instructor/proctor/recordings",
    };

    /**
     * 강사와 무관하게 <b>전역 목록</b>인 화면들. 콘텐츠·공지는 관리자가 등록한 것이 하나라도
     * 있으면 그게 나오는 게 맞다 — 예시는 정말 0건일 때만이라 여기서는 렌더만 확인한다.
     */
    private static final String[] SHARED_PAGES = {
            "/instructor/contents",
            "/instructor/notice",
    };

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired SampleInstructorData sampleData;

    private LoginUser freshInstructor(String loginId) {
        return new LoginUser(userRepository.save(User.builder()
                .loginId(loginId).password("{noop}1234")
                .name("신규강사").role(Role.INSTRUCTOR).status(UserStatus.ACTIVE)
                .email(loginId + "@ssa.local").phone("010-9999-9999").birthDate("1990-01-01")
                .privacyConsentAt(LocalDateTime.now()).thirdPartyConsentAt(LocalDateTime.now())
                .build()));
    }

    @Test
    @DisplayName("담당 과정이 없는 강사도 모든 화면에서 예시 데이터와 안내 배너를 본다")
    @Transactional
    void everyInstructorScreenFallsBackToSample() throws Exception {
        LoginUser fresh = freshInstructor("instructor-sample-all");

        for (String page : SAMPLE_PAGES) {
            mvc.perform(get(page).with(SecurityMockMvcRequestPostProcessors.user(fresh)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("화면 예시용 샘플 데이터")))
                    // 200 만으로는 부족하다 — 렌더 도중 예외가 나도 200 이다
                    .andExpect(content().string(containsString("</html>")));
        }
        for (String page : SHARED_PAGES) {
            mvc.perform(get(page).with(SecurityMockMvcRequestPostProcessors.user(fresh)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("</html>")));
        }
    }

    @Test
    @DisplayName("전역 목록 화면의 예시 데이터도 준비돼 있다 (콘텐츠·공지가 0건인 서버용)")
    void sharedScreenSamplesExist() {
        assertThat(sampleData.contents()).isNotEmpty();
        assertThat(sampleData.notices()).isNotEmpty();
    }

    @Test
    @DisplayName("예시 시험의 채점 현황·채점 팝업이 열린다")
    @Transactional
    void sampleExamGradingScreens() throws Exception {
        LoginUser fresh = freshInstructor("instructor-sample-exam");

        // 시험별 채점 현황
        mvc.perform(get("/instructor/grading/exams/900701")
                        .with(SecurityMockMvcRequestPostProcessors.user(fresh)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("화면 예시용 샘플 데이터")))
                .andExpect(content().string(containsString("</html>")));

        // 채점 팝업 — 자동채점 문항과 수동채점(서술형) 문항이 함께 있어야 한다.
        // 문항은 인라인 JSON 으로 내려가는데 Thymeleaf 가 한글을 \\uXXXX 로 escape 하므로
        // 한글로 단언하면 항상 실패한다. escape 되지 않는 필드로 확인한다.
        mvc.perform(get("/instructor/grading/attempts/900801")
                        .with(SecurityMockMvcRequestPostProcessors.user(fresh)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"attemptId\":\"900801\"")))
                // 수동채점 문항(manual=true)과 자동채점 문항(manual=false)이 둘 다 있어야 한다
                .andExpect(content().string(containsString("\"manual\":true")))
                .andExpect(content().string(containsString("\"manual\":false")));
    }

    @Test
    @DisplayName("예시 감독 상세가 열린다")
    @Transactional
    void sampleProctorDetail() throws Exception {
        LoginUser fresh = freshInstructor("instructor-sample-proctor");

        mvc.perform(get("/instructor/proctor/exams/900701")
                        .with(SecurityMockMvcRequestPostProcessors.user(fresh)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("</html>")));
    }

    @Test
    @DisplayName("예시 시험은 성적 파일을 만들지 않고 이유를 알려준다")
    @Transactional
    void sampleExamHasNoGradeFile() throws Exception {
        LoginUser fresh = freshInstructor("instructor-sample-file");

        mvc.perform(get("/instructor/grading/exams/900701/grades.xlsx")
                        .with(SecurityMockMvcRequestPostProcessors.user(fresh)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/instructor/grading/exams/900701/grades.csv")
                        .with(SecurityMockMvcRequestPostProcessors.user(fresh)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("데이터가 있는 강사는 예시가 아니라 실제 데이터를 본다")
    @WithUserDetails("instructor1")
    void realDataWinsOverSample() throws Exception {
        // 시드 강사는 담당 과정과 시험이 있다
        mvc.perform(get("/instructor/grading"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("화면 예시용 샘플 데이터"))));
        mvc.perform(get("/instructor/courses"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("화면 예시용 샘플 데이터"))));
    }

    @Test
    @DisplayName("예시 녹화는 재생 링크를 주지 않는다 (없는 파일을 찾으러 가면 안 된다)")
    void sampleRecordingsAreNotPlayable() {
        assertThat(sampleData.recordings()).isNotEmpty().allSatisfy(r -> {
            assertThat(r.playable()).isFalse();
            assertThat(r.streamUrl()).isEmpty();
        });
    }

    @Test
    @DisplayName("예시 콘텐츠는 파일 링크를 주지 않는다")
    void sampleContentsHaveNoFileUrl() {
        assertThat(sampleData.contents()).isNotEmpty()
                .allSatisfy(c -> assertThat(c.fileUrl()).isNull());
    }

    @Test
    @DisplayName("예시 출결 매트릭스는 요청마다 같은 값이다 (캡처본과 화면이 어긋나면 안 된다)")
    void sampleAttendanceMatrixIsDeterministic() {
        var first = sampleData.attendanceMatrix();
        var second = sampleData.attendanceMatrix();

        assertThat(first.getColumns()).hasSameSizeAs(second.getColumns());
        assertThat(first.getRows()).hasSameSizeAs(second.getRows());
        for (int i = 0; i < first.getRows().size(); i++) {
            assertThat(first.getRows().get(i).attendanceRate())
                    .isEqualTo(second.getRows().get(i).attendanceRate());
        }
    }
}
