package com.ssa.lms.ai.client;

/**
 * 모델 응답.
 *
 * <p>실패해도 예외를 던지지 않고 이 객체로 돌려준다 — 호출부가 안내 문구를 보여줄 수
 * 있어야 하고, 모델이 죽었다고 화면이 500 이 되면 안 된다.</p>
 *
 * @param ok          호출 성공 여부
 * @param text        모델 답변 (실패 시 빈 문자열)
 * @param reason      실패 사유 코드 — DISABLED / NO_KEY / RATE_LIMITED / TIMEOUT / API_ERROR
 * @param userMessage 화면에 그대로 보여줄 수 있는 안내 문구
 * @param inputTokens  사용량 기록용 (모르면 0)
 * @param outputTokens 사용량 기록용 (모르면 0)
 */
public record AiAnswer(
        boolean ok,
        String text,
        String reason,
        String userMessage,
        int inputTokens,
        int outputTokens
) {

    public static AiAnswer success(String text, int in, int out) {
        return new AiAnswer(true, text, null, null, in, out);
    }

    public static AiAnswer failure(String reason, String userMessage) {
        return new AiAnswer(false, "", reason, userMessage, 0, 0);
    }

    /** 기능이 꺼져 있거나 키가 없을 때. 장애가 아니라 "아직 준비 안 됨"이다. */
    public static AiAnswer disabled() {
        return failure("DISABLED",
                "AI 답변 기능이 아직 켜지지 않았어요. 담당 강사님께 질문을 전달해 주세요.");
    }
}
