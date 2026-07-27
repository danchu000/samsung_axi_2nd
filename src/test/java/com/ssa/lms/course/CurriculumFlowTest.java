package com.ssa.lms.course;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.CourseStatus;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.SessionRepository;
import com.ssa.lms.course.repository.SubjectRepository;
import com.ssa.lms.course.entity.Subject;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 과목/차시 구성 세로 슬라이스: 과목 추가(orderNo 자동) → 차시 추가(seq 자동) → 삭제(orphanRemoval).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class CurriculumFlowTest {

    @Autowired MockMvc mvc;
    @Autowired CourseRepository courseRepository;
    @Autowired SubjectRepository subjectRepository;
    @Autowired SessionRepository sessionRepository;

    private Long newCourse(String code) {
        return courseRepository.save(Course.builder()
                .courseCode(code).courseName("구성 테스트 과정")
                .startDate(LocalDate.of(2026, 1, 1)).endDate(LocalDate.of(2026, 6, 1))
                .capacity(20).status(CourseStatus.DRAFT).completionProgressRate(80).build()).getId();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 과목_두개_추가시_orderNo가_1_2로_부여된다() throws Exception {
        Long courseId = newCourse("COURSE-CUR-1");

        mvc.perform(post("/admin/courses/" + courseId + "/subjects").with(csrf())
                .param("name", "과목A")).andExpect(redirectedUrl("/admin/courses/" + courseId));
        mvc.perform(post("/admin/courses/" + courseId + "/subjects").with(csrf())
                .param("name", "과목B")).andExpect(redirectedUrl("/admin/courses/" + courseId));

        List<Subject> subjects = subjectRepository.findByCourseIdOrderByOrderNo(courseId);
        assertThat(subjects).extracting(Subject::getName).containsExactly("과목A", "과목B");
        assertThat(subjects).extracting(Subject::getOrderNo).containsExactly(1, 2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 차시_추가시_seq가_자동으로_이어붙고_삭제하면_함께_사라진다() throws Exception {
        Long courseId = newCourse("COURSE-CUR-2");
        mvc.perform(post("/admin/courses/" + courseId + "/subjects").with(csrf()).param("name", "과목X"));
        Long subjectId = subjectRepository.findByCourseIdOrderByOrderNo(courseId).get(0).getId();

        mvc.perform(post("/admin/courses/" + courseId + "/subjects/" + subjectId + "/sessions").with(csrf())
                .param("name", "1차시").param("learningMinutes", "60"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/courses/" + courseId + "/subjects/" + subjectId + "/sessions").with(csrf())
                .param("name", "2차시"))
                .andExpect(status().is3xxRedirection());

        assertThat(sessionRepository.findBySubjectIdOrderBySeq(subjectId))
                .extracting("seq").containsExactly(1, 2);

        // 과목 삭제 → 차시 orphanRemoval
        mvc.perform(post("/admin/courses/" + courseId + "/subjects/" + subjectId + "/delete").with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(subjectRepository.findById(subjectId)).isEmpty();
        assertThat(sessionRepository.findBySubjectIdOrderBySeq(subjectId)).isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 과목명_없이_추가시_저장되지_않는다() throws Exception {
        Long courseId = newCourse("COURSE-CUR-3");
        mvc.perform(post("/admin/courses/" + courseId + "/subjects").with(csrf()).param("name", ""))
                .andExpect(status().is3xxRedirection());
        assertThat(subjectRepository.findByCourseIdOrderByOrderNo(courseId)).isEmpty();
    }
}
