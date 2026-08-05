package com.ssa.lms.web;

import com.ssa.lms.assignment.dto.AssignmentSearchCond;
import com.ssa.lms.assignment.dto.CourseAssignmentRow;
import com.ssa.lms.assignment.entity.Assignment;
import com.ssa.lms.assignment.entity.CourseAssignment;
import com.ssa.lms.assignment.repository.CourseAssignmentRepository;
import com.ssa.lms.assignment.service.CourseAssignmentService;
import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.demo.SampleScreenData;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 강사 과제 채점 목록(/instructor/assignments) 이 비어 보이던 사고 재발 방지.
 *
 * <p><b>무슨 일이 있었나:</b> 이 화면은 "본인이 <i>채점자로 지정된</i> 과제" 로 좁혀 왔는데,
 * 과제 배정에서 채점자 지정은 선택 항목이라({@code CourseAssignment.grader} 가 nullable)
 * 채점자를 비워 둔 과제는 <b>담당 강사에게도 한 건도 보이지 않았다</b>. 로컬 시드는 담당
 * 과정의 배정에 전부 채점자를 넣어 두기 때문에 개발 중에는 드러나지 않았고, 채점자 없이
 * 배정한 운영에서만 목록이 통째로 비었다.</p>
 *
 * <p>그래서 이 테스트는 <b>채점자가 비어 있는 배정</b>을 직접 만들어 확인한다. 담당 과정
 * 제한(권한정의서 △)은 그대로여야 하므로 미담당 과정이 새어 나오지 않는 것도 같이 고정한다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class InstructorAssignmentListTest {

    /** 로컬 시드 기준 — 001·002 는 instructor1 담당, 003 은 담당 강사가 없다. */
    private static final String OWN_COURSE = "COURSE-2026-001";
    private static final String OTHER_COURSE = "COURSE-2026-003";
    private static final String GRADING_PREFIX = "/instructor/assignments/";

    @Autowired MockMvc mvc;
    @Autowired CourseAssignmentService courseAssignmentService;
    @Autowired CourseAssignmentRepository courseAssignmentRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired UserRepository userRepository;
    @Autowired SampleScreenData sampleScreenData;

    @PersistenceContext EntityManager em;

    private Long instructorId;

    @BeforeEach
    void findInstructor() {
        instructorId = userRepository.findByLoginId("instructor1").orElseThrow().getId();
    }

    @Test
    @DisplayName("채점자를 지정하지 않은 배정도 담당 강사 목록에 나온다")
    @Transactional
    void ungradedAssignmentIsVisibleToOwningInstructor() {
        Long id = saveUngradedAssignment(OWN_COURSE);

        List<CourseAssignmentRow> rows = courseAssignmentService.searchForInstructor(
                AssignmentSearchCond.empty(), GRADING_PREFIX, instructorId);

        assertThat(rows).extracting(CourseAssignmentRow::id).contains(String.valueOf(id));
        // 채점자 칸은 "-" 로 비어 있어도 행 자체는 보여야 한다
        assertThat(rows).filteredOn(r -> r.id().equals(String.valueOf(id)))
                .allSatisfy(r -> assertThat(r.instructor()).isEqualTo("-"));

        // 예전 기준(채점자 = 나)으로는 보이지 않던 행이라는 것까지 함께 고정한다
        List<CourseAssignmentRow> byGrader = courseAssignmentService.search(
                AssignmentSearchCond.empty().withGrader(instructorId), GRADING_PREFIX);
        assertThat(byGrader).extracting(CourseAssignmentRow::id).doesNotContain(String.valueOf(id));
    }

    @Test
    @DisplayName("담당하지 않는 과정의 과제는 채점자가 비어 있어도 새어 나오지 않는다")
    @Transactional
    void otherCourseStaysHidden() {
        Long id = saveUngradedAssignment(OTHER_COURSE);

        List<CourseAssignmentRow> rows = courseAssignmentService.searchForInstructor(
                AssignmentSearchCond.empty(), GRADING_PREFIX, instructorId);

        assertThat(rows).extracting(CourseAssignmentRow::id).doesNotContain(String.valueOf(id));
    }

    @Test
    @DisplayName("담당 과정이 없는 강사는 빈 목록 — 쿼리로 내려가지 않는다")
    void instructorWithoutCourseGetsEmptyList() {
        assertThat(courseAssignmentService.searchForInstructor(
                AssignmentSearchCond.empty(), GRADING_PREFIX, null)).isEmpty();
    }

    @Test
    @DisplayName("강사 과제 채점 화면이 잘리지 않고 렌더된다")
    @WithUserDetails("instructor1")
    void listPageRenders() throws Exception {
        mvc.perform(get("/instructor/assignments"))
                .andExpect(status().isOk())
                // 200 만으로는 부족하다 — 렌더 도중 예외가 나도 200 이다 (CLAUDE.md 규칙 3)
                .andExpect(content().string(containsString("</html>")));
    }

    @Test
    @DisplayName("예시 행은 채점 화면으로 넘어가지 않고 목록으로 되돌아온다")
    @WithUserDetails("instructor1")
    void sampleRowGradingIsGuarded() throws Exception {
        mvc.perform(get("/instructor/assignments/900401/grading"))
                .andExpect(status().is3xxRedirection());
    }

    /**
     * 담당 과정에 배정이 하나도 없는 강사 — 화면정의서 캡처가 되도록 예시 행이 대신 나온다.
     * 시드 강사(instructor1)는 배정이 있어 이 경로를 타지 않으므로 계정을 따로 만든다.
     */
    @Test
    @DisplayName("배정이 없는 강사는 예시 행과 안내 배너를 본다")
    @Transactional
    void instructorWithoutAssignmentsSeesSampleRows() throws Exception {
        LoginUser fresh = new LoginUser(userRepository.save(User.builder()
                .loginId("instructor-no-course").password("{noop}1234")
                .name("신규강사").role(Role.INSTRUCTOR).status(UserStatus.ACTIVE)
                .email("no-course@ssa.local").phone("010-9999-9999").birthDate("1990-01-01")
                .privacyConsentAt(LocalDateTime.now()).thirdPartyConsentAt(LocalDateTime.now())
                .build()));

        mvc.perform(get("/instructor/assignments")
                        .with(SecurityMockMvcRequestPostProcessors.user(fresh)))
                .andExpect(status().isOk())
                // 예시 배너 fragment 가 실제로 붙는지 (이름이 틀리면 여기서만 터진다)
                .andExpect(content().string(containsString("화면 예시용 샘플 데이터")))
                .andExpect(content().string(containsString("900401")))
                .andExpect(content().string(containsString("</html>")));
    }

    @Test
    @DisplayName("예시 과제 행은 목록 화면과 같은 형식이다")
    void sampleRowsMatchScreenFormat() {
        List<CourseAssignmentRow> sample = sampleScreenData.courseAssignments();

        assertThat(sample).isNotEmpty();
        // 화면 렌더러(assignments.js)가 기대하는 값들
        assertThat(sample).allSatisfy(r -> {
            assertThat(r.evalType()).isEqualTo("assignment");
            assertThat(r.status()).isIn("waiting", "pending", "completed");
            assertThat(r.startDate()).matches("\\d{4}\\.\\d{2}\\.\\d{2}");
            assertThat(r.endTime()).endsWith("까지");
            // 실제 채점 대상이 없으므로 채점 화면으로 보내면 안 된다
            assertThat(r.address()).isEqualTo("#");
            assertThat(SampleScreenData.isSampleId(Long.valueOf(r.id()))).isTrue();
        });
        assertThat(sample).extracting(CourseAssignmentRow::status)
                .contains("waiting", "pending", "completed");
    }

    /** 채점자를 지정하지 않은 배정 1건. 과제 정의는 시드에 있는 것을 재사용한다. */
    private Long saveUngradedAssignment(String courseCode) {
        Course course = courseRepository.findByCourseCode(courseCode).orElseThrow();
        Assignment definition = em.createQuery(
                        "select a from Assignment a order by a.id", Assignment.class)
                .setMaxResults(1).getSingleResult();

        CourseAssignment saved = courseAssignmentRepository.save(CourseAssignment.builder()
                .course(course)
                .assignment(definition)
                .submissionType(CourseAssignment.SubmissionType.TEXT)
                .startAt(LocalDateTime.now().minusDays(1))
                .endAt(LocalDateTime.now().plusDays(7))
                .allowLate(true)
                .allowResubmit(false)
                .maxResubmit(0)
                .autoGrading(false)
                .score(100)
                .passScore(60)
                .grader(null)                 // ← 이 사고의 핵심
                .status(CourseAssignment.CourseAssignmentStatus.OPEN)
                .build());
        em.flush();
        return saved.getId();
    }
}
