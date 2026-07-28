package com.ssa.lms.course;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.CourseInstructor;
import com.ssa.lms.course.entity.CourseStatus;
import com.ssa.lms.course.repository.CourseInstructorRepository;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * B 계약 조회 4종 (b-audit-reply.md §3-3(2)) + 공통 404 advice (§3-3(3)) 검증.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class CourseQueryServiceTest {

    @Autowired MockMvc mvc;
    @Autowired CourseQueryService courseQueryService;
    @Autowired CourseRepository courseRepository;
    @Autowired CourseInstructorRepository courseInstructorRepository;
    @Autowired UserRepository userRepository;

    private Course course(String suffix) {
        return courseRepository.save(Course.builder()
                .courseCode("COURSE-CQS-" + suffix).courseName("조회계약 과정 " + suffix).cohort("1기")
                .startDate(LocalDate.of(2026, 1, 1)).endDate(LocalDate.of(2026, 6, 1))
                .capacity(20).status(CourseStatus.IN_PROGRESS).completionProgressRate(80).build());
    }

    private User instructor(String suffix, String name) {
        return userRepository.save(User.builder()
                .loginId("cqs-ins-" + suffix).password("x").name(name)
                .role(Role.INSTRUCTOR).status(UserStatus.ACTIVE).build());
    }

    @Test
    void 강사의_담당_과정_id_목록을_한_번에_조회한다() {
        Course c1 = course("A1");
        Course c2 = course("A2");
        course("A3"); // 미배정 과정 — 결과에 없어야 함
        User ins = instructor("A", "담당강사");
        courseInstructorRepository.save(CourseInstructor.builder().course(c1).instructor(ins).primaryInstructor(true).build());
        courseInstructorRepository.save(CourseInstructor.builder().course(c2).instructor(ins).build());

        assertThat(courseQueryService.findCourseIdsByInstructorId(ins.getId()))
                .containsExactlyInAnyOrder(c1.getId(), c2.getId());
        assertThat(courseQueryService.findCourseIdsByInstructorId(-1L)).isEmpty();
    }

    @Test
    void 과정_셀렉트_옵션은_코드와_기수를_포함한다() {
        Course c = course("B1");
        List<CourseQueryService.CourseOption> options = courseQueryService.findAllCourseOptions();

        assertThat(options).extracting(CourseQueryService.CourseOption::id).contains(c.getId());
        CourseQueryService.CourseOption found = options.stream()
                .filter(o -> o.id().equals(c.getId())).findFirst().orElseThrow();
        assertThat(found.courseCode()).isEqualTo("COURSE-CQS-B1");
        assertThat(found.courseName()).isEqualTo("조회계약 과정 B1");
        assertThat(found.cohort()).isEqualTo("1기");
    }

    @Test
    void 사용자_id_목록을_표시_정보로_변환한다() {
        User u1 = instructor("C1", "나강사");
        User u2 = instructor("C2", "가강사");

        List<CourseQueryService.UserDisplay> displays =
                courseQueryService.findUserDisplays(List.of(u1.getId(), u2.getId(), -1L));

        // 없는 id(-1)는 빠지고, 이름 가나다순으로 정렬된다
        assertThat(displays).extracting(CourseQueryService.UserDisplay::name)
                .containsExactly("가강사", "나강사");
        assertThat(displays).extracting(CourseQueryService.UserDisplay::loginId)
                .containsExactly("cqs-ins-C2", "cqs-ins-C1");
        assertThat(courseQueryService.findUserDisplays(List.of())).isEmpty();
        assertThat(courseQueryService.findUserDisplays(null)).isEmpty();
    }

    @Test
    void 과정_담당_강사_목록은_대표강사가_먼저_온다() {
        Course c = course("D1");
        User sub = instructor("D1", "부강사");
        User primary = instructor("D2", "대표강사");
        courseInstructorRepository.save(CourseInstructor.builder().course(c).instructor(sub).build());
        courseInstructorRepository.save(CourseInstructor.builder().course(c).instructor(primary).primaryInstructor(true).build());

        List<CourseQueryService.InstructorDisplay> instructors =
                courseQueryService.findInstructorsByCourseId(c.getId());

        assertThat(instructors).extracting(CourseQueryService.InstructorDisplay::name)
                .containsExactly("대표강사", "부강사");
        assertThat(instructors.get(0).primaryInstructor()).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 없는_과정_상세_URL_은_500이_아니라_404다() throws Exception {
        mvc.perform(get("/admin/courses/999999")).andExpect(status().isNotFound());
    }
}
