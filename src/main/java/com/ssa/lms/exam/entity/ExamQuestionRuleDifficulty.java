package com.ssa.lms.exam.entity;

import com.ssa.lms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 자동 출제 규칙의 난이도별 문항 수.
 * 화면 addRuleDifficultyLevel{n} + addRuleDifficultyCount{n} 한 줄에 해당.
 */
@Entity
@Table(
        name = "exam_question_rule_difficulty",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rule_difficulty", columnNames = {"rule_id", "difficulty"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamQuestionRuleDifficulty extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private ExamQuestionRule rule;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", length = 10, nullable = false)
    private Difficulty difficulty;

    /** COUNT 는 SQL 함수명과 헷갈리므로 컬럼명은 question_count 로 둔다. */
    @Column(name = "question_count", nullable = false)
    private Integer questionCount;

    @Builder
    public ExamQuestionRuleDifficulty(Difficulty difficulty, Integer questionCount) {
        this.difficulty = difficulty;
        this.questionCount = questionCount;
    }

    void assignRule(ExamQuestionRule rule) {
        this.rule = rule;
    }
}
