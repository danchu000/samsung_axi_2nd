package com.ssa.lms.survey.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 설문 응답 1건(= 응답자 1명의 제출).
 *
 * 익명 설문(Survey.anonymous=true)이면 user 를 null 로 저장한다. 다만 이수 반영
 * (Survey.reflectCompletion=true)과 익명은 동시에 성립할 수 없다 —
 * 누가 냈는지 몰라야 하는데 누가 냈는지로 이수를 판정할 수는 없기 때문이다.
 * 서비스 계층에서 이 조합을 막는다.
 */
@Entity
@SQLDelete(sql = "UPDATE survey_response SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Table(
        name = "survey_response",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_survey_response_user", columnNames = {"survey_id", "user_id"}),
        indexes = @Index(name = "idx_response_survey", columnList = "survey_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyResponse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    /** 익명 설문이면 null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "response", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SurveyAnswer> answers = new ArrayList<>();

    @Builder
    public SurveyResponse(Survey survey, User user, LocalDateTime submittedAt) {
        this.survey = survey;
        this.user = user;
        this.submittedAt = submittedAt;
    }

    public void addAnswer(SurveyAnswer answer) {
        this.answers.add(answer);
        answer.assignResponse(this);
    }
}
