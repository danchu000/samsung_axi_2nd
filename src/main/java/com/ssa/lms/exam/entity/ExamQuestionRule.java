package com.ssa.lms.exam.entity;

import com.ssa.lms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 자동 출제 규칙.
 *
 * 매핑 근거: admin-evaluation-test-add.html 의 addRuleModal
 *  (addRuleSubject / addRuleTopic / addRuleSubtopic / addRuleTags / addRuleCount
 *   + addRuleDifficultyLevel1~3, addRuleDifficultyCount1~3)
 *
 * 난이도별 개수는 화면이 3줄을 받으므로 자식 테이블(ExamQuestionRuleDifficulty)로 분리.
 */
@Entity
@Table(name = "exam_question_rule", indexes = @Index(name = "idx_exam_rule_exam", columnList = "exam_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamQuestionRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    /** 대분류 (화면 addRuleSubject). */
    @Column(name = "category_l", length = 50)
    private String categoryL;

    /** 중분류 (화면 addRuleTopic). */
    @Column(name = "category_m", length = 50)
    private String categoryM;

    /** 소분류 (화면 addRuleSubtopic). */
    @Column(name = "category_s", length = 50)
    private String categoryS;

    /** 쉼표 구분 태그. Question.tags 와 매칭. */
    @Column(name = "tags", length = 255)
    private String tags;

    /** 이 규칙으로 뽑을 총 문항 수. */
    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamQuestionRuleDifficulty> difficulties = new ArrayList<>();

    @Builder
    public ExamQuestionRule(String categoryL, String categoryM, String categoryS,
                           String tags, Integer totalCount) {
        this.categoryL = categoryL;
        this.categoryM = categoryM;
        this.categoryS = categoryS;
        this.tags = tags;
        this.totalCount = totalCount;
    }

    void assignExam(Exam exam) {
        this.exam = exam;
    }

    public void addDifficulty(ExamQuestionRuleDifficulty difficulty) {
        this.difficulties.add(difficulty);
        difficulty.assignRule(this);
    }
}
