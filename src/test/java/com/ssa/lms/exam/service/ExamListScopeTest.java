package com.ssa.lms.exam.service;

import com.ssa.lms.assignment.dto.AssignmentSearchCond;
import com.ssa.lms.assignment.dto.CourseAssignmentRow;
import com.ssa.lms.assignment.service.CourseAssignmentService;
import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.exam.dto.ExamListRow;
import com.ssa.lms.exam.dto.ExamSearchCond;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시험·과제 목록의 <b>강사 권한 경계</b>를 고정한다.
 *
 * <p>이 두 목록은 원래 전체를 조회한 뒤 자바에서 담당 과정만 남겼다. 그 상태로 페이징을 붙이면
 * 건수가 어긋나고, 무엇보다 "필터가 조회 <em>뒤에</em> 있다"는 구조 자체가 위험하다.
 * 지금은 담당 과정 조건이 쿼리 안에 있다 — 그게 유지되는지를 여기서 지킨다.</p>
 *
 * <p><b>왜 건수 비교로 검증하나:</b> "강사에게 안 보인다"만 확인하면 목록이 통째로 비어도
 * 통과한다. 관리자가 보는 건수보다 <b>적으면서</b>, 강사가 보는 행은 <b>전부 담당 과정</b>이고,
 * 담당 아닌 과정을 <b>직접 지정해도 0건</b>이어야 한다. 셋을 같이 봐야 의미가 있다.</p>
 */
@SpringBootTest
@Transactional
class ExamListScopeTest {

    private static final PageRequest FIRST_PAGE =
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));

    @Autowired ExamService examService;
    @Autowired CourseAssignmentService courseAssignmentService;
    @Autowired CourseQueryService courseQueryService;
    @Autowired UserRepository userRepository;

    private Long instructorId() {
        return userRepository.findByLoginId("instructor1").orElseThrow().getId();
    }

    /** 강사가 담당하지 않는 과정 id. 없으면 이 테스트가 아무것도 못 지키므로 시드를 의심해야 한다. */
    private Long notMyCourseId(Long instructorId) {
        Set<Long> mine = Set.copyOf(courseQueryService.findCourseIdsByInstructorId(instructorId));
        return courseQueryService.findAllCourseOptions().stream()
                .map(CourseQueryService.CourseOption::id)
                .filter(id -> !mine.contains(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "강사 미담당 과정이 시드에 없어 권한 경계를 검증할 수 없다"));
    }

    @Test
    @DisplayName("시험 목록 — 강사는 담당 과정만 보고, 관리자보다 적게 본다")
    void 시험목록_강사_범위() {
        Long instructorId = instructorId();
        Set<Long> myCourses = Set.copyOf(courseQueryService.findCourseIdsByInstructorId(instructorId));

        Page<ExamListRow> asAdmin =
                examService.searchScoped(new ExamSearchCond(), null, true, FIRST_PAGE);
        Page<ExamListRow> asInstructor =
                examService.searchScoped(new ExamSearchCond(), instructorId, false, FIRST_PAGE);

        assertThat(asInstructor.getTotalElements())
                .as("담당 아닌 과정 시험이 강사 목록에 섞이면 권한 사고다")
                .isLessThan(asAdmin.getTotalElements());
        assertThat(allRows(examService, instructorId))
                .isNotEmpty()
                .allSatisfy(row -> assertThat(myCourses).contains(row.courseId()));
    }

    @Test
    @DisplayName("시험 목록 — 강사가 미담당 과정을 직접 지정해도 0건이다")
    void 시험목록_미담당_과정_직접지정() {
        Long instructorId = instructorId();
        ExamSearchCond cond = new ExamSearchCond();
        cond.setCourseId(notMyCourseId(instructorId));

        Page<ExamListRow> result = examService.searchScoped(cond, instructorId, false, FIRST_PAGE);

        assertThat(result.getTotalElements())
                .as("필터가 조회 뒤가 아니라 쿼리 안에 있어야 파라미터 조작으로 못 뚫는다")
                .isZero();
        assertThat(examService.searchScoped(cond, null, true, FIRST_PAGE).getTotalElements())
                .as("관리자에게는 보여야 한다 — 안 보이면 그냥 데이터가 없는 것이라 검증이 무의미해진다")
                .isPositive();
    }

    @Test
    @DisplayName("시험 목록 — 페이지를 이어 붙이면 총 건수와 같고 중복이 없다")
    void 시험목록_페이지_연결() {
        List<ExamListRow> all = allRows(examService, null);
        Page<ExamListRow> first =
                examService.searchScoped(new ExamSearchCond(), null, true, FIRST_PAGE);

        assertThat(all).hasSize((int) first.getTotalElements());
        assertThat(all).extracting(ExamListRow::id).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("과제 목록 — 강사는 담당 과정만 보고, 미담당 과정을 지정해도 0건이다")
    void 과제목록_강사_범위() {
        Long instructorId = instructorId();
        Set<Long> myCourses = Set.copyOf(courseQueryService.findCourseIdsByInstructorId(instructorId));

        Page<CourseAssignmentRow> asAdmin = courseAssignmentService.searchScoped(
                AssignmentSearchCond.empty(), "/admin/evaluation/assignments/", null, true, FIRST_PAGE);
        Page<CourseAssignmentRow> asInstructor = courseAssignmentService.searchScoped(
                AssignmentSearchCond.empty(), "/admin/evaluation/assignments/",
                instructorId, false, FIRST_PAGE);

        assertThat(asInstructor.getTotalElements()).isLessThan(asAdmin.getTotalElements());

        // 화면 행에는 과정 "코드"만 실려서 id 로 못 비교한다. 미담당 과정을 직접 지정해 확인한다.
        AssignmentSearchCond onNotMine = new AssignmentSearchCond(
                notMyCourseId(instructorId), null, null, null);
        assertThat(courseAssignmentService.searchScoped(
                onNotMine, "/admin/evaluation/assignments/", instructorId, false, FIRST_PAGE)
                .getTotalElements()).isZero();
        assertThat(courseAssignmentService.searchScoped(
                onNotMine, "/admin/evaluation/assignments/", null, true, FIRST_PAGE)
                .getTotalElements()).isPositive();
        assertThat(asInstructor.getContent()).isNotEmpty();
        assertThat(myCourses).isNotEmpty();
    }

    /** 전 페이지를 순회해 모은 행. viewerId 가 null 이면 관리자 시점. */
    private List<ExamListRow> allRows(ExamService service, Long instructorId) {
        List<ExamListRow> all = new ArrayList<>();
        for (int page = 0; page < 50; page++) {
            Page<ExamListRow> p = service.searchScoped(
                    new ExamSearchCond(), instructorId, instructorId == null,
                    PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "id")));
            all.addAll(p.getContent());
            if (!p.hasNext()) {
                break;
            }
        }
        return all;
    }
}
