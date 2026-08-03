package com.ssa.lms.ai.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        Long userId,
        /** 진단 기록용 — 어느 과정에 대한 질문인지. 해당 없으면 null */
        Long courseId,
        /**
         * 사용량 기록에 <b>암호화해서</b> 남길 질문 본문. null 이면 남기지 않는다.
         * 훈련생 Q&A 만 채운다 — 강사·배치 호출은 진단 대상이 아니다.
         */
        String logQuestion,
        /**
         * 서버 도구(웹 검색·웹 페치) 정의. 비어 있으면 도구 없이 부른다.
         *
         * <p>도구를 붙인 호출은 <b>모델이 웹 페이지를 실제로 열어 읽으므로</b> 일반 호출보다
         * 훨씬 오래 걸린다. {@code ClaudeAiClient} 가 이 값이 있으면 타임아웃이 긴
         * 별도 커넥션으로 보낸다.</p>
         */
        List<Map<String, Object>> tools
) {

    /** 도구를 쓰는 호출인지 — 타임아웃과 요청 본문이 갈린다. */
    public boolean hasTools() {
        return tools != null && !tools.isEmpty();
    }

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
        private Long courseId;
        private String logQuestion;
        private List<Map<String, Object>> tools;

        private Builder(String purpose) { this.purpose = purpose; }

        public Builder system(String s) { this.system = s; return this; }
        public Builder user(String c) { this.messages.add(Message.user(c)); return this; }
        public Builder assistant(String c) { this.messages.add(Message.assistant(c)); return this; }
        public Builder messages(List<Message> m) { this.messages.addAll(m); return this; }
        public Builder maxOutputTokens(Integer n) { this.maxOutputTokens = n; return this; }
        public Builder userId(Long id) { this.userId = id; return this; }
        public Builder courseId(Long id) { this.courseId = id; return this; }
        /** 진단용으로 질문을 기록에 남긴다(암호화 저장). 남기지 않으려면 부르지 않는다. */
        public Builder logQuestion(String q) { this.logQuestion = q; return this; }

        /** 서버 도구(웹 검색 등)를 붙인다. 안 부르면 도구 없이 나간다. */
        public Builder tools(List<Map<String, Object>> t) { this.tools = t; return this; }

        public AiRequest build() {
            return new AiRequest(purpose, system, List.copyOf(messages),
                    maxOutputTokens, userId, courseId, logQuestion,
                    tools == null ? List.of() : List.copyOf(tools));
        }
    }
}
