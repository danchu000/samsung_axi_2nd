package com.ssa.lms.course;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.CourseStatus;
import com.ssa.lms.course.repository.CourseInstructorRepository;
import com.ssa.lms.course.repository.CourseRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 강사-과정 매핑 세로 슬라이스: 배정 → 중복 거부 → 비(非)강사 거부 → 해제.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class InstructorAssignmentFlowTest {

    @Autowired MockMvc mvc;
    @Autowired CourseRepository courseRepository;
    @Autowired CourseInstructorRepository courseInstructorRepository;
    @Autowired UserRepository userRepository;

    private Long course;
    private Long instructorId;
    private Long traineeId;

    private void fixture(String suffix) {
        course = courseRepository.save(Course.builder()
                .courseCode("COURSE-INS-" + suffix).courseName("강사매핑 과정")
                .startDate(LocalDate.of(2026, 1, 1)).endDate(LocalDate.of(2026, 6, 1))
                .capacity(20).status(CourseStatus.DRAFT).completionProgressRate(80).build()).getId();
        instructorId = userRepository.save(User.builder()
                .loginId("ins-" + suffix).password("x").name("배정강사")
                .role(Role.INSTRUCTOR).status(UserStatus.ACTIVE).build()).getId();
        traineeId = userRepository.save(User.builder()
                .loginId("tra-" + suffix).password("x").name("수강생")
                .role(Role.TRAINEE).status(UserStatus.ACTIVE).build()).getId();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 강사를_배정하고_해제한다() throws Exception {
        fixture("A");
        mvc.perform(post("/admin/courses/" + course + "/instructors").with(csrf())
                        .param("userId", instructorId.toString()).param("primary", "true"))
                .andExpect(redirectedUrl("/admin/courses/" + course));
        assertThat(courseInstructorRepository.existsByCourseIdAndInstructorId(course, instructorId)).isTrue();

        Long mappingId = courseInstructorRepository.findByCourseId(course).get(0).getId();
        mvc.perform(post("/admin/courses/" + course + "/instructors/" + mappingId + "/delete").with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(courseInstructorRepository.existsByCourseIdAndInstructorId(course, instructorId)).isFalse();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 이미_배정된_강사를_다시_배정하면_추가되지_않는다() throws Exception {
        fixture("B");
        mvc.perform(post("/admin/courses/" + course + "/instructors").with(csrf())
                .param("userId", instructorId.toString()));
        mvc.perform(post("/admin/courses/" + course + "/instructors").with(csrf())
                        .param("userId", instructorId.toString()))
                .andExpect(flash().attributeExists("instructorError"));
        assertThat(courseInstructorRepository.findByCourseId(course)).hasSize(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 강사가_아닌_계정은_배정할_수_없다() throws Exception {
        fixture("C");
        mvc.perform(post("/admin/courses/" + course + "/instructors").with(csrf())
                        .param("userId", traineeId.toString()))
                .andExpect(flash().attributeExists("instructorError"));
        assertThat(courseInstructorRepository.findByCourseId(course)).isEmpty();
    }
}
