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
 * 설문 문항의 보기. 화면의 addChoice 로 동적 추가되므로 개수 제한 없음.
 */
@Entity
@SQLDelete(sql = "UPDATE survey_choice SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Table(
        name = "survey_choice",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_survey_choice_seq", columnNames = {"question_id", "seq"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyChoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private SurveyQuestion question;

    @Column(name = "seq", nullable = false)
    private Integer seq;

    @Column(name = "content", length = 500, nullable = false)
    private String content;

    @Builder
    public SurveyChoice(Integer seq, String content) {
        this.seq = seq;
        this.content = content;
    }

    void assignQuestion(SurveyQuestion question) {
        this.question = question;
    }
}
