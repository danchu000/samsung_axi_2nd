package com.ssa.lms.ai.dto;

import java.util.List;

/**
 * AI 학습 도우미 답변 — 화면이 그대로 그리는 모양.
 *
 * @param ok       답변 생성 성공 여부. 실패해도 HTTP 는 200 이고 {@code answer} 에 안내 문구가 들어간다
 * @param answer   화면에 보여줄 본문 (실패 시 안내 문구)
 * @param sources  답변이 참고한 학습 자료 링크. 근거를 못 짚으면 빈 목록
 * @param reason   실패 사유 코드 (성공 시 null) — 화면 분기용이 아니라 원인 추적용
 * @param general  <b>과정 자료가 아니라 모델의 일반 지식으로 답한 것</b>인지.
 *                 훈련생이 "수업에서 배운 내용"으로 오해하면 안 되므로 화면에서 구분해 표시한다
 */
public record AiQnaAnswer(
        boolean ok,
        String answer,
        List<Source> sources,
        String reason,
        boolean general
) {

    /** @param label 자료 제목  @param href 훈련생이 실제로 열 수 있는 링크 */
    public record Source(String label, String href) {}

    public static AiQnaAnswer ok(String answer, List<Source> sources) {
        return new AiQnaAnswer(true, answer, sources, null, false);
    }

    /** 근거 자료를 붙이지 않는다 — 실패한 답변에 자료를 달면 답이 있는 것처럼 보인다. */
    public static AiQnaAnswer fail(String reason, String message) {
        return new AiQnaAnswer(false, message, List.of(), reason, false);
    }
}
