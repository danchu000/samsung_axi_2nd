package com.ssa.lms.grading.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 성적 변경 이력.
 *
 * 매핑 근거: static/js/assignments-grading.js gradingHistoryData
 *   ({ date, instructor, score: '65 → 75', result: '통과', reason: '기준 미달 예외 승인' })
 *   화면이 이미 "변경 이력" 탭을 갖고 있으므로 처음부터 필요한 테이블이다.
 *
 * 점수 정정은 내역서 증빙 관점에서 반드시 사유와 함께 남아야 한다. append-only.
 */
@Entity
@Table(name = "grade_history", indexes = @Index(name = "idx_grade_history_grade", columnList = "grade_id, changed_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GradeHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grade_id", nullable = false)
    private Grade grade;

    @Column(name = "before_score")
    private Integer beforeScore;

    @Column(name = "after_score")
    private Integer afterScore;

    @Column(name = "before_passed")
    private Boolean beforePassed;

    @Column(name = "after_passed")
    private Boolean afterPassed;

    /** 변경 사유. 화면 필수 입력. */
    @Column(name = "reason", length = 500, nullable = false)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Builder
    public GradeHistory(Grade grade, Integer beforeScore, Integer afterScore,
                        Boolean beforePassed, Boolean afterPassed, String reason,
                        User changedBy, LocalDateTime changedAt) {
        this.grade = grade;
        this.beforeScore = beforeScore;
        this.afterScore = afterScore;
        this.beforePassed = beforePassed;
        this.afterPassed = afterPassed;
        this.reason = reason;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }
}
