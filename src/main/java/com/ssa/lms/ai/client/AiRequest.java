package com.ssa.lms.ai.client;

import java.util.ArrayList;
import java.util.List;

/**
 * 모델에 보낼 요청.
 *
 * <p>대화형(Q&amp;A)과 단발 분석(커리큘럼 추천·로드맵 정리)을 같은 모양으로 담는다.
 * {@code system} 에 역할·규칙을, {@code messages} 에 실제 대화를 넣는다.</p>
 *
 * <p>{@link #purpose} 는 사용량 기록에 남는다 — 어느 기능이 비용을 얼마나 쓰는지
 * 나중에 보려면 호출 시점에 남겨야 한다.</p>
 */
public record AiRequest(
        String purpose,
        String system,
        List<Message> messages,
        Integer maxOutputTokens,
        Long userId
) {

    /** role = "user" | "assistant" */
    public record Message(String role, String content) {
        public static Message user(String c) { return new Message("user", c); }
        public static Message assistant(String c) { return new Message("assistant", c); }
    }

    public static Builder of(String purpose) {
        return new Builder(purpose);
    }

    public static final class Builder {
        private final String purpose;
        private String system;
        private final List<Message> messages = new ArrayList<>();
        private Integer maxOutputTokens;
        private Long userId;

        private Builder(String purpose) { this.purpose = purpose; }

        public Builder system(String s) { this.system = s; return this; }
        public Builder user(String c) { this.messages.add(Message.user(c)); return this; }
        public Builder assistant(String c) { this.messages.add(Message.assistant(c)); return this; }
        public Builder messages(List<Message> m) { this.messages.addAll(m); return this; }
        public Builder maxOutputTokens(Integer n) { this.maxOutputTokens = n; return this; }
        public Builder userId(Long id) { this.userId = id; return this; }

        public AiRequest build() {
            return new AiRequest(purpose, system, List.copyOf(messages), maxOutputTokens, userId);
        }
    }
}
