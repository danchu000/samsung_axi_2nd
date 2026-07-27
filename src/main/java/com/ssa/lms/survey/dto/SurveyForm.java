package com.ssa.lms.survey.dto;

import com.ssa.lms.survey.entity.Survey;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 설문 등록/수정 폼.
 *
 * 화면 admin-attendance-survey-add.html 의 입력 필드에 대응한다.
 * HTML 의 id 속성(surveyTitle, surveyStatus, dueDate, sessionLink 등)은 그대로 두고
 * name 만 camelCase 로 맞췄다 — 해당 페이지 JS 는 getElementById 로만 접근한다.
 *
 * 문항은 화면에서 동적으로 추가되므로 questions[i].* 로 인덱스 바인딩된다.
 * Spring 의 autoGrowNestedPaths 가 리스트를 자동으로 늘려준다.
 */
@Getter
@Setter
public class SurveyForm {

    private Long id;

    @NotBlank(message = "설문명을 입력하세요.")
    private String title;

    /** 화면 라디오 값: 만족도 / 평가 / 기타 (enum 이름도 허용). */
    @NotBlank(message = "설문 유형을 선택하세요.")
    private String surveyType = "만족도";

    /** null = 전체 대상 설문. */
    private Long courseId;

    /** 연계 차시. 화면 sessionLink. */
    private Long sessionId;

    /** 시작일. 비우면 오늘로 본다. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    /** 마감일. 화면 dueDate. */
    @NotNull(message = "마감일을 입력하세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    /** 화면 required 라디오. */
    private boolean required = true;

    /** 화면 completionReflect 라디오. */
    private boolean reflectCompletion;

    /** 익명 수집 여부. reflectCompletion 과 동시에 true 일 수 없다(서비스에서 검증). */
    private boolean anonymous;

    /** 화면 surveyStatus 값: active(활성화) / inactive(비활성화), 또는 enum 이름. */
    private String status = "active";

    @Valid
    private List<SurveyQuestionForm> questions = new ArrayList<>();

    /* ===== 변환 ===== */

    public Survey.SurveyType toSurveyType() {
        return switch (surveyType.trim()) {
            case "만족도", "과정 만족도", "COURSE_SATISFACTION" -> Survey.SurveyType.COURSE_SATISFACTION;
            case "평가", "강의 피드백", "LECTURE_FEEDBACK" -> Survey.SurveyType.LECTURE_FEEDBACK;
            case "시설", "시설 만족도", "FACILITY" -> Survey.SurveyType.FACILITY;
            case "수료", "수료 전 최종 설문", "COMPLETION" -> Survey.SurveyType.COMPLETION;
            case "기타", "ETC" -> Survey.SurveyType.ETC;
            default -> throw new IllegalArgumentException("알 수 없는 설문 유형: " + surveyType);
        };
    }

    /**
     * 화면의 활성화/비활성화 토글을 설문 상태로 옮긴다.
     *
     * 화면에는 상태 칸이 "활성화 / 비활성화" 두 개뿐인데 엔티티는 4상태
     * (DRAFT/SCHEDULED/ONGOING/CLOSED)다. 그래서 비활성화는 DRAFT 로 두고,
     * 활성화는 기간으로 SCHEDULED / ONGOING / CLOSED 를 자동 판정한다
     * (화면의 "상태 (자동 설정)" 문구가 이 동작을 가리킨다).
     */
    public Survey.SurveyStatus toStatus(LocalDateTime now) {
        String s = status == null ? "active" : status.trim();
        if ("inactive".equalsIgnoreCase(s) || "비활성화".equals(s) || "DRAFT".equals(s)) {
            return Survey.SurveyStatus.DRAFT;
        }
        // enum 이름을 그대로 넘긴 경우는 존중한다 (시드·API 용)
        if ("SCHEDULED".equals(s) || "ONGOING".equals(s) || "CLOSED".equals(s)) {
            return Survey.SurveyStatus.valueOf(s);
        }
        LocalDateTime start = startAtValue();
        LocalDateTime end = endAtValue();
        if (now.isBefore(start)) {
            return Survey.SurveyStatus.SCHEDULED;
        }
        if (now.isAfter(end)) {
            return Survey.SurveyStatus.CLOSED;
        }
        return Survey.SurveyStatus.ONGOING;
    }

    public LocalDateTime startAtValue() {
        LocalDate d = startDate != null ? startDate : LocalDate.now();
        return d.atStartOfDay();
    }

    /** 마감일은 그날 23:59:59 까지 열어 둔다 (화면이 날짜만 받기 때문). */
    public LocalDateTime endAtValue() {
        return dueDate.atTime(23, 59, 59);
    }

    public static SurveyForm from(Survey survey) {
        SurveyForm form = new SurveyForm();
        form.id = survey.getId();
        form.title = survey.getTitle();
        form.surveyType = switch (survey.getSurveyType()) {
            case COURSE_SATISFACTION -> "만족도";
            case LECTURE_FEEDBACK -> "평가";
            case FACILITY -> "시설";
            case COMPLETION -> "수료";
            case ETC -> "기타";
        };
        form.courseId = survey.getCourse() == null ? null : survey.getCourse().getId();
        form.sessionId = survey.getSession() == null ? null : survey.getSession().getId();
        form.startDate = survey.getStartAt().toLocalDate();
        form.dueDate = survey.getEndAt().toLocalDate();
        form.required = survey.isRequired();
        form.reflectCompletion = survey.isReflectCompletion();
        form.anonymous = survey.isAnonymous();
        form.status = survey.getStatus() == Survey.SurveyStatus.DRAFT ? "inactive" : "active";
        form.questions = survey.getQuestions().stream()
                .map(SurveyQuestionForm::from)
                .collect(ArrayList::new, List::add, List::addAll);
        return form;
    }
}
