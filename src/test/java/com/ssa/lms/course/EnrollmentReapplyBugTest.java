package com.ssa.lms.course;

import com.ssa.lms.course.entity.*;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * 버그 재현: 신청 → 취소 → 재신청 전체 플로우.
 * 증상: 수강신청하면 취소로 표시되고 재신청이 안 된다는 제보.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class EnrollmentReapplyBugTest {

    @Autowired MockMvc mvc;
    @Autowired CourseRepository courseRepository;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired UserRepository userRepository;

    @Test
    @WithUserDetails("trainee1")
    void 신청_취소_후_재신청하면_다시_APPLIED가_된다() throws Exception {
        Long courseId = courseRepository.save(Course.builder()
                .courseCode("COURSE-REAPPLY-1").courseName("재신청 검증 과정")
                // 시작일을 과거로 둔다 — ExamListScopeTest.notMyCourseId 가 "시작일 내림차순 첫 과정"을
                // 집는데, 이 테스트 과정이 맨 앞에 오면 시험 없는 과정이 잡혀 그쪽 검증이 무너진다
                .startDate(LocalDate.of(2026, 3, 1)).endDate(LocalDate.of(2026, 8, 1))
                .capacity(30).status(CourseStatus.RECRUITING).completionProgressRate(80).build()).getId();
        Long traineeId = userRepository.findByLoginId("trainee1").orElseThrow().getId();

        // 1) 최초 신청 → APPLIED
        mvc.perform(post("/trainee/courses/" + courseId + "/enroll").with(csrf()))
                .andExpect(redirectedUrl("/trainee/my-course"));
        Enrollment e = enrollmentRepository.findByTraineeIdAndCourseId(traineeId, courseId).orElseThrow();
        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.APPLIED);
        Long enrollmentId = e.getId();

        // 2) 취소 → CANCELLED
        mvc.perform(post("/trainee/enrollments/" + enrollmentId + "/cancel").with(csrf()))
                .andExpect(redirectedUrl("/trainee/my-course"));
        assertThat(enrollmentRepository.findById(enrollmentId).orElseThrow().getStatus())
                .isEqualTo(EnrollmentStatus.CANCELLED);

        // 3) 취소 후 화면: 과정이 다시 "모집중 과정 신청" 목록에 보여야 한다
        String htmlAfterCancel = mvc.perform(get("/trainee/my-course"))
                .andReturn().getResponse().getContentAsString();
        assertThat(htmlAfterCancel).contains("재신청 검증 과정").contains("</html>");

        // 4) 재신청 → 같은 행이 다시 APPLIED 로 돌아와야 한다
        mvc.perform(post("/trainee/courses/" + courseId + "/enroll").with(csrf()))
                .andExpect(redirectedUrl("/trainee/my-course"));
        Enrollment after = enrollmentRepository.findByTraineeIdAndCourseId(traineeId, courseId).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(EnrollmentStatus.APPLIED);

        // 5) 재신청 후 화면에 "취소" 배지가 아닌 "신청" 배지가 떠야 한다
        String htmlAfterReapply = mvc.perform(get("/trainee/my-course"))
                .andReturn().getResponse().getContentAsString();
        assertThat(htmlAfterReapply).contains("badge-APPLIED").contains("</html>");
    }
}
