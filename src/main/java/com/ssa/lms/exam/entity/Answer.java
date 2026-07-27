package com.ssa.lms.exam.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 응시자의 문항별 답안.
 *
 * 임시저장과 최종제출이 같은 행을 upsert 한다. 제출 여부는 이 테이블이 아니라
 * ExamAttempt.status 로만 판정한다. (임시저장된 답안도 감사 목적상 남겨야 함)
 */
@Entity
@Table(
        name = "answer",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_answer_attempt_question", columnNames = {"attempt_id", "question_id"}),
        indexes = @Index(name = "idx_answer_attempt", columnList = "attempt_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /** 객관식 선택 보기. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "choice_id")
    private QuestionChoice choice;

    /** 주관식/코딩 답안 본문. */
    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "is_correct")
    private Boolean correct;

    @Column(name = "score")
    private Integer score;

    /** 자동 채점으로 매겨진 점수인지 여부. */
    @Column(name = "auto_graded", nullable = false)
    private boolean autoGraded;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private User gradedBy;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    /** 마지막 임시저장 시각. */
    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    @Builder
    public Answer(ExamAttempt attempt, Question question, QuestionChoice choice,
                  String answerText, LocalDateTime savedAt) {
        this.attempt = attempt;
        this.question = question;
        this.choice = choice;
        this.answerText = answerText;
        this.savedAt = savedAt;
    }

    /** 임시저장 갱신. 채점 결과는 건드리지 않는다. */
    public void updateAnswer(QuestionChoice choice, String answerText, LocalDateTime savedAt) {
        this.choice = choice;
        this.answerText = answerText;
        this.savedAt = savedAt;
    }

    public void gradeAuto(boolean correct, int score, LocalDateTime at) {
        this.correct = correct;
        this.score = score;
        this.autoGraded = true;
        this.gradedAt = at;
    }

    public void gradeManual(User grader, Integer score, Boolean correct, LocalDateTime at) {
        this.gradedBy = grader;
        this.score = score;
        this.correct = correct;
        this.autoGraded = false;
        this.gradedAt = at;
    }
}
