package com.ssa.lms.completion.service;

import com.ssa.lms.attendance.service.AttendanceService;
import com.ssa.lms.completion.entity.*;
import com.ssa.lms.completion.repository.CompletionCriteriaRepository;
import com.ssa.lms.completion.repository.CompletionRepository;
import com.ssa.lms.completion.service.grade.GradeCompletionProvider;
import com.ssa.lms.completion.web.CompletionView;
import com.ssa.lms.content.service.ProgressQueryService;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이수 기준 설정 + 자동 판정.
 *
 * <p>판정 근거는 <b>진도</b>({@link ProgressQueryService}, P2-A 제공/로컬은 fallback 0),
 * <b>출결</b>({@link AttendanceService}, 이 트랙), <b>성적</b>({@link GradeCompletionProvider} seam, B 통합 전 fallback)
 * 이다. 기준({@link CompletionCriteria})을 충족하면 이수(PASS), 아니면 미이수(FAIL) 로 자동 판정하고,
 * 관리자가 확정(CONFIRMED)하면 이수증 발급 대상이 된다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompletionService {

    private final CompletionRepository completionRepository;
    private final CompletionCriteriaRepository criteriaRepository;
    private final CourseRepository courseRepository;
    private final CourseQueryService courseQueryService;
    private final UserRepository userRepository;
    private final ProgressQueryService progressQueryService;
    private final AttendanceService attendanceService;
    private final GradeCompletionProvider gradeCompletionProvider;

    /* ===== 이수 기준 ===== */

    /**
     * 과정 이수 기준 — 저장된 값이 없으면 과정의 진도 기준({@code completionProgressRate})을 초기값으로 한
     * (미저장) 기본 기준을 돌려준다.
     */
    public CompletionCriteria criteriaOf(Long courseId) {
        return criteriaRepository.findByCourseId(courseId)
                .orElseGet(() -> {
                    Course course = getCourse(courseId);
                    return CompletionCriteria.builder()
                            .course(course)
                            .minProgressRate(course.getCompletionProgressRate())
                            .minAttendanceRate(80)
                            .requireGradePass(false)
                            .build();
                });
    }

    @Transactional
    public void saveCriteria(Long courseId, int minProgressRate, int minAttendanceRate,
                             boolean requireGradePass, String note) {
        criteriaRepository.findByCourseId(courseId).ifPresentOrElse(
                c -> c.update(minProgressRate, minAttendanceRate, requireGradePass, note),
                () -> criteriaRepository.save(CompletionCriteria.builder()
                        .course(getCourse(courseId))
                        .minProgressRate(minProgressRate).minAttendanceRate(minAttendanceRate)
                        .requireGradePass(requireGradePass).note(note).build()));
    }

    /* ===== 자동 판정 ===== */

    /**
     * 과정 수강생 전원을 대상으로 이수 자동 판정(재)실행. 이미 확정(CONFIRMED)된 건은 확정 상태를 유지하고
     * 판정 근거 스냅샷만 갱신한다.
     *
     * @return 판정된 수강생 수
     */
    @Transactional
    public int evaluate(Long courseId) {
        Course course = getCourse(courseId);
        CompletionCriteria criteria = criteriaOf(courseId);
        List<User> trainees = userRepository.findAllById(courseQueryService.findUserIdsByCourseId(courseId));
        LocalDateTime now = LocalDateTime.now();

        for (User trainee : trainees) {
            int progressRate = progressQueryService.completedRatio(trainee.getId(), courseId);
            int attendanceRate = attendanceService.attendanceRate(trainee.getId(), courseId);

            Boolean gradesConfirmed = null;
            boolean gradesOk = true;
            if (criteria.isRequireGradePass()) {
                gradesConfirmed = gradeCompletionProvider.gradesConfirmed(trainee.getId(), courseId);
                gradesOk = gradesConfirmed;
            }

            boolean pass = progressRate >= criteria.getMinProgressRate()
                    && attendanceRate >= criteria.getMinAttendanceRate()
                    && gradesOk;
            CompletionResult result = pass ? CompletionResult.PASS : CompletionResult.FAIL;

            Completion completion = completionRepository.findByCourseIdAndTraineeId(courseId, trainee.getId())
                    .orElse(null);
            if (completion == null) {
                completionRepository.save(Completion.builder()
                        .course(course).trainee(trainee)
                        .progressRate(progressRate).attendanceRate(attendanceRate)
                        .gradesConfirmed(gradesConfirmed)
                        .result(result).confirmStatus(ConfirmStatus.EXPECTED).evaluatedAt(now).build());
            } else {
                completion.applyEvaluation(progressRate, attendanceRate, gradesConfirmed, result, now);
            }
        }
        return trainees.size();
    }

    /* ===== 확정 ===== */

    @Transactional
    public void confirm(Long completionId) {
        get(completionId).confirm(LocalDateTime.now());
    }

    @Transactional
    public void changeConfirmStatus(Long completionId, ConfirmStatus status) {
        get(completionId).changeConfirmStatus(status, LocalDateTime.now());
    }

    /* ===== 조회 ===== */

    public List<Completion> findByCourse(Long courseId) {
        return completionRepository.findByCourseId(courseId);
    }

    /** 이수 관리 화면용 행 뷰(트랜잭션 안에서 지연 로딩 필드 materialize — open-in-view=false). */
    public List<CompletionView> viewsByCourse(Long courseId) {
        return completionRepository.findByCourseId(courseId).stream()
                .map(CompletionView::of).toList();
    }

    /** 수강생 본인 이수 현황 행 뷰(훈련생 이수관리 화면). */
    public List<CompletionView> viewsByTrainee(Long traineeId) {
        return completionRepository.findByTraineeId(traineeId).stream()
                .map(CompletionView::of).toList();
    }

    /**
     * 이수 정보가 해당 수강생 본인의 것인지 — 훈련생 이수증 다운로드 권한 경계.
     * 존재하지 않거나 남의 것이면 false(호출부에서 404 처리해 존재 여부 노출을 막는다).
     */
    public boolean isOwnedByTrainee(Long completionId, Long traineeId) {
        return completionRepository.findById(completionId)
                .map(c -> c.getTrainee().getId().equals(traineeId))
                .orElse(false);
    }

    public Completion get(Long completionId) {
        return completionRepository.findById(completionId)
                .orElseThrow(() -> new IllegalArgumentException("이수 정보를 찾을 수 없습니다: " + completionId));
    }

    private Course getCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("과정을 찾을 수 없습니다: " + courseId));
    }
}
