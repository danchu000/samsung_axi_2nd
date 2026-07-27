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
