package com.ssa.lms.exam.dto;

import com.ssa.lms.exam.entity.Answer;
import com.ssa.lms.exam.entity.ExamQuestion;
import com.ssa.lms.exam.entity.Question;
import com.ssa.lms.exam.entity.QuestionChoice;

import java.util.List;

/**
 * 응시 화면(do-test.html)의 문항 한 개.
 *
 * <b>Question 엔티티를 그대로 내리면 안 된다</b> — correctAnswer / explanation 이
 * HTML 에 실려 나가 응시자가 개발자도구로 정답을 볼 수 있다. 이 레코드에는 정답이 없다.
 * QuestionChoice.correct 도 담지 않는다.
 */
public record AttemptQuestionRow(
        /** 화면 JS 가 문자열로 비교하므로 문자열. */
        String questionId,
        int no,
        /** MULTIPLE_CHOICE | SHORT_ANSWER | CODING */
        String type,
        String typeLabel,
        String text,
        int score,
        List<ChoiceRow> choices,
        /** 임시저장된 선택 보기 id. 없으면 null. */
        String savedChoiceId,
        /** 임시저장된 주관식/코딩 답안. 없으면 null. */
        String savedText
) {

    public record ChoiceRow(String id, int seq, String content) {
    }

    public static AttemptQuestionRow of(ExamQuestion examQuestion, int no, Answer saved) {
        Question q = examQuestion.getQuestion();
        List<ChoiceRow> choices = q.getQuestionType() == Question.QuestionType.MULTIPLE_CHOICE
                ? q.getChoices().stream()
                        .map(c -> new ChoiceRow(String.valueOf(c.getId()), c.getSeq(), c.getContent()))
                        .toList()
                : List.of();

        QuestionChoice savedChoice = saved == null ? null : saved.getChoice();
        return new AttemptQuestionRow(
                String.valueOf(q.getId()),
                no,
                q.getQuestionType().name(),
                typeLabel(q.getQuestionType()),
                q.getQuestionText(),
                examQuestion.resolveScore(),
                choices,
                savedChoice == null ? null : String.valueOf(savedChoice.getId()),
                saved == null ? null : saved.getAnswerText()
        );
    }

    public static String typeLabel(Question.QuestionType type) {
        return switch (type) {
            case MULTIPLE_CHOICE -> "객관식";
            case SHORT_ANSWER -> "주관식";
            case CODING -> "코딩";
        };
    }
}
