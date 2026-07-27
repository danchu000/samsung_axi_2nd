package com.ssa.lms.exam.entity;

import com.ssa.lms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시험에 편성된 문항 (수동 출제).
 *
 * 자동 출제 규칙(ExamQuestionRule)으로 뽑힌 문항도 확정 시점에 이 테이블로 내려온다.
 * 그래야 응시자에게 실제로 나간 문항이 무엇이었는지 3년간 재현 가능하다.
 */
@Entity
@Table(
        name = "exam_question",
        uniqueConstraints = @UniqueConstraint(name = "uk_exam_question", columnNames = {"exam_id", "question_id"}),
        indexes = @Index(name = "idx_exam_question_exam", columnList = "exam_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /** 출제 순서. randomOrder=true 여도 기준 순서로 보관한다. */
    @Column(name = "seq", nullable = false)
    private Integer seq;

    /** 이 시험에서만 적용할 배점. null 이면 Question.score 사용. */
    @Column(name = "score_override")
    private Integer scoreOverride;

    /** 자동 출제 규칙으로 편성된 문항인지 여부. */
    @Column(name = "from_rule", nullable = false)
    private boolean fromRule;

    @Builder
    public ExamQuestion(Question question, Integer seq, Integer scoreOverride, boolean fromRule) {
        this.question = question;
        this.seq = seq;
        this.scoreOverride = scoreOverride;
        this.fromRule = fromRule;
    }

    void assignExam(Exam exam) {
        this.exam = exam;
    }

    /** 실제 적용 배점. */
    public int resolveScore() {
        return scoreOverride != null ? scoreOverride : question.getScore();
    }
}
