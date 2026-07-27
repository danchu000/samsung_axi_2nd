package com.ssa.lms.survey.entity;

import com.ssa.lms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 설문 문항. 화면 admin-attendance-survey-add.html 의 questionModal (qText, qType).
 */
/*
 * soft delete 를 걸지 않는다.
 *
 * 설문 수정 시 문항·보기는 통째로 교체되는데, @SQLDelete 로 두면 DELETE 가
 * UPDATE is_deleted=true 로 바뀌어 행이 그대로 남는다. 그런데 유니크 제약
 * uk_survey_question_seq 는 is_deleted 를 보지 않으므로, 같은 seq 로 새 행을 넣는 순간
 * 반드시 충돌한다(실제로 500 발생). 부분 유니크 인덱스는 MySQL 이 지원하지 않는다.
 *
 * 이 둘은 독립적으로 조회되는 기록이 아니라 부모(Survey)에 종속된 값이고,
 * 보존 요건은 부모의 soft delete 로 충족된다. 그래서 물리 삭제를 쓴다.
 * (과제 슬라이스의 course_assignment 유니크 제약 충돌과 같은 구조의 문제다)
 */
@Entity
@Table(
        name = "survey_question",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_survey_question_seq", columnNames = {"survey_id", "seq"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @Column(name = "seq", nullable = false)
    private Integer seq;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", length = 20, nullable = false)
    private SurveyQuestionType questionType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    /** SCALE 형일 때 최대 점수(예: 5점 척도면 5). 그 외 null. */
    @Column(name = "scale_max")
    private Integer scaleMax;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seq ASC")
    private List<SurveyChoice> choices = new ArrayList<>();

    @Builder
    public SurveyQuestion(Integer seq, SurveyQuestionType questionType, String content,
                          boolean required, Integer scaleMax) {
        this.seq = seq;
        this.questionType = questionType;
        this.content = content;
        this.required = required;
        this.scaleMax = scaleMax;
    }

    void assignSurvey(Survey survey) {
        this.survey = survey;
    }

    public void addChoice(SurveyChoice choice) {
        this.choices.add(choice);
        choice.assignQuestion(this);
    }

    /** {@link Survey#clearQuestions()} 와 같은 이유로 flush 전에는 재삽입 금지. */
    public void clearChoices() {
        this.choices.clear();
    }

    public enum SurveyQuestionType {
        /** 단일 선택 */
        SINGLE,
        /** 복수 선택 */
        MULTI,
        /** 척도(만족도 n점) */
        SCALE,
        /** 주관식 서술 */
        TEXT
    }
}
