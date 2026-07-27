package com.ssa.lms.exam.service;

import com.ssa.lms.exam.entity.Answer;
import com.ssa.lms.exam.entity.Question;
import com.ssa.lms.exam.entity.QuestionChoice;
import org.springframework.stereotype.Component;

/**
 * 자동 채점.
 *
 * {@link Question#isAutoGradable()} 가 true 인 유형(객관식·주관식)만 여기서 채점한다.
 * 코딩/서술형 수동 채점과 Grade 반영은 다음 슬라이스(시험 채점) 담당이므로 건드리지 않는다.
 *
 * 주관식 규칙 (Question 의 플래그를 존중):
 *  - caseSensitive=false → 대소문자 무시
 *  - allowPartial=true  → 완전 일치가 아니어도 정답 문자열을 포함하면 절반 점수(부분점수)
 *
 * 공백은 항상 정규화한다 (연속 공백 1개, 앞뒤 trim). 오탈자가 아니라 입력 습관 차이라 감점 근거가 없다.
 */
@Component
public class AutoGrader {

    /** 채점 결과 — 배점 대비 획득 점수와 정오답. */
    public record Result(int score, boolean correct) {
        static Result zero() {
            return new Result(0, false);
        }
    }

    /**
     * @param answer 임시저장된 답안. null 이면 미응답(0점).
     * @param maxScore 이 시험에서의 실제 배점 (ExamQuestion.resolveScore()).
     */
    public Result grade(Question question, Answer answer, int maxScore) {
        if (!question.isAutoGradable() || answer == null) {
            return Result.zero();
        }
        return switch (question.getQuestionType()) {
            case MULTIPLE_CHOICE -> gradeChoice(answer, maxScore);
            case SHORT_ANSWER -> gradeShortAnswer(question, answer, maxScore);
            case CODING -> Result.zero(); // isAutoGradable()=false 라 도달하지 않는다
        };
    }

    /* ===== 내부 ===== */

    private Result gradeChoice(Answer answer, int maxScore) {
        QuestionChoice choice = answer.getChoice();
        if (choice == null) {
            return Result.zero();
        }
        return choice.isCorrect() ? new Result(maxScore, true) : Result.zero();
    }

    private Result gradeShortAnswer(Question question, Answer answer, int maxScore) {
        String submitted = normalize(answer.getAnswerText());
        String correct = normalize(question.getCorrectAnswer());
        if (submitted.isEmpty() || correct.isEmpty()) {
            return Result.zero();
        }

        String a = question.isCaseSensitive() ? submitted : submitted.toLowerCase();
        String b = question.isCaseSensitive() ? correct : correct.toLowerCase();

        if (a.equals(b)) {
            return new Result(maxScore, true);
        }
        if (question.isAllowPartial() && a.contains(b)) {
            // 정답을 포함하되 군더더기가 붙은 답 — 부분점수. 정오답은 오답으로 남긴다
            // (성적 통계에서 "맞춘 문항"으로 세면 안 되기 때문).
            return new Result(maxScore / 2, false);
        }
        return Result.zero();
    }

    private String normalize(String text) {
        return text == null ? "" : text.strip().replaceAll("\\s+", " ");
    }
}
