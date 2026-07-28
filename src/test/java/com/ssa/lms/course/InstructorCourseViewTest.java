package com.ssa.lms.course;

import com.ssa.lms.course.entity.*;
import com.ssa.lms.course.repository.CourseInstructorRepository;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.user.entity.User;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 강사 담당 과정/훈련생/일정 화면 검증.
 *
 * <p>렌더 테스트는 응답이 끝까지 그려지는지({@code </html>}) 확인한다 —
 * Thymeleaf 렌더 중 예외(예: session 예약어)는 200 인 채로 응답이 잘려 status 검사만으로는 못 잡는다.</p>
 *
 * <p>권한 경계: 강사는 자기 담당 과정만 접근할 수 있다. 담당 아닌 과정 id → 403,
 * 존재하지 않는 과정 id → 404 를 테스트로 고정한다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class InstructorCourseViewTest {

    @Autowired MockMvc mvc;
    @Autowired CourseRepository courseRepository;
    @Autowired CourseInstructorRepository courseInstructorRepository;
    @Autowired UserRepository userRepository;

    private Course course(String suffix, CourseStatus status) {
        return courseRepository.save(Course.builder()
                .courseCode("COURSE-ICV-" + suffix).courseName("강사뷰 과정 " + suffix).cohort("1기")
                .category("테스트")
                .startDate(LocalDate.of(2026, 1, 1)).endDate(LocalDate.of(2026, 6, 1))
                .capacity(20).status(status).completionProgressRate(80).build());
    }

    private Course ownedCourseWithSchedule(String suffix) {
        Course c = Course.builder()
                .courseCode("COURSE-ICV-" + suffix).courseName("담당 과정 " + suffix).cohort("2기")
                .category("클라우드")
                .startDate(LocalDate.of(2026, 2, 1)).endDate(LocalDate.of(2026, 7, 1))
                .capacity(20).status(CourseStatus.IN_PROGRESS).completionProgressRate(80).build();
        Subject subject = Subject.builder().name("파이썬 기초").description("문법").orderNo(1).build();
        subject.addSession(Session.builder().seq(1).name("환경설정").lessonDate(LocalDate.of(2026, 2, 3)).learningMinutes(60).build());
        c.addSubject(subject);
        c = courseRepository.save(c);
        courseInstructorRepository.save(CourseInstructor.builder()
                .course(c).instructor(instructor1()).primaryInstructor(true).build());
        return c;
    }

    private User instructor1() {
        return userRepository.findByLoginId("instructor1").orElseThrow();
    }

    /* ===== 렌더 ===== */

    @Test
    @WithUserDetails("instructor1")
    void 담당_과정_목록이_끝까지_렌더된다() throws Exception {
        String html = mvc.perform(get("/instructor/courses"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("담당 과정");
        assertThat(html).contains("</html>");
    }

    @Test
    @WithUserDetails("instructor1")
    void 담당_과정_상세가_과목차시까지_끝까지_렌더된다() throws Exception {
        Long id = ownedCourseWithSchedule("DETAIL").getId();
        String html = mvc.perform(get("/instructor/courses/" + id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("과목 · 차시 구성");
        assertThat(html).contains("파이썬 기초");
        assertThat(html).contains("</html>");
    }

    @Test
    @WithUserDetails("instructor1")
    void 담당_훈련생_화면이_끝까지_렌더된다() throws Exception {
        ownedCourseWithSchedule("TRAINEE");
        String html = mvc.perform(get("/instructor/trainees"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("담당 훈련생");
        assertThat(html).contains("</html>");
    }

    @Test
    @WithUserDetails("instructor1")
    void 강사_일정_화면이_lessonDate까지_끝까지_렌더된다() throws Exception {
        Long id = ownedCourseWithSchedule("SCHED").getId();
        String html = mvc.perform(get("/instructor/scheduler").param("courseId", id.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("일정 관리");
        assertThat(html).contains("2026-02-03");
        assertThat(html).contains("</html>");
    }

    @Test
    @WithUserDetails("admin")
    void 관리자_일정_화면이_끝까지_렌더된다() throws Exception {
        String html = mvc.perform(get("/admin/courses/schedule"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("일정 관리");
        assertThat(html).contains("</html>");
    }

    /* ===== 권한 경계 ===== */

    @Test
    @WithUserDetails("instructor1")
    void 담당하지_않는_과정_상세는_403이다() throws Exception {
        Long notMine = course("FORBIDDEN", CourseStatus.IN_PROGRESS).getId();
        mvc.perform(get("/instructor/courses/" + notMine)).andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("instructor1")
    void 담당하지_않는_과정의_훈련생_조회는_403이다() throws Exception {
        Long notMine = course("FORB-TRAINEE", CourseStatus.IN_PROGRESS).getId();
        mvc.perform(get("/instructor/trainees").param("courseId", notMine.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("instructor1")
    void 담당하지_않는_과정의_일정_조회는_403이다() throws Exception {
        Long notMine = course("FORB-SCHED", CourseStatus.IN_PROGRESS).getId();
        mvc.perform(get("/instructor/scheduler").param("courseId", notMine.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("instructor1")
    void 존재하지_않는_과정_상세는_404다() throws Exception {
        mvc.perform(get("/instructor/courses/99999999")).andExpect(status().isNotFound());
    }
}
