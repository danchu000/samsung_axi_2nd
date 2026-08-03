package com.ssa.lms.ai.client;

/**
 * AI 호출 실패 사유 코드. {@code AiUsageLog.failReason} 에 그대로 저장된다.
 *
 * <p><b>왜 코드를 나누는가</b><br>
 * 예전에는 호출 실패를 전부 {@link #API_ERROR} 하나로 뭉갰다. 그러면 관리자 화면에서
 * <b>"크레딧이 떨어진 것"과 "모델이 잠깐 죽은 것"이 똑같이 보인다.</b> 앞은 콘솔에서
 * 충전하면 5초면 끝나고, 뒤는 기다리면 저절로 낫는데, 구분이 안 되니 관리자는
 * 매번 서버 로그를 뒤지게 된다.</p>
 *
 * <p><b>훈련생에게는 이 코드를 보여주지 않는다.</b> 크레딧 잔액은 내부 사정이고
 * 훈련생이 할 수 있는 일이 없다. 화면 문구는 {@code ClaudeAiClient} 가 따로 만든다.</p>
 *
 * <p>{@code fail_reason} 컬럼이 40자라 코드는 그 안에서 짓는다.</p>
 */
public final class AiFailReason {

    /** 기능이 꺼져 있거나 키가 없음. 장애가 아니다. */
    public static final String DISABLED = "DISABLED";

    /** 우리가 건 하루 상한 초과. 요금 사고를 막는 자체 안전장치다. */
    public static final String RATE_LIMITED = "RATE_LIMITED";

    /** 크레딧 소진 — 결제하면 다음 호출부터 복구된다. 관리자가 가장 먼저 알아야 할 상태. */
    public static final String CREDIT_EXHAUSTED = "CREDIT_EXHAUSTED";

    /** 403 인데 사유를 특정하지 못함 — 크레딧 아니면 키 권한이다. 둘 다 콘솔에서 본다. */
    public static final String NO_ACCESS = "NO_ACCESS";

    /** 키가 틀렸거나 폐기됨. 재발급 + 재시작이 필요하다. */
    public static final String INVALID_KEY = "INVALID_KEY";

    /** Anthropic 쪽 속도 제한. 기다리면 낫는다 — 우리 하루 상한과 구분한다. */
    public static final String UPSTREAM_LIMIT = "UPSTREAM_LIMIT";

    /** 호출은 됐는데 본문이 비어 돌아옴. */
    public static final String EMPTY_RESPONSE = "EMPTY_RESPONSE";

    /** 위 어디에도 안 맞는 실패. 타임아웃·네트워크·5xx 가 여기로 온다. */
    public static final String API_ERROR = "API_ERROR";

    private AiFailReason() {
    }
}
