package com.ssa.lms.course;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.CourseStatus;
import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "나의 과정" 카드의 진도 바 · 주 CTA 가 상태별로 맞게 렌더되는지 검증.
 *
 * <p>렌더 도중 예외가 나도 응답은 200 이라 잘린 HTML 이 내려간다 (CLAUDE.md 규칙 3) —
 * 반드시 {@code </html>} 까지 확인한다.</p>
 *
 * <p>검증 데이터를 직접 심으므로 {@code @Transactional} 로 롤백한다. 시드 과정이 다른 테스트의
 * "시작일 내림차순 첫 과정" 가정을 깨지 않도록 남기지 않는다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class MyCourseCardRenderTest {

    @Autowired MockMvc mvc;
    @Autowired CourseRepository courseRepository;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired UserRepository userRepository;

    private Course course(String code, String name) {
        return courseRepository.save(Course.builder()
                .courseCode(code).courseName(name)
                .startDate(LocalDate.of(2026, 3, 1)).endDate(LocalDate.of(2026, 8, 1))
                .capacity(30).status(CourseStatus.IN_PROGRESS).completionProgressRate(80).build());
    }

    private Enrollment enroll(User trainee, Course c, EnrollmentStatus status, double progress) {
        Enrollment e = Enrollment.builder()
                .trainee(trainee).course(c).status(status)
                .appliedAt(LocalDateTime.now().minusDays(3)).build();
        e.updateProgressRate(progress);
        return enrollmentRepository.save(e);
    }

    @Test
    @DisplayName("상태 4종 + 진도 0/중간/100% 카드가 각각 의도대로 렌더된다")
    @WithUserDetails("trainee1")
    void 카드가_상태별로_렌더된다() throws Exception {
        User trainee = userRepository.findByLoginId("trainee1").orElseThrow();

        Course zero      = course("COURSE-CARD-0",   "진도0 과정");
        Course mid       = course("COURSE-CARD-MID", "진도중간 과정");
        Course done      = course("COURSE-CARD-100", "진도100 과정");
        Course applied   = course("COURSE-CARD-APP", "승인대기 과정");
        Course cancelled = course("COURSE-CARD-CAN", "취소된 과정");
        Course rejected  = course("COURSE-CARD-REJ", "반려된 과정");

        enroll(trainee, zero,      EnrollmentStatus.APPROVED,  0.0);
        enroll(trainee, mid,       EnrollmentStatus.APPROVED,  45.5);
        enroll(trainee, done,      EnrollmentStatus.APPROVED,  100.0);
        enroll(trainee, applied,   EnrollmentStatus.APPLIED,   0.0);
        enroll(trainee, cancelled, EnrollmentStatus.CANCELLED, 0.0);
        enroll(trainee, rejected,  EnrollmentStatus.REJECTED,  0.0);

        String html = mvc.perform(get("/trainee/my-course"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 진도 바 — 로케일 탓에 소수점이 콤마로 나오면 width 가 깨지므로 값까지 확인한다
        assertThat(html).contains("width:0.0%", "width:45.5%", "width:100.0%");
        assertThat(html).contains("progress-fill is-done");   // 100% 는 완료색

        // 주 CTA — 진도에 따라 문구가 갈리고, 링크는 해당 과정으로 간다
        assertThat(html).contains("학습 시작", "이어서 학습");
        assertThat(html).contains("/trainee/learning?courseId=" + zero.getId());
        assertThat(html).contains("/trainee/learning?courseId=" + mid.getId());
        assertThat(html).contains("/trainee/learning?courseId=" + done.getId());

        // 승인대기 — 진도 바 대신 안내, 학습으로 가는 링크는 없다
        assertThat(html).contains("승인 대기 중");
        assertThat(html).doesNotContain("/trainee/learning?courseId=" + applied.getId());

        // 취소·반려 — 재신청 안내만, 학습 링크 없음
        assertThat(html).contains("재신청은 과정이 모집중일 때만 가능합니다");
        assertThat(html).doesNotContain("/trainee/learning?courseId=" + cancelled.getId());
        assertThat(html).doesNotContain("/trainee/learning?courseId=" + rejected.getId());

        // 신청 취소는 APPLIED·APPROVED 에만 남는다
        assertThat(html).contains("/trainee/enrollments/");
        assertThat(html).contains("</html>");
    }

    @Test
    @DisplayName("카드의 '이어서 학습' 링크를 따라가면 그 과정의 학습 화면이 열린다")
    @WithUserDetails("trainee1")
    void CTA_링크가_실제로_학습화면으로_이어진다() throws Exception {
        User trainee = userRepository.findByLoginId("trainee1").orElseThrow();
        Course c = course("COURSE-CARD-CTA", "CTA 검증 과정");
        enroll(trainee, c, EnrollmentStatus.APPROVED, 30.0);

        String cards = mvc.perform(get("/trainee/my-course")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(cards).contains("/trainee/learning?courseId=" + c.getId());

        // 카드가 내보낸 링크를 그대로 따라간다 — 잘린 응답도 200 이므로 닫는 태그까지 본다
        String learning = mvc.perform(get("/trainee/learning").param("courseId", String.valueOf(c.getId())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(learning).contains("CTA 검증 과정").contains("</html>");
    }
}
