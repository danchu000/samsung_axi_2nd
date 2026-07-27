package com.ssa.lms.exam.dto;

import com.ssa.lms.exam.entity.ExamQuestion;

/**
 * 시험에 편성된 문항 한 행 (수정 화면의 manualQuestionList).
 * fromRule 로 수동 편성인지 규칙 확정인지 구분해 화면에 배지를 띄운다.
 */
public record ExamQuestionRow(
        String questionId,
        String code,
        String title,
        String difficulty,
        int seq,
        int score,
        boolean fromRule
) {

    public static ExamQuestionRow of(ExamQuestion eq) {
        String text = eq.getQuestion().getQuestionText() == null
                ? "" : eq.getQuestion().getQuestionText().strip();
        return new ExamQuestionRow(
                String.valueOf(eq.getQuestion().getId()),
                eq.getQuestion().getQuestionCode(),
                text.length() <= 40 ? text : text.substring(0, 40) + "…",
                eq.getQuestion().getDifficulty().name().toLowerCase(),
                eq.getSeq(),
                eq.resolveScore(),
                eq.isFromRule()
        );
    }
}
