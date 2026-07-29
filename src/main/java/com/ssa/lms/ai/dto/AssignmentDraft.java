package com.ssa.lms.ai.dto;

/**
 * AI 가 만든 과제 초안. [기능 4] 강사가 확인·수정해서 확정한다.
 *
 * <p><b>초안이지 확정이 아니다.</b> 이 값은 배정 화면의 입력칸을 미리 채우는 데만 쓴다.
 * 강사가 손대지 않아도 그대로 나가는 경로는 만들지 않는다 — 훈련생에게 과제가 늘어나는
 * 일을 모델이 혼자 결정하면 잘못 붙어도 되돌릴 사람이 없다.</p>
 *
 * @param ok          생성 성공 여부
 * @param title       과제 제목 (실패 시 빈 문자열)
 * @param description 과제 설명 — 무엇을, 왜, 어떻게 제출하는지
 * @param criteria    평가 기준
 * @param message     실패했을 때 화면에 보여줄 안내
 */
public record AssignmentDraft(
        boolean ok, String title, String description, String criteria, String message
) {
    public static AssignmentDraft ok(String title, String description, String criteria) {
        return new AssignmentDraft(true, title, description, criteria, null);
    }

    public static AssignmentDraft fail(String message) {
        return new AssignmentDraft(false, "", "", "", message);
    }
}
