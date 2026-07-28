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
 *
 * <p><b>유니크 제약에 set_no 가 들어간 이유</b> — 문제 세트를 여러 벌 만들면 문제은행이 모자랄 때
 * 같은 문제가 서로 다른 세트에 들어갈 수 있다. (exam_id, question_id) 로만 잡으면 그 순간 500 이 난다.
 * 한 세트 안에서 같은 문제가 두 번 나오는 것만 막으면 된다.</p>
 */
@Entity
@Table(
        name = "exam_question",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_exam_question", columnNames = {"exam_id", "set_no", "question_id"}),
        indexes = @Index(name = "idx_exam_question_exam", columnList = "exam_id, set_no")
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

    /**
     * 이 문항이 속한 문제 세트 번호 (1부터).
     *
     * <p>{@code Exam.questionSetCount == 1} 이면 전부 1 이라 세트 기능이 없던 때와 같다.
     * 응시자에게 실제로 배정된 세트는 {@code ExamAttempt.assignedSetNo} 에 남는다.</p>
     */
    @Column(name = "set_no", nullable = false)
    private Integer setNo;

    /** 출제 순서. randomOrder=true 여도 기준 순서로 보관한다. 세트 안에서의 순서다. */
    @Column(name = "seq", nullable = false)
    private Integer seq;

    /** 이 시험에서만 적용할 배점. null 이면 Question.score 사용. */
    @Column(name = "score_override")
    private Integer scoreOverride;

    /** 자동 출제 규칙으로 편성된 문항인지 여부. */
    @Column(name = "from_rule", nullable = false)
    private boolean fromRule;

    @Builder
    public ExamQuestion(Question question, Integer setNo, Integer seq, Integer scoreOverride, boolean fromRule) {
        this.question = question;
        // 값을 안 넘기는 기존 호출부(시드·테스트)는 전부 1번 세트다.
        this.setNo = (setNo == null || setNo < 1) ? 1 : setNo;
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
