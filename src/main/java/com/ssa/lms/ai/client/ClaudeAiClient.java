package com.ssa.lms.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssa.lms.ai.config.AiProperties;
import com.ssa.lms.ai.entity.AiUsageLog;
import com.ssa.lms.ai.repository.AiUsageLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
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

    /** 한도 확인용 조회. 저장은 {@link #recorder} 가 별도 트랜잭션으로 한다. */
    private final AiUsageLogRepository usageRepo;

    private final AiUsageRecorder recorder;

    /** 일반 호출. */
    private final RestClient rest;

    /**
     * 웹 검색용 — <b>읽기 타임아웃이 훨씬 길다.</b>
     *
     * <p>도구를 붙인 호출은 모델이 검색하고 <b>공고 원문 페이지를 실제로 열어 읽는다.</b>
     * 수십 초에서 몇 분이 걸린다. 일반 호출과 같은 커넥션을 쓰면 둘 중 하나를 반드시
     * 희생하게 된다 — 짧게 잡으면 수집이 매번 끊기고, 길게 잡으면 Q&amp;A 가 죽었을 때
     * 훈련생이 몇 분씩 빈 화면을 본다.</p>
     *
     * <p>수집기는 <b>스케줄러 스레드</b>에서 돈다. 여기서 무한정 매달리면 같은 스케줄러에
     * 물린 다른 배치(출석 알림 등)까지 통째로 멈춘다. 그래서 반드시 상한이 있어야 한다.</p>
     */
    private final RestClient searchRest;

    public ClaudeAiClient(AiProperties props, AiUsageLogRepository usageRepo,
                          AiUsageRecorder recorder, RestClient.Builder builder) {
        this.props = props;
        this.usageRepo = usageRepo;
        this.recorder = recorder;

        RestClient.Builder base = builder.clone()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("x-api-key", props.getApiKey())
                .defaultHeader("anthropic-version", props.getApiVersion())
                .defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE);

        /*
         * 두 커넥션 모두 반드시 읽기 타임아웃을 건다.
         *
         * 예전에는 일반 호출이 `base.clone().build()` 라 타임아웃이 **없었다.**
         * lms.ai.timeout 설정은 있었는데 읽는 곳이 아무 데도 없었다 — 설정만 보고
         * "30초면 끊기겠지" 하고 넘어가기 딱 좋은 형태였다.
         *
         * 상한이 없으면 모델이 응답하지 않을 때 요청이 무한정 매달린다. 그 앞에는
         * Cloudflare 터널이 있어 100초에서 먼저 끊고, 화면에는 원인과 무관한
         * "네트워크 상태를 확인하세요" 가 뜬다. 서버 쪽에는 아무 기록도 남지 않는다.
         */
        this.rest = base.clone()
                .requestFactory(readTimeout(props.getTimeout()))
                .build();
        this.searchRest = base.clone()
                .requestFactory(readTimeout(props.getSearchTimeout()))
                .build();
    }

    /** 연결은 빨리 포기하고, 응답은 오래 기다린다 — 검색은 원래 느리지만 연결까지 느릴 이유는 없다. */
    private static ClientHttpRequestFactory readTimeout(Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(15));
        factory.setReadTimeout(readTimeout);
        return factory;
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
                    AiFailReason.RATE_LIMITED, 0, promptChars));
            return AiAnswer.failure(AiFailReason.RATE_LIMITED, limited);
        }

        long started = System.currentTimeMillis();
        try {
            // 도구를 쓰는 호출만 긴 타임아웃 커넥션으로 보낸다
            JsonNode res = (request.hasTools() ? searchRest : rest).post()
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
                        AiFailReason.EMPTY_RESPONSE, elapsed, promptChars));
                return AiAnswer.failure(AiFailReason.EMPTY_RESPONSE,
                        "답변을 만들지 못했어요. 질문을 조금 더 구체적으로 적어주시겠어요?");
            }

            record(AiUsageLog.success(request.purpose(), request.userId(), props.getModel(),
                            in, out, elapsed, promptChars, text.length())
                    .withQuestion(request.courseId(), request.logQuestion()));
            return AiAnswer.success(text, in, out);

        } catch (RestClientResponseException e) {
            // API 가 4xx/5xx 로 거절한 경우 — 사유를 갈라야 관리자가 뭘 할지 알 수 있다
            long elapsed = System.currentTimeMillis() - started;
            String reason = classify(e);
            record(AiUsageLog.failure(request.purpose(), request.userId(), props.getModel(),
                    reason, elapsed, promptChars));
            return AiAnswer.failure(reason, userMessageFor(reason));

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - started;
            // 예외 메시지에 요청 본문이 섞여 나올 수 있어 클래스명과 요약만 남긴다
            log.error("[AI] 호출 실패 purpose={} elapsed={}ms cause={}",
                    request.purpose(), elapsed, e.getClass().getSimpleName(), e);
            record(AiUsageLog.failure(request.purpose(), request.userId(), props.getModel(),
                    AiFailReason.API_ERROR, elapsed, promptChars));
            return AiAnswer.failure(AiFailReason.API_ERROR,
                    "지금은 답변을 가져오지 못했어요. 잠시 후 다시 시도하거나 강사님께 전달해 주세요.");
        }
    }

    /**
     * 거절 응답을 사유 코드로 분류한다.
     *
     * <p><b>왜 나누는가</b> — 예전에는 전부 {@code API_ERROR} 였다. 그러면 관리자 화면에서
     * "크레딧이 떨어졌다"와 "모델이 잠깐 죽었다"가 똑같이 보인다. 앞은 콘솔에서 충전하면
     * 끝이고 뒤는 기다리면 낫는데, 구분이 안 되니 매번 서버 로그를 뒤지게 된다.</p>
     *
     * <p>본문의 {@code error.type} 을 먼저 보고, 못 읽으면 상태 코드로 넘어간다.
     * 외부 스펙은 예고 없이 바뀌므로 <b>실제로 받은 type 문자열을 로그에 남긴다</b> —
     * 분류가 안 맞으면 그 로그를 보고 고치면 된다.</p>
     */
    private String classify(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        String type = errorTypeOf(e);

        // 키·본문은 남기지 않는다. 상태 코드와 사유 타입만으로 충분하다
        log.error("[AI] 호출 거부 status={} error.type={}", status, type == null ? "(판독불가)" : type);

        if (type != null) {
            switch (type) {
                case "billing_error":        return AiFailReason.CREDIT_EXHAUSTED;
                case "authentication_error": return AiFailReason.INVALID_KEY;
                case "permission_error":     return AiFailReason.NO_ACCESS;
                case "rate_limit_error":     return AiFailReason.UPSTREAM_LIMIT;
                default: break;   // 모르는 타입 — 상태 코드로 판단한다
            }
        }

        /*
         * 401/403 은 "기다려도 안 낫는" 부류라 일반 오류와 갈라 둔다.
         * 403 은 크레딧 소진일 수도 권한 문제일 수도 있는데, 본문을 못 읽었으면
         * 단정하지 않고 NO_ACCESS 로 둔다 — 관리자 문구가 둘 다 안내한다.
         */
        return switch (status) {
            case 401 -> AiFailReason.INVALID_KEY;
            case 403 -> AiFailReason.NO_ACCESS;
            case 429 -> AiFailReason.UPSTREAM_LIMIT;
            default -> AiFailReason.API_ERROR;
        };
    }

    /** 응답 본문의 {@code error.type}. JSON 이 아니거나 필드가 없으면 null. */
    private String errorTypeOf(RestClientResponseException e) {
        try {
            JsonNode body = e.getResponseBodyAs(JsonNode.class);
            if (body == null) return null;
            String type = body.path("error").path("type").asText("");
            return type.isBlank() ? null : type;
        } catch (Exception ignored) {
            return null;   // 본문 판독 실패로 호출 자체를 망치지 않는다
        }
    }

    /**
     * 화면에 그대로 나갈 문구.
     *
     * <p><b>훈련생에게 크레딧·키 문제를 말하지 않는다.</b> 내부 사정이고 훈련생이 할 수
     * 있는 일이 없다. "충전이 필요합니다"를 훈련생 화면에 띄우면 불안만 준다.
     * 실제 사유는 관리자 대시보드에만 보인다.</p>
     */
    private String userMessageFor(String reason) {
        return switch (reason) {
            case AiFailReason.CREDIT_EXHAUSTED, AiFailReason.NO_ACCESS, AiFailReason.INVALID_KEY ->
                    "AI 기능이 잠시 중단됐어요. 담당자가 확인 중이니 강사님께 질문해 주세요.";
            case AiFailReason.UPSTREAM_LIMIT ->
                    "지금 요청이 몰려 있어요. 잠시 후 다시 시도해 주세요.";
            default ->
                    "지금은 답변을 가져오지 못했어요. 잠시 후 다시 시도하거나 강사님께 전달해 주세요.";
        };
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
        if (r.hasTools()) {
            b.put("tools", r.tools());
        }
        return b;
    }

    /**
     * 응답의 content 블록에서 text 만 이어 붙인다.
     *
     * <p>도구를 쓰면 {@code server_tool_use}·{@code web_search_tool_result} 같은 블록이
     * 함께 온다. 그건 모델이 일한 흔적이지 답이 아니므로 건너뛴다 — 통째로 이어 붙이면
     * JSON 을 파싱하려는 쪽이 검색 결과 뭉치를 같이 받게 된다.</p>
     */
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
     * 사용량 기록. 실제 저장은 {@link AiUsageRecorder} 가 <b>별도 트랜잭션</b>으로 한다.
     *
     * <p><b>여기에 {@code @Transactional} 을 달면 안 된다.</b> 이 메서드는 같은 클래스의
     * {@code ask()} 가 부르는데, 자기 호출은 프록시를 타지 않아 애너테이션이 조용히 무시된다.
     * 그렇게 기록이 호출부의 읽기 전용 트랜잭션에 얹혀 운영에서 500 이 났다
     * ({@link AiUsageRecorder} 주석 참고).</p>
     *
     * <p>기록 실패가 본 기능을 막아서는 안 되므로 예외는 <b>트랜잭션 경계 밖인</b>
     * 여기서 삼킨다.</p>
     */
    protected void record(AiUsageLog entry) {
        try {
            recorder.record(entry);
        } catch (Exception e) {
            log.warn("[AI] 사용량 기록 실패 — 기능은 계속한다 cause={}", e.getClass().getSimpleName());
        }
    }
}
