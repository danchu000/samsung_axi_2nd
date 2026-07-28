package com.ssa.lms.assignment.service;

import com.ssa.lms.assignment.dto.AssignmentSearchCond;
import com.ssa.lms.assignment.dto.CourseAssignmentForm;
import com.ssa.lms.assignment.dto.CourseAssignmentRow;
import com.ssa.lms.assignment.dto.SubmissionTypes;
import com.ssa.lms.assignment.dto.UserOption;
import com.ssa.lms.assignment.entity.Assignment;
import com.ssa.lms.assignment.entity.AssignmentCriteria;
import com.ssa.lms.assignment.entity.CourseAssignment;
import com.ssa.lms.assignment.repository.AssignmentLookupRepository;
import com.ssa.lms.assignment.repository.CourseAssignmentRepository;
import com.ssa.lms.assignment.repository.SubmissionRepository;
import com.ssa.lms.course.service.CourseQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 과정에 배정된 과제 서비스.
 *
 * <p>{@code Assignment}(재사용 정의)와 {@code CourseAssignment}(과정 배정)는 분리돼 있고,
 * 훈련생이 보고 제출하는 단위는 <b>항상 CourseAssignment</b> 다. IA "데이터 정리" 2~3행
 * 구조를 그대로 따른 것이라 합치면 안 된다.</p>
 *
 * <p>수강생 명단은 A의 {@link CourseQueryService#findUserIdsByCourseId(Long)} 만 쓴다.
 * A 소유 리포지토리(EnrollmentRepository 등)를 직접 주입하지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseAssignmentService {

    private final CourseAssignmentRepository courseAssignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final AssignmentLookupRepository lookupRepository;
    private final AssignmentService assignmentService;
    private final CourseQueryService courseQueryService;

    public CourseAssignment getOrThrow(Long id) {
        return courseAssignmentRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("배정된 과제를 찾을 수 없습니다. id=" + id));
    }

    /**
     * 목록 조회.
     *
     * 제출 인원·채점 인원은 행마다 조회하면 N+1 이라 id 묶음으로 한 번씩만 집계한다.
     * 수강생 수는 과정별로 캐시해서 같은 과정이 여러 번 나와도 A쪽 조회를 한 번만 한다.
     *
     * @param gradingUrlPrefix 화면별 채점 URL 앞부분 (관리자/강사가 다르다)
     */
    public List<CourseAssignmentRow> search(AssignmentSearchCond cond, String gradingUrlPrefix) {
        List<CourseAssignment> found = courseAssignmentRepository.search(
                cond.courseId(), cond.graderId(), cond.entityStatusOrNull(), cond.keywordOrNull());
        return applyDerivedStatus(toRows(found, 0, gradingUrlPrefix), cond.derivedStatusOrNull());
    }

    /**
     * 목록 조회 — 강사는 담당 과정 과제만, <b>서버 페이징</b>.
     *
     * <p><b>강사 제한이 쿼리 조건으로 들어간다.</b> 관리자 과제 목록은 지금까지 아무 제한이 없어
     * 강사가 들어오면 담당하지 않는 과정의 과제까지 다 보였다(권한정의서의 △ 위반).
     * 시험 목록과 같은 방식으로 A의 {@link CourseQueryService#findCourseIdsByInstructorId(Long)}
     * 결과를 SQL 에 내려서 막는다.</p>
     *
     * <p><b>화면 상태 필터(waiting/pending/completed)만 예외다.</b> 이 값은 채점 진행률에서
     * 파생되는 것이라 DB where 로 걸 수가 없다. 그 필터가 걸린 경우에는
     * (권한 조건은 그대로 쿼리에 건 채로) 대상 전체를 만들어 거른 뒤 <b>메모리에서 잘라낸다</b> —
     * 그래야 totalCount 가 화면에 실제로 보이는 건수와 맞는다. DB 로 먼저 잘라버리면
     * "10건 가져와 3건만 남는데 총 16건" 이 되어 페이지 수가 통째로 어긋난다.</p>
     */
    public Page<CourseAssignmentRow> searchScoped(AssignmentSearchCond cond, String gradingUrlPrefix,
                                                  Long viewerId, boolean admin, Pageable pageable) {
        boolean scoped = !admin;
        List<Long> courseIds = List.of(-1L);   // scoped=false 일 때 자리만 채우는 더미
        if (scoped) {
            courseIds = viewerId == null
                    ? List.of() : courseQueryService.findCourseIdsByInstructorId(viewerId);
            if (courseIds.isEmpty()) {
                // 담당 과정이 없는 강사. 빈 in 절을 DB 에 내리지 않는다.
                return Page.empty(pageable);
            }
        }

        String derivedStatus = cond.derivedStatusOrNull();
        if (derivedStatus == null) {
            Page<CourseAssignment> page = courseAssignmentRepository.searchPage(
                    cond.courseId(), cond.graderId(), cond.entityStatusOrNull(), cond.keywordOrNull(),
                    scoped, courseIds, pageable);
            List<CourseAssignmentRow> rows = toRows(
                    page.getContent(), (int) pageable.getOffset(), gradingUrlPrefix);
            return new PageImpl<>(rows, pageable, page.getTotalElements());
        }

        List<CourseAssignment> all = courseAssignmentRepository.searchPage(
                cond.courseId(), cond.graderId(), cond.entityStatusOrNull(), cond.keywordOrNull(),
                scoped, courseIds, Pageable.unpaged(pageable.getSort())).getContent();
        List<CourseAssignmentRow> filtered =
                applyDerivedStatus(toRows(all, 0, gradingUrlPrefix), derivedStatus);

        int from = Math.min((int) pageable.getOffset(), filtered.size());
        int to = Math.min(from + pageable.getPageSize(), filtered.size());
        // 잘라낸 뒤 번호를 다시 매긴다 — 2페이지가 1번부터 시작하면 안 된다.
        List<CourseAssignmentRow> pageRows = renumber(filtered.subList(from, to), from);
        return new PageImpl<>(pageRows, pageable, filtered.size());
    }

    /* ===== 목록 행 조립 ===== */

    /**
     * 엔티티 → 화면 행. 제출/채점 인원은 id 묶음으로 한 번씩만 집계하고(N+1 회피),
     * 수강생 수는 과정별로 캐시해 같은 과정이 여러 번 나와도 A쪽 조회를 한 번만 한다.
     *
     * @param startNumber 이 묶음의 첫 행 앞에 몇 건이 있는지 (서버 페이징 오프셋)
     */
    private List<CourseAssignmentRow> toRows(List<CourseAssignment> found, int startNumber,
                                             String gradingUrlPrefix) {
        List<Long> ids = found.stream().map(CourseAssignment::getId).toList();
        Map<Long, Long> submitted = toCountMap(
                ids.isEmpty() ? List.of() : courseAssignmentRepository.countSubmittedUsers(ids));
        Map<Long, Long> graded = toCountMap(
                ids.isEmpty() ? List.of() : courseAssignmentRepository.countGraded(ids));
        Map<Long, Long> enrolledByCourse = new HashMap<>();

        List<CourseAssignmentRow> rows = new ArrayList<>();
        int number = startNumber;
        for (CourseAssignment ca : found) {
            Long courseId = ca.getCourse().getId();
            long enrolled = enrolledByCourse.computeIfAbsent(courseId,
                    id -> (long) courseQueryService.findUserIdsByCourseId(id).size());
            rows.add(CourseAssignmentRow.of(
                    ca, ++number,
                    submitted.getOrDefault(ca.getId(), 0L),
                    graded.getOrDefault(ca.getId(), 0L),
                    enrolled,
                    gradingUrlPrefix));
        }
        return rows;
    }

    /**
     * 화면 상태(waiting/pending/completed) 필터.
     * 채점 진행률에서 파생되는 값이라 DB where 로 못 건다 — 행을 만든 뒤에 걸러낸다.
     */
    private List<CourseAssignmentRow> applyDerivedStatus(List<CourseAssignmentRow> rows,
                                                         String statusFilter) {
        if (statusFilter == null) {
            return rows;
        }
        return renumber(rows.stream().filter(r -> r.status().equals(statusFilter)).toList(), 0);
    }

    /** 번호만 {@code startNumber + 1} 부터 다시 매긴 복사본. */
    private List<CourseAssignmentRow> renumber(List<CourseAssignmentRow> rows, int startNumber) {
        List<CourseAssignmentRow> result = new ArrayList<>(rows.size());
        int n = startNumber;
        for (CourseAssignmentRow row : rows) {
            result.add(new CourseAssignmentRow(
                    row.id(), ++n, row.courseName(), row.courseId(), row.title(),
                    row.instructor(), row.evalType(), row.startDate(), row.startTime(),
                    row.endDate(), row.endTime(), row.submitted(), row.notSubmitted(),
                    row.status(), row.address()));
        }
        return result;
    }

    /**
     * 미제출자 목록.
     * 수강생 전체(A의 CourseQueryService) − 제출 이력이 있는 사람.
     */
    public List<UserOption> findNotSubmittedUsers(Long courseAssignmentId) {
        CourseAssignment ca = getOrThrow(courseAssignmentId);
        List<Long> enrolled = courseQueryService.findUserIdsByCourseId(ca.getCourse().getId());
        List<Long> submitted = submissionRepository.findSubmittedUserIds(courseAssignmentId);
        List<Long> notSubmitted = enrolled.stream().filter(id -> !submitted.contains(id)).toList();
        return List.copyOf(lookupRepository.findUserOptions(notSubmitted).values());
    }

    public CourseAssignmentForm loadForm(Long id) {
        return CourseAssignmentForm.from(getOrThrow(id));
    }

    @Transactional
    public Long create(CourseAssignmentForm form) {
        validate(form);
        if (courseAssignmentRepository.existsByCourseIdAndAssignmentId(
                form.getCourseId(), form.getAssignmentId())) {
            throw new IllegalArgumentException("이 과정에 이미 배정된 과제입니다.");
        }
        Assignment assignment = assignmentService.getOrThrow(form.getAssignmentId());

        CourseAssignment ca = CourseAssignment.builder()
                .course(lookupRepository.courseRef(form.getCourseId()))
                .assignment(assignment)
                .overrideDescription(blankToNull(form.getOverrideDescription()))
                .submissionType(form.getSubmissionType() == null || form.getSubmissionType().isBlank()
                        ? assignment.getDefaultSubmissionType()
                        : SubmissionTypes.parse(form.getSubmissionType()))
                .startAt(form.startAt())
                .endAt(form.endAt())
                .allowLate(form.isAllowLate())
                .allowResubmit(form.isAllowResubmit())
                .maxResubmit(form.normalizedMaxResubmit())
                .autoGrading(form.isAutoGrading())
                .score(form.getScore())
                .passScore(form.getPassScore())
                .grader(lookupRepository.userRef(form.getGraderId()))
                .status(form.toStatus())
                .build();

        form.toCriteria().forEach(ca::addCriteria);
        return courseAssignmentRepository.save(ca).getId();
    }

    @Transactional
    public void update(Long id, CourseAssignmentForm form) {
        validate(form);
        CourseAssignment ca = getOrThrow(id);
        ca.update(
                blankToNull(form.getOverrideDescription()),
                form.getSubmissionType() == null || form.getSubmissionType().isBlank()
                        ? ca.getSubmissionType()
                        : SubmissionTypes.parse(form.getSubmissionType()),
                form.startAt(), form.endAt(),
                form.isAllowLate(), form.isAllowResubmit(), form.normalizedMaxResubmit(),
                form.isAutoGrading(), form.getScore(), form.getPassScore(),
                lookupRepository.userRef(form.getGraderId()),
                form.toStatus());

        // 채점 기준을 통째로 교체한다. clear() 후 바로 add() 하면 Hibernate 가 orphan DELETE
        // 보다 INSERT 를 먼저 내보내 uk_criteria_seq(course_assignment_id, seq) 에 걸린다.
        // 그래서 삭제를 먼저 flush 한 뒤에 넣는다. (QuestionService.update() 와 같은 이유)
        ca.clearCriteria();
        courseAssignmentRepository.flush();
        for (AssignmentCriteria criteria : form.toCriteria()) {
            ca.addCriteria(criteria);
        }
    }

    /** 선택 비활성화 — 화면의 "선택한 과제 비활성화". 마감 처리로 본다. */
    @Transactional
    public void close(List<Long> ids) {
        courseAssignmentRepository.findAllById(ids)
                .forEach(ca -> ca.changeStatus(CourseAssignment.CourseAssignmentStatus.CLOSED));
    }

    /** soft delete (@SQLDelete). 제출물은 물리적으로 남는다 (3년 보존). */
    @Transactional
    public void delete(List<Long> ids) {
        courseAssignmentRepository.deleteAllById(ids);
    }

    /* ===== 내부 ===== */

    /**
     * 채점 기준 배점 합계는 배점과 같아야 한다.
     * 기준을 하나도 입력하지 않은 경우(총점 채점)는 허용한다.
     */
    private void validate(CourseAssignmentForm form) {
        if (form.getEndDate().isBefore(form.getStartDate())) {
            throw new IllegalArgumentException("종료일이 시작일보다 빠릅니다.");
        }
        List<AssignmentCriteria> criteria = form.toCriteria();
        if (criteria.isEmpty()) {
            return;
        }
        int sum = criteria.stream().mapToInt(AssignmentCriteria::getScore).sum();
        if (sum != form.getScore()) {
            throw new IllegalArgumentException(
                    "평가 기준 배점 합계(" + sum + ")가 과제 배점(" + form.getScore() + ")과 다릅니다.");
        }
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private static Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return map;
    }
}
