package com.ssa.lms.dashboard.service;

import com.ssa.lms.completion.entity.Completion;
import com.ssa.lms.completion.entity.CompletionCriteria;
import com.ssa.lms.completion.entity.CompletionResult;
import com.ssa.lms.completion.repository.CompletionRepository;
import com.ssa.lms.completion.service.CompletionService;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.course.service.CourseService;
import com.ssa.lms.dashboard.dto.CoursePaceView;
import com.ssa.lms.dashboard.dto.DashboardMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

/**
 * 대시보드 차트 지표 계산.
 *
 * <p>관리자는 전체 과정, 강사는 담당 과정 — <b>범위만 다르고 계산은 같다</b>.
 * 계산이 갈리면 같은 훈련생을 두고 관리자와 강사가 다른 숫자를 보게 된다.
 * 그래서 이 서비스 하나만 두고 과정 목록만 다르게 넘긴다.</p>
 *
 * <p><b>없는 값을 만들어내지 않는다.</b> 진도 데이터가 없으면 그 과정을 빼고,
 * 이수 판정 이력이 없으면 빈 목록을 돌려준다. 화면은 "아직 없습니다"를 보여준다 —
 * 0으로 채우면 "0%"인지 "모름"인지 구분이 안 된다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardMetricsService {

    /** 진도·이수 판정에 인정하는 수강 상태. 다른 판정과 같은 기준을 쓴다. */
    private static final Set<EnrollmentStatus> COUNTED =
            EnumSet.of(EnrollmentStatus.APPROVED, EnrollmentStatus.COMPLETED);

    private final EnrollmentRepository enrollmentRepository;
    private final CompletionRepository completionRepository;
    private final CompletionService completionService;
    private final CourseService courseService;
    private final CourseQueryService courseQueryService;

    /** 관리자 — 전체 과정. */
    public DashboardMetrics forAdmin() {
        return of(courseService.findAll());
    }

    /**
     * 강사 — 담당 과정만.
     * 남의 과정 수치가 섞이면 안 된다. 담당 판정은 A 의 공식 조회를 쓴다.
     */
    public DashboardMetrics forInstructor(Long instructorId) {
        return of(courseService.findAll().stream()
                .filter(c -> courseQueryService.isInstructorOf(instructorId, c.getId()))
                .toList());
    }

    /**
     * 훈련생 — 우리 반에서 내 학습 위치.
     *
     * <p>수강 중인 과정 중 <b>진도가 가장 많이 남은 과정</b> 하나만 돌려준다.
     * 여러 개를 한꺼번에 보여주면 "그래서 뭘 해야 하나"가 흐려진다.</p>
     *
     * <p>비교 기준은 진도율이다. 성적으로 줄 세우면 훈련생 사이에 위화감이 생기고,
     * 성적은 확정 전 값이 섞일 수 있다. 진도는 본인 노력으로 바로 바꿀 수 있는 값이다.</p>
     */
    public Optional<CoursePaceView> paceOf(Long traineeId) {
        return enrollmentRepository.findByTraineeIdOrderByAppliedAtDesc(traineeId).stream()
                .filter(e -> COUNTED.contains(e.getStatus()))
                .map(e -> paceIn(e.getCourse(), traineeId))
                .flatMap(Optional::stream)
                // 진도가 가장 낮은 과정 = 지금 가장 신경 써야 할 과정
                .min(Comparator.comparingInt(CoursePaceView::myProgress));
    }

    private Optional<CoursePaceView> paceIn(Course course, Long traineeId) {
        List<Enrollment> rows = enrollmentRepository
                .findByCourseIdOrderByAppliedAtDesc(course.getId()).stream()
                .filter(e -> COUNTED.contains(e.getStatus()))
                .sorted(Comparator.comparingDouble(Enrollment::getProgressRate).reversed())
                .toList();

        // 혼자면 순위라는 게 성립하지 않는다
        if (rows.size() < 2) return Optional.empty();

        int myIdx = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getTrainee().getId().equals(traineeId)) { myIdx = i; break; }
        }
        if (myIdx < 0) return Optional.empty();

        final int mine = myIdx;
        // 나를 뺀 나머지 — 이름은 담지 않는다. 트랙에 함께 그려야 달리기로 보인다
        List<Integer> others = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            if (i != mine) others.add(pct(rows.get(i)));
        }
        int avg = (int) Math.round(rows.stream().mapToDouble(Enrollment::getProgressRate).average().orElse(0));

        // 등수순 전체 주자 — 세로 트랙에 위(선두)에서 아래로 늘어놓는다
        List<CoursePaceView.Runner> runners = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            runners.add(new CoursePaceView.Runner(i + 1, pct(rows.get(i)), i == mine));
        }

        return Optional.of(new CoursePaceView(
                course.getCourseName(),
                myIdx + 1,
                rows.size(),
                pct(rows.get(myIdx)),
                myIdx > 0 ? pct(rows.get(myIdx - 1)) : null,
                myIdx < rows.size() - 1 ? pct(rows.get(myIdx + 1)) : null,
                pct(rows.get(0)),
                avg,
                others,
                runners));
    }

    private int pct(Enrollment e) {
        return (int) Math.round(e.getProgressRate());
    }

    public DashboardMetrics of(List<Course> courses) {
        if (courses == null || courses.isEmpty()) {
            return DashboardMetrics.empty();
        }
        return new DashboardMetrics(courseProgress(courses), completion(courses));
    }

    /* ===== 과정별 진행 현황 ===== */

    private List<DashboardMetrics.CourseProgress> courseProgress(List<Course> courses) {
        List<DashboardMetrics.CourseProgress> out = new ArrayList<>();

        for (Course c : courses) {
            List<Enrollment> enrolled = enrollmentRepository.findByCourseIdOrderByAppliedAtDesc(c.getId())
                    .stream()
                    .filter(e -> COUNTED.contains(e.getStatus()))
                    .toList();

            // 수강생이 없으면 평균 진도라는 것 자체가 없다. 0% 로 그리면 "진도 안 나감"으로 읽힌다.
            if (enrolled.isEmpty()) continue;

            int avgProgress = (int) Math.round(
                    enrolled.stream().mapToDouble(Enrollment::getProgressRate).average().orElse(0));

            out.add(new DashboardMetrics.CourseProgress(
                    c.getCourseName(), avgProgress, elapsedRate(c), enrolled.size()));
        }
        return out;
    }

    /**
     * 기간 경과율 — 오늘이 훈련 기간의 몇 %인지.
     *
     * <p>진도율과 나란히 놓고 봐야 "일정보다 뒤처졌는지"를 알 수 있다.
     * 시작 전이면 0, 끝났으면 100. 날짜가 없거나 뒤집혀 있으면 -1 을 돌려
     * 화면이 기준선을 그리지 않게 한다 — 엉뚱한 선이 그려지는 것보다 없는 게 낫다.</p>
     */
    private int elapsedRate(Course c) {
        LocalDate start = c.getStartDate();
        LocalDate end = c.getEndDate();
        if (start == null || end == null || !end.isAfter(start)) return -1;

        LocalDate today = LocalDate.now();
        if (!today.isAfter(start)) return 0;
        if (!today.isBefore(end)) return 100;

        long total = ChronoUnit.DAYS.between(start, end);
        long passed = ChronoUnit.DAYS.between(start, today);
        return (int) Math.round(passed * 100.0 / total);
    }

    /* ===== 이수 요건 충족 현황 ===== */

    /**
     * 요건별 충족 인원.
     *
     * <p><b>이수 판정이 실행된 훈련생만</b> 센다. 판정 전 인원까지 분모에 넣으면
     * 아직 평가하지 않은 사람이 "미충족"으로 잡혀 실제보다 나쁘게 보인다.</p>
     *
     * <p>기준(최소 진도율·출결률·성적 확정 필요 여부)은 과정마다 다를 수 있으므로
     * 과정별 기준으로 각각 판정한 뒤 합산한다.</p>
     */
    private List<DashboardMetrics.Requirement> completion(List<Course> courses) {
        int total = 0, progressOk = 0, attendanceOk = 0, gradeOk = 0, allOk = 0;

        for (Course c : courses) {
            List<Completion> rows = completionRepository.findByCourseId(c.getId());
            if (rows.isEmpty()) continue;

            CompletionCriteria criteria = completionService.criteriaOf(c.getId());
            int minProgress = criteria.getMinProgressRate();
            int minAttendance = criteria.getMinAttendanceRate();
            boolean requireGrade = criteria.isRequireGradePass();

            for (Completion r : rows) {
                total++;
                if (r.getProgressRate() >= minProgress) progressOk++;
                if (r.getAttendanceRate() >= minAttendance) attendanceOk++;
                // 성적 요건을 안 쓰는 과정이면 그 요건은 충족한 것으로 본다
                if (!requireGrade || Boolean.TRUE.equals(r.getGradesConfirmed())) gradeOk++;
                if (r.getResult() == CompletionResult.PASS) allOk++;
            }
        }

        if (total == 0) return List.of();   // 판정 이력이 없다 — 화면이 "없음"을 보여준다

        return List.of(
                new DashboardMetrics.Requirement("진도 충족", progressOk, total),
                new DashboardMetrics.Requirement("출석 충족", attendanceOk, total),
                new DashboardMetrics.Requirement("성적 충족", gradeOk, total),
                new DashboardMetrics.Requirement("전체 충족", allOk, total)
        );
    }
}
