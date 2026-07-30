package com.ssa.lms.course;

import com.ssa.lms.course.entity.*;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 수강신청 세로 슬라이스: 훈련생 신청(APPLIED) → 관리자 승인/반려 → 정원 초과 거부 → 취소.
 * 훈련생 엔드포인트는 시드 계정 trainee1(@WithUserDetails)로, 관리자 엔드포인트는 @WithMockUser(ADMIN)로 검증.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class EnrollmentFlowTest {

    @Autowired MockMvc mvc;
    @Autowired CourseRepository courseRepository;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired UserRepository userRepository;

    private Long recruitingCourse(String code, int capacity) {
        return courseRepository.save(Course.builder()
                .courseCode(code).courseName("모집 과정 " + code)
                .startDate(LocalDate.of(2026, 3, 1)).endDate(LocalDate.of(2026, 8, 1))
                .capacity(capacity).status(CourseStatus.RECRUITING).completionProgressRate(80).build()).getId();
    }

    @Test
    @WithUserDetails("trainee1")
    void 나의과정_화면이_렌더링된다() throws Exception {
        mvc.perform(get("/trainee/my-course")).andExpect(status().isOk())
                .andExpect(view().name("trainee/my-course"));
    }

    @Test
    @WithUserDetails("trainee1")
    void 훈련생이_모집중_과정에_신청하면_APPLIED로_저장된다() throws Exception {
        Long courseId = recruitingCourse("COURSE-ENR-1", 30);
        Long traineeId = userRepository.findByLoginId("trainee1").orElseThrow().getId();

        mvc.perform(post("/trainee/courses/" + courseId + "/enroll").with(csrf()))
                .andExpect(redirectedUrl("/trainee/my-course"))
                .andExpect(flash().attributeExists("enrollMessage"));

        Enrollment e = enrollmentRepository.findByTraineeIdAndCourseId(traineeId, courseId).orElseThrow();
        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.APPLIED);
    }

    @Test
    @WithUserDetails("trainee1")
    void 모집중이_아닌_과정_신청은_거부된다() throws Exception {
        Long draft = courseRepository.save(Course.builder()
                .courseCode("COURSE-ENR-DRAFT").courseName("작성중 과정")
                .startDate(LocalDate.of(2026, 3, 1)).endDate(LocalDate.of(2026, 8, 1))
                .capacity(10).status(CourseStatus.DRAFT).completionProgressRate(80).build()).getId();

        mvc.perform(post("/trainee/courses/" + draft + "/enroll").with(csrf()))
                .andExpect(flash().attributeExists("enrollError"));
        Long traineeId = userRepository.findByLoginId("trainee1").orElseThrow().getId();
        assertThat(enrollmentRepository.findByTraineeIdAndCourseId(traineeId, draft)).isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 관리자가_신청을_승인한다() throws Exception {
        Long courseId = recruitingCourse("COURSE-ENR-2", 30);
        User trainee = userRepository.findByLoginId("trainee1").orElseThrow();
        Long enrollId = enrollmentRepository.save(Enrollment.builder()
                .trainee(trainee).course(courseRepository.findById(courseId).orElseThrow())
                .status(EnrollmentStatus.APPLIED).appliedAt(LocalDateTime.now()).build()).getId();

        mvc.perform(post("/admin/courses/" + courseId + "/enrollments/" + enrollId + "/approve").with(csrf()))
                .andExpect(redirectedUrl("/admin/courses/" + courseId));
        assertThat(enrollmentRepository.findById(enrollId).orElseThrow().getStatus())
                .isEqualTo(EnrollmentStatus.APPROVED);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 통합_승인대기_화면이_대기_신청을_보여준다() throws Exception {
        Long courseId = recruitingCourse("COURSE-ENR-PEND", 30);
        User trainee = userRepository.findByLoginId("trainee1").orElseThrow();
        enrollmentRepository.save(Enrollment.builder()
                .trainee(trainee).course(courseRepository.findById(courseId).orElseThrow())
                .status(EnrollmentStatus.APPLIED).appliedAt(LocalDateTime.now()).build());

        String html = mvc.perform(get("/admin/enrollments/pending"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin-03-courses/enrollment-approval"))
                .andReturn().getResponse().getContentAsString();
        // 렌더 도중 예외는 200 인 채 HTML 이 잘리므로 닫힘 태그까지 확인 (CLAUDE.md 규칙 3)
        assertThat(html).contains("모집 과정 COURSE-ENR-PEND").contains("</html>");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 통합_화면에서_승인하면_APPROVED가_된다() throws Exception {
        Long courseId = recruitingCourse("COURSE-ENR-4", 30);
        User trainee = userRepository.findByLoginId("trainee1").orElseThrow();
        Long enrollId = enrollmentRepository.save(Enrollment.builder()
                .trainee(trainee).course(courseRepository.findById(courseId).orElseThrow())
                .status(EnrollmentStatus.APPLIED).appliedAt(LocalDateTime.now()).build()).getId();

        mvc.perform(post("/admin/enrollments/" + enrollId + "/approve").with(csrf()))
                .andExpect(redirectedUrl("/admin/enrollments/pending"))
                .andExpect(flash().attributeExists("message"));
        assertThat(enrollmentRepository.findById(enrollId).orElseThrow().getStatus())
                .isEqualTo(EnrollmentStatus.APPROVED);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 통합_화면에서_반려하면_REJECTED가_된다() throws Exception {
        Long courseId = recruitingCourse("COURSE-ENR-5", 30);
        User trainee = userRepository.findByLoginId("trainee1").orElseThrow();
        Long enrollId = enrollmentRepository.save(Enrollment.builder()
                .trainee(trainee).course(courseRepository.findById(courseId).orElseThrow())
                .status(EnrollmentStatus.APPLIED).appliedAt(LocalDateTime.now()).build()).getId();

        mvc.perform(post("/admin/enrollments/" + enrollId + "/reject").with(csrf()))
                .andExpect(redirectedUrl("/admin/enrollments/pending"))
                .andExpect(flash().attributeExists("message"));
        assertThat(enrollmentRepository.findById(enrollId).orElseThrow().getStatus())
                .isEqualTo(EnrollmentStatus.REJECTED);
    }

    @Test
    @WithUserDetails("trainee1")
    void 통합_승인_화면은_관리자만_접근할_수_있다() throws Exception {
        mvc.perform(get("/admin/enrollments/pending")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 정원이_찬_과정은_승인시_거부된다() throws Exception {
        Long courseId = recruitingCourse("COURSE-ENR-3", 1);
        Course course = courseRepository.findById(courseId).orElseThrow();

        // 정원 1명 — 이미 1명 승인된 상태
        User approved = userRepository.save(User.builder()
                .loginId("enr-approved").password("x").name("기승인").role(Role.TRAINEE).status(UserStatus.ACTIVE).build());
        enrollmentRepository.save(Enrollment.builder().trainee(approved).course(course)
                .status(EnrollmentStatus.APPROVED).appliedAt(LocalDateTime.now()).build());

        User waiting = userRepository.save(User.builder()
                .loginId("enr-waiting").password("x").name("대기").role(Role.TRAINEE).status(UserStatus.ACTIVE).build());
        Long waitId = enrollmentRepository.save(Enrollment.builder().trainee(waiting).course(course)
                .status(EnrollmentStatus.APPLIED).appliedAt(LocalDateTime.now()).build()).getId();

        mvc.perform(post("/admin/courses/" + courseId + "/enrollments/" + waitId + "/approve").with(csrf()))
                .andExpect(flash().attributeExists("enrollmentError"));
        assertThat(enrollmentRepository.findById(waitId).orElseThrow().getStatus())
                .isEqualTo(EnrollmentStatus.APPLIED);
    }
}
