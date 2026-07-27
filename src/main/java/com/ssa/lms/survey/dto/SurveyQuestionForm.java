package com.ssa.lms.survey.dto;

import com.ssa.lms.survey.entity.SurveyQuestion;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 설문 문항 입력값. {@link SurveyForm#getQuestions()} 의 원소로 바인딩된다
 * (questions[0].content, questions[0].choices[0] ...).
 *
 * 화면 admin-attendance-survey-add.html 의 questionModal(qText, qType) + addChoice 에 대응한다.
 */
@Getter
@Setter
public class SurveyQuestionForm {

    /** 화면 select 값: scale / single / multi / text (enum 이름도 허용). */
    private String questionType = "scale";

    private String content;

    private boolean required = true;

    /** SCALE 형의 최대 점수. 비어 있으면 5점 척도로 본다. */
    private Integer scaleMax;

    /** SINGLE / MULTI 형의 보기. 빈 칸은 저장 시 걸러진다. */
    private List<String> choices = new ArrayList<>();

    public SurveyQuestion.SurveyQuestionType toQuestionType() {
        if (questionType == null) {
            throw new IllegalArgumentException("문항 유형을 선택하세요.");
        }
        return switch (questionType.trim().toLowerCase()) {
            case "single", "단일선택", "객관식(단일)" -> SurveyQuestion.SurveyQuestionType.SINGLE;
            case "multi", "복수선택", "객관식(복수)" -> SurveyQuestion.SurveyQuestionType.MULTI;
            case "scale", "척도", "척도형" -> SurveyQuestion.SurveyQuestionType.SCALE;
            case "text", "서술", "서술형" -> SurveyQuestion.SurveyQuestionType.TEXT;
            default -> throw new IllegalArgumentException("알 수 없는 문항 유형: " + questionType);
        };
    }

    /** 선택지가 필요한 유형인지. */
    public boolean needsChoices() {
        SurveyQuestion.SurveyQuestionType type = toQuestionType();
        return type == SurveyQuestion.SurveyQuestionType.SINGLE
                || type == SurveyQuestion.SurveyQuestionType.MULTI;
    }

    /** 공백 보기를 걸러낸 목록. */
    public List<String> cleanChoices() {
        List<String> result = new ArrayList<>();
        if (choices == null) {
            return result;
        }
        for (String c : choices) {
            if (c != null && !c.isBlank()) {
                result.add(c.strip());
            }
        }
        return result;
    }

    /** SCALE 형의 최대 점수 — 미입력이면 5. */
    public Integer resolvedScaleMax() {
        if (toQuestionType() != SurveyQuestion.SurveyQuestionType.SCALE) {
            return null;
        }
        return (scaleMax == null || scaleMax < 2) ? 5 : scaleMax;
    }

    public static SurveyQuestionForm from(SurveyQuestion q) {
        SurveyQuestionForm form = new SurveyQuestionForm();
        form.questionType = switch (q.getQuestionType()) {
            case SINGLE -> "single";
            case MULTI -> "multi";
            case SCALE -> "scale";
            case TEXT -> "text";
        };
        form.content = q.getContent();
        form.required = q.isRequired();
        form.scaleMax = q.getScaleMax();
        form.choices = q.getChoices().stream().map(c -> c.getContent()).collect(ArrayList::new, List::add, List::addAll);
        return form;
    }
}
