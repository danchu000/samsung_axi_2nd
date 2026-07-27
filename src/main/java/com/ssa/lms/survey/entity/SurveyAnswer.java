package com.ssa.lms.survey.entity;

import com.ssa.lms.common.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 설문 문항별 응답값.
 *
 * MULTI(복수 선택)는 선택한 보기 수만큼 행이 생긴다. 그래서 (response, question) 유니크를
 * 걸지 않고 (response, question, choice) 로 건다.
 */
@Entity
@SQLDelete(sql = "UPDATE survey_answer SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Table(
        name = "survey_answer",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_survey_answer", columnNames = {"response_id", "question_id", "choice_id"}),
        indexes = @Index(name = "idx_survey_answer_question", columnList = "question_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "response_id", nullable = false)
    private SurveyResponse response;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private SurveyQuestion question;

    /** SINGLE / MULTI 형에서 선택한 보기. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "choice_id")
    private SurveyChoice choice;

    /** SCALE 형의 점수. */
    @Column(name = "scale_value")
    private Integer scaleValue;

    /** TEXT 형의 서술 답변. */
    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Builder
    public SurveyAnswer(SurveyQuestion question, SurveyChoice choice,
                        Integer scaleValue, String answerText) {
        this.question = question;
        this.choice = choice;
        this.scaleValue = scaleValue;
        this.answerText = answerText;
    }

    void assignResponse(SurveyResponse response) {
        this.response = response;
    }
}
