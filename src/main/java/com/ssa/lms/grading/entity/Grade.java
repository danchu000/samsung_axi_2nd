package com.ssa.lms.grading.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 학습자 × 평가 단위의 확정 성적.
 *
 * 매핑 근거: static/js/result-grading.js studentTableData 의
 *   status 값(미채점 / 채점중 / 채점완료 / 확정)과 GradeStatus 가 1:1 대응한다.
 *
 * 소유권 주의 (설계안 §9-2 결정):
 *  이 테이블은 개발자 B 가 소유한다. 개발자 A 의 이수(completion) 판정이 이 값을 읽어야 하므로,
 *  A 는 이 엔티티에 직접 접근하지 말고 B 가 제공하는 GradeQueryService 를 통해 조회한다.
 *
 * evalRefId 는 evalType 에 따라 exam.id 또는 course_assignment.id 를 가리키는 다형 참조라
 * DB FK 를 걸 수 없다. 대신 (evalType, evalRefId) 조합 유효성은 서비스 계층에서 검증한다.
 */
@Entity
@Table(
        name = "grade",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_grade_target", columnNames = {"user_id", "eval_type", "eval_ref_id"}),
        indexes = {
                @Index(name = "idx_grade_course_status", columnList = "course_id, status"),
                @Index(name = "idx_grade_user", columnList = "user_id"),
                @Index(name = "idx_grade_eval", columnList = "eval_type, eval_ref_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Grade extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 내역서 "훈련별 데이터 독립 수집" 요건. 집계 쿼리가 항상 과정 단위로 끊기도록 비정규화 보관. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "eval_type", length = 20, nullable = false)
    private EvalType evalType;

    /** evalType=EXAM 이면 exam.id, ASSIGNMENT 이면 course_assignment.id. */
    @Column(name = "eval_ref_id", nullable = false)
    private Long evalRefId;

    @Column(name = "auto_score")
    private Integer autoScore;

    @Column(name = "manual_score")
    private Integer manualScore;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "passed")
    private Boolean passed;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private GradeStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private User gradedBy;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    /** 확정자. 확정 이후에는 GradeHistory 를 남기지 않고는 점수를 바꿀 수 없다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Builder
    public Grade(User user, Course course, EvalType evalType, Long evalRefId, GradeStatus status) {
        this.user = user;
        this.course = course;
        this.evalType = evalType;
        this.evalRefId = evalRefId;
        this.status = status;
    }

    public void applyScore(Integer autoScore, Integer manualScore, Integer passScore,
                           User grader, LocalDateTime at) {
        this.autoScore = autoScore;
        this.manualScore = manualScore;
        this.totalScore = (autoScore == null ? 0 : autoScore) + (manualScore == null ? 0 : manualScore);
        this.passed = passScore != null && this.totalScore >= passScore;
        this.gradedBy = grader;
        this.gradedAt = at;
        this.status = GradeStatus.GRADED;
    }

    public void confirm(User confirmer, LocalDateTime at) {
        this.confirmedBy = confirmer;
        this.confirmedAt = at;
        this.status = GradeStatus.CONFIRMED;
    }

    public boolean isConfirmed() {
        return this.status == GradeStatus.CONFIRMED;
    }

    public enum EvalType {
        EXAM,
        ASSIGNMENT
    }

    /** 화면 값: 미채점 / 채점중 / 채점완료 / 확정 */
    public enum GradeStatus {
        UNGRADED,
        GRADING,
        GRADED,
        CONFIRMED
    }
}
