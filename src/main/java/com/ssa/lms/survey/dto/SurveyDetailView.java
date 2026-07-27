package com.ssa.lms.survey.dto;

import com.ssa.lms.survey.entity.Survey;
import com.ssa.lms.survey.entity.SurveyQuestion;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 훈련생 설문 응답 화면(survey-detail-page.html)에 내려주는 설문 1건.
 *
 * 원본 화면의 SURVEY_DETAIL 더미 객체와 같은 shape 이다. 화면 JS 는 유형을
 * CHOICE / TEXT / STAR 세 가지로만 다뤘는데, 서버는 SINGLE / MULTI / SCALE / TEXT
 * 네 가지를 내려준다 (MULTI 는 화면에서 체크박스로 렌더링).
 */
public record SurveyDetailView(
        String id,
        String title,
        String course,
        String deadline,
        boolean submitted,
        /** 지금 응답을 받을 수 있는지 — 기간 밖이거나 이미 제출했으면 false. */
        boolean answerable,
        boolean anonymous,
        List<Question> questions
) {

    public record Question(
            String id,
            /** SINGLE / MULTI / SCALE / TEXT */
            String type,
            boolean required,
            String title,
            /** SCALE 형의 최대 점수. 그 외 null. */
            Integer maxStars,
            List<Choice> options
    ) {
    }

    public record Choice(String id, String content) {
    }

    public static SurveyDetailView of(Survey s, boolean submitted, boolean answerable) {
        String courseText = s.getCourse() == null
                ? "전체"
                : s.getCourse().getCourseName()
                  + (s.getCourse().getCohort() == null ? "" : " / " + s.getCourse().getCohort());

        return new SurveyDetailView(
                String.valueOf(s.getId()),
                s.getTitle(),
                courseText,
                s.getEndAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                submitted,
                answerable,
                s.isAnonymous(),
                s.getQuestions().stream().map(SurveyDetailView::toQuestion).toList()
        );
    }

    private static Question toQuestion(SurveyQuestion q) {
        return new Question(
                String.valueOf(q.getId()),
                q.getQuestionType().name(),
                q.isRequired(),
                q.getContent(),
                q.getQuestionType() == SurveyQuestion.SurveyQuestionType.SCALE
                        ? (q.getScaleMax() == null ? 5 : q.getScaleMax())
                        : null,
                q.getChoices().stream()
                        .map(c -> new Choice(String.valueOf(c.getId()), c.getContent()))
                        .toList()
        );
    }
}
