package com.ssa.lms.grading.dto;

/**
 * 채점 팝업의 문항 하나 — {@code grading-modal-result.html} 의 {@code questions[]}.
 *
 * <p>{@code manual=false} 인 문항(객관식·주관식)은 이미 {@code AutoGrader} 가 채점했다.
 * <b>여기서 점수를 다시 계산하거나 덮어쓰면 안 된다</b> — 화면도 입력을 막는다.</p>
 *
 * @param given        현재 부여된 점수. 미채점이면 null
 * @param manual       수동 채점 대상인지 ({@code Question.isAutoGradable() == false})
 * @param allowPartial 부분점수 허용 여부. false 면 0 또는 만점만 입력할 수 있다.
 */
public record GradingQuestionRow(
        String questionId,
        int q,
        String type,
        String status,
        String statusClass,
        int max,
        Integer given,
        String questionText,
        String modelAnswer,
        String answer,
        String comment,
        boolean manual,
        boolean allowPartial
) {
}
