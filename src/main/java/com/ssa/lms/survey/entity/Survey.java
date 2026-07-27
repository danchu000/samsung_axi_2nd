package com.ssa.lms.survey.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.Session;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 설문지.
 *
 * 매핑 근거
 *  - IA "데이터 정리" 8행 (상태/설문명/유형/연계 차시/이수 반영/필수/응답률/마감일)
 *  - templates/admin/admin-05-attendance/admin-attendance-survey-add.html
 *    (surveyTitle, surveyType, surveyStatus, sessionLink, completionReflect, required, dueDate)
 *  - static/js/trainee/surveys.js SURVEYS
 *
 * 상태 주의: 훈련생 화면의 SUBMITTED / CLOSED_UNANSWERED 는 컬럼이 아니다.
 * (survey.status, 그리고 해당 사용자의 SurveyResponse 존재 여부)의 조합으로 만드는 파생 상태다.
 */
@Entity
@Table(
        name = "survey",
        indexes = {
                @Index(name = "idx_survey_course", columnList = "course_id"),
                @Index(name = "idx_survey_status", columnList = "status"),
                @Index(name = "idx_survey_period", columnList = "start_at, end_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Survey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "survey_type", length = 20, nullable = false)
    private SurveyType surveyType;

    /** null = 전체 대상 설문(화면의 과정 "전체"). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    /** 연계 차시. 화면 sessionLink. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    /** 응답 필수 여부. 화면 required. */
    @Column(name = "is_required", nullable = false)
    private boolean required;

    /** 이수 판정에 반영할지. 화면 completionReflect. A 의 이수 로직이 이 플래그를 읽는다. */
    @Column(name = "reflect_completion", nullable = false)
    private boolean reflectCompletion;

    /** 응답을 익명으로 수집할지. 익명이면 SurveyResponse.user 를 저장하지 않는다. */
    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SurveyStatus status;

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seq ASC")
    private List<SurveyQuestion> questions = new ArrayList<>();

    @Builder
    public Survey(String title, SurveyType surveyType, Course course, Session session,
                  boolean required, boolean reflectCompletion, boolean anonymous,
                  LocalDateTime startAt, LocalDateTime endAt, SurveyStatus status) {
        this.title = title;
        this.surveyType = surveyType;
        this.course = course;
        this.session = session;
        this.required = required;
        this.reflectCompletion = reflectCompletion;
        this.anonymous = anonymous;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
    }

    public void addQuestion(SurveyQuestion question) {
        this.questions.add(question);
        question.assignSurvey(this);
    }

    public boolean isOpenAt(LocalDateTime at) {
        return status == SurveyStatus.ONGOING && !at.isBefore(startAt) && !at.isAfter(endAt);
    }

    public enum SurveyType {
        /** 과정 만족도 */
        COURSE_SATISFACTION,
        /** 강의 피드백 */
        LECTURE_FEEDBACK,
        /** 시설 만족도 */
        FACILITY,
        /** 수료 전 최종 설문 */
        COMPLETION,
        ETC
    }

    public enum SurveyStatus {
        DRAFT,
        SCHEDULED,
        ONGOING,
        CLOSED
    }
}
