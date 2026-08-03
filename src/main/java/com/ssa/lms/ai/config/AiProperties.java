package com.ssa.lms.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * AI(Claude) 연동 설정.
 *
 * <p><b>기본값은 꺼짐(enabled=false)</b>이다. 키가 없거나 꺼져 있으면 앱은 그대로 뜨고,
 * AI 기능만 "준비 중" 응답으로 대체된다 — 배포 환경에 키가 없다고 서비스가 죽으면 안 된다.</p>
 *
 * <p><b>API 키는 절대 저장소에 넣지 않는다.</b> 환경변수로만 주입한다:
 * <pre>
 *   ANTHROPIC_API_KEY=sk-ant-...  LMS_AI_ENABLED=true  ./gradlew bootRun
 * </pre>
 * application.yml 에는 {@code ${ANTHROPIC_API_KEY:}} 형태로 참조만 둔다.</p>
 *
 * <p><b>비용은 실제로 나간다.</b> 그래서 아래 안전장치를 기본으로 켠다.
 * <ul>
 *   <li>{@link #dailyRequestLimit} — 하루 호출 상한. 무한 루프·악의적 반복으로 요금이 터지는 것을 막는다</li>
 *   <li>{@link #maxOutputTokens} — 응답 길이 상한</li>
 *   <li>{@link #timeout} — 응답이 안 오면 화면이 멈춘다</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "lms.ai")
public class AiProperties {

    /** AI 기능 사용 여부. 꺼져 있으면 호출하지 않고 안내 응답만 돌려준다. */
    private boolean enabled = false;

    /** Anthropic API 키. 환경변수 ANTHROPIC_API_KEY 로 주입한다. */
    private String apiKey = "";

    private String baseUrl = "https://api.anthropic.com";

    /** 모델 id. 기본은 Sonnet — 품질 대비 비용이 학습 도우미 용도에 맞다. */
    private String model = "claude-sonnet-5";

    /** Anthropic API 버전 헤더. */
    private String apiVersion = "2023-06-01";

    /** 응답 최대 토큰. 너무 길면 비용도 늘고 화면에서 읽히지도 않는다. */
    private int maxOutputTokens = 600;

    private Duration timeout = Duration.ofSeconds(30);

    /**
     * 웹 검색을 쓰는 호출의 읽기 타임아웃.
     *
     * <p>모델이 검색하고 <b>공고 원문 페이지를 실제로 열어 읽는</b> 시간까지 포함이라
     * 일반 호출과 자릿수가 다르다. 짧게 잡으면 수집이 매번 도중에 끊긴다.</p>
     *
     * <p>그렇다고 무한정 둘 수는 없다 — 수집기는 스케줄러 스레드에서 돌기 때문에
     * 여기서 매달리면 같은 스케줄러에 물린 다른 배치까지 멈춘다.</p>
     */
    private Duration searchTimeout = Duration.ofMinutes(5);

    /**
     * 하루 호출 상한(전체 기준). 0 이면 무제한.
     * 상한에 걸리면 호출하지 않고 안내 문구를 돌려준다 — 조용히 실패하면 원인을 못 찾는다.
     */
    private int dailyRequestLimit = 200;

    /** 사용자 1인당 하루 호출 상한. 0 이면 무제한. */
    private int dailyRequestLimitPerUser = 20;

    /** 키가 실제로 준비됐는지. 켜져 있어도 키가 없으면 호출할 수 없다. */
    public boolean isUsable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }

    public int getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }

    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }

    public Duration getSearchTimeout() { return searchTimeout; }
    public void setSearchTimeout(Duration searchTimeout) { this.searchTimeout = searchTimeout; }

    public int getDailyRequestLimit() { return dailyRequestLimit; }
    public void setDailyRequestLimit(int dailyRequestLimit) { this.dailyRequestLimit = dailyRequestLimit; }

    public int getDailyRequestLimitPerUser() { return dailyRequestLimitPerUser; }
    public void setDailyRequestLimitPerUser(int v) { this.dailyRequestLimitPerUser = v; }
}
