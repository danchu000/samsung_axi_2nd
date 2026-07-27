package com.ssa.lms.grading.service;

import com.ssa.lms.grading.dto.GradeSummary;
import com.ssa.lms.grading.entity.Grade;
import com.ssa.lms.grading.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 성적 조회 계약 — **개발자 A의 이수(completion) 판정이 호출하는 진입점**이다.
 * (a-requests.md P1-8에서 B가 제공하기로 한 것)
 *
 * <p>`grade` / `grade_history` 테이블은 B 소유다. A는 {@code GradeRepository} 나
 * {@code Grade} 엔티티를 직접 쓰지 말고 이 서비스만 호출할 것. 채점 규칙(부분점수,
 * 재응시 시 어느 회차를 반영할지, 성적 정정 이력)이 전부 B쪽에 있어서, 직접 조회하면
 * 이수 판정이 조용히 틀린 값을 쓰게 된다.</p>
 *
 * <p><b>필요한 시그니처가 더 있으면 B에게 요청하면 추가한다.</b> 여기 없는 걸
 * 리포지토리로 우회하지 말 것.</p>
 *
 * <p>주의: 설문의 이수 반영은 이 서비스가 아니라 {@code Survey.reflectCompletion}
 * 플래그로 별도 판정한다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeQueryService {

    private final GradeRepository gradeRepository;

    /**
     * 해당 훈련생이 그 과정에서 **확정(CONFIRMED)** 받은 성적 전부.
     * 미채점·채점중·채점완료 상태는 제외한다 — 확정 전 점수로 이수를 판정하면 안 된다.
     */
    public List<GradeSummary> findConfirmedGrades(Long userId, Long courseId) {
        return gradeRepository
                .findByUserIdAndCourseIdAndStatus(userId, courseId, Grade.GradeStatus.CONFIRMED)
                .stream()
                .map(GradeSummary::from)
                .toList();
    }

    /**
     * 평가 1건의 성적. 시험 채점 슬라이스에서 추가한 조회다 (기존 시그니처는 건드리지 않았다).
     *
     * @param evalRefId evalType 에 따라 exam.id 또는 course_assignment.id
     */
    public java.util.Optional<GradeSummary> findGrade(Long userId, Grade.EvalType evalType, Long evalRefId) {
        return gradeRepository.findByUserIdAndEvalTypeAndEvalRefId(userId, evalType, evalRefId)
                .map(GradeSummary::from);
    }

    /** 확정 여부와 무관하게 그 과정의 성적 전부. 진행 현황 표시용. */
    public List<GradeSummary> findAllGrades(Long userId, Long courseId) {
        return gradeRepository.findByUserIdAndCourseId(userId, courseId).stream()
                .map(GradeSummary::from)
                .toList();
    }

    /**
     * 그 과정의 모든 평가 성적이 확정됐는지.
     *
     * <p>성적 행이 하나도 없으면 {@code false} 를 돌려준다. "평가가 없어서 자동 이수"가
     * 되어버리면 안 되기 때문이다 — 평가가 실제로 없는 과정이라면 이수 기준 쪽에서
     * 성적을 요구하지 않도록 설정해야 한다.</p>
     */
    public boolean hasAllRequiredGradesConfirmed(Long userId, Long courseId) {
        List<Grade> grades = gradeRepository.findByUserIdAndCourseId(userId, courseId);
        if (grades.isEmpty()) {
            return false;
        }
        return gradeRepository.countUnconfirmed(userId, courseId) == 0;
    }

    /** 확정된 성적 중 불합격이 하나라도 있는지. 이수 부결 사유 표시용. */
    public boolean hasFailedGrade(Long userId, Long courseId) {
        return findConfirmedGrades(userId, courseId).stream()
                .anyMatch(g -> Boolean.FALSE.equals(g.passed()));
    }

    /**
     * 확정 성적의 평균 점수. 확정된 성적이 없으면 {@code null}.
     * 이수 기준의 "평균 n점 이상" 판정에 쓴다.
     */
    public Double averageConfirmedScore(Long userId, Long courseId) {
        return findConfirmedGrades(userId, courseId).stream()
                .map(GradeSummary::totalScore)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .stream().boxed().findFirst().orElse(null);
    }
}
