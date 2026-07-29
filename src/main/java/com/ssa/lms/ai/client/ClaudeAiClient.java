package com.ssa.lms.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssa.lms.ai.config.AiProperties;
import com.ssa.lms.ai.entity.AiUsageLog;
import com.ssa.lms.ai.repository.AiUsageLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Claude Messages API 호출 구현.
 *
 * <p>Spring Boot 3.2+ 의 {@code RestClient} 만 쓴다 — 별도 SDK 의존성을 넣지 않는다.
 * 폐쇄망 배포가 예정돼 있어 외부 의존을 늘리지 않는 편이 안전하고, 이 정도 호출에
 * SDK 는 과하다.</p>
 *
 * <p><b>이 클래스가 지키는 것</b>
 * <ul>
 *   <li><b>예외를 밖으로 던지지 않는다</b> — 모델이 죽어도 화면은 살아 있어야 한다</li>
 *   <li><b>호출 전에 한도를 본다</b> — 토큰은 곧 돈이다. 무한 루프 한 번에 요금이 터진다</li>
 *   <li><b>모든 호출을 기록한다</b> — 성공·실패 모두. 기록은 별도 트랜잭션으로 남겨
 *       호출부가 롤백돼도 사용량은 남는다</li>
 *   <li><b>키를 로그에 남기지 않는다</b></li>
 * </ul>
 */
public class ClaudeAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAiClient.class);

    private final AiProperties props;
    private final AiUsageLogRepository usageRepo;
    private final RestClient rest;

    public ClaudeAiClient(AiProperties props, AiUsageLogRepository usageRepo, RestClient.Builder builder) {
        this.props = props;
        this.usageRepo = usageRepo;
        this.rest = builder
                .baseUrl(props.getBaseUrl())
                .defaultHeader("x-api-key", props.getApiKey())
                .defaultHeader("anthropic-version", props.getApiVersion())
                .defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public boolean available() {
        return props.isUsable();
    }

    @Override
    public AiAnswer ask(AiRequest request) {
        if (!props.isUsable()) {
            return AiAnswer.disabled();
        }

        int promptChars = charsOf(request);

        String limited = checkLimit(request.userId());
        if (limited != null) {
            record(AiUsageLog.failure(request.purpose(), request.userId(), props.getModel(),
                    "RATE_LIMITED", 0, promptChars));
            return AiAnswer.failure("RATE_LIMITED", limited);
        }

        long started = System.currentTimeMillis();
        try {
            JsonNode res = rest.post()
                    .uri("/v1/messages")
                    .body(body(request))
                    .retrieve()
                    .body(JsonNode.class);

            long elapsed = System.currentTimeMillis() - started;
            String text = extractText(res);
            int in = res != null && res.path("usage").has("input_tokens")
                    ? res.path("usage").path("input_tokens").asInt() : 0;
            int out = res != null && res.path("usage").has("output_tokens")
                    ? res.path("usage").path("output_tokens").asInt() : 0;

            if (text.isBlank()) {
                record(AiUsageLog.failure(request.purpose(), request.userId(), props.getModel(),
                        "EMPTY_RESPONSE", elapsed, promptChars));
                return AiAnswer.failure("EMPTY_RESPONSE",
                        "답변을 만들지 못했어요. 질문을 조금 더 구체적으로 적어주시겠어요?");
            }

            record(AiUsageLog.success(request.purpose(), request.userId(), props.getModel(),
                            in, out, elapsed, promptChars, text.length())
                    .withQuestion(request.courseId(), request.logQuestion()));
            return AiAnswer.success(text, in, out);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - started;
            // 예외 메시지에 요청 본문이 섞여 나올 수 있어 클래스명과 요약만 남긴다
            log.error("[AI] 호출 실패 purpose={} elapsed={}ms cause={}",
                    request.purpose(), elapsed, e.getClass().getSimpleName(), e);
            record(AiUsageLog.failure(request.purpose(), request.userId(), props.getModel(),
                    "API_ERROR", elapsed, promptChars));
            return AiAnswer.failure("API_ERROR",
                    "지금은 답변을 가져오지 못했어요. 잠시 후 다시 시도하거나 강사님께 전달해 주세요.");
        }
    }

    /** 하루 상한 확인. 넘었으면 사용자에게 보여줄 문구를 돌려준다. */
    private String checkLimit(Long userId) {
        LocalDate today = LocalDate.now();
        var from = today.atStartOfDay();

        if (props.getDailyRequestLimit() > 0
                && usageRepo.countSuccessSince(from) >= props.getDailyRequestLimit()) {
            log.warn("[AI] 일일 전체 호출 한도 도달 limit={}", props.getDailyRequestLimit());
            return "오늘 사용할 수 있는 AI 답변 횟수를 모두 썼어요. 내일 다시 이용하거나 강사님께 질문해 주세요.";
        }

        if (userId != null && props.getDailyRequestLimitPerUser() > 0
                && usageRepo.countSuccessByUserSince(userId, from) >= props.getDailyRequestLimitPerUser()) {
            return "오늘 질문할 수 있는 횟수를 모두 썼어요. 내일 다시 이용하거나 강사님께 질문해 주세요.";
        }
        return null;
    }

    private Map<String, Object> body(AiRequest r) {
        Map<String, Object> b = new HashMap<>();
        b.put("model", props.getModel());
        b.put("max_tokens", r.maxOutputTokens() != null ? r.maxOutputTokens() : props.getMaxOutputTokens());
        if (r.system() != null && !r.system().isBlank()) {
            b.put("system", r.system());
        }
        List<Map<String, String>> msgs = new ArrayList<>();
        for (AiRequest.Message m : r.messages()) {
            msgs.add(Map.of("role", m.role(), "content", m.content()));
        }
        b.put("messages", msgs);
        return b;
    }

    /** 응답의 content 블록에서 text 만 이어 붙인다. */
    private String extractText(JsonNode res) {
        if (res == null) return "";
        JsonNode content = res.path("content");
        if (!content.isArray()) return "";
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : content) {
            if ("text".equals(block.path("type").asText())) {
                sb.append(block.path("text").asText());
            }
        }
        return sb.toString().trim();
    }

    private int charsOf(AiRequest r) {
        int n = r.system() == null ? 0 : r.system().length();
        for (AiRequest.Message m : r.messages()) {
            n += m.content() == null ? 0 : m.content().length();
        }
        return n;
    }

    /**
     * 사용량 기록은 <b>별도 트랜잭션</b>으로 남긴다.
     * 호출부 트랜잭션이 롤백돼도 "돈을 썼다"는 사실은 남아야 한다.
     * 기록 실패가 본 기능을 막아서도 안 되므로 예외를 삼킨다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void record(AiUsageLog entry) {
        try {
            usageRepo.save(entry);
        } catch (Exception e) {
            log.warn("[AI] 사용량 기록 실패 — 기능은 계속한다 cause={}", e.getClass().getSimpleName());
        }
    }
}
