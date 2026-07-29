package com.ssa.lms.ai.client;

/**
 * AI 모델 호출 포트.
 *
 * <p>화면·서비스는 이 인터페이스만 알고, 실제 구현이 Claude 인지 다른 모델인지는 모른다.
 * 덕분에 (1) 키가 없을 때 안내 구현으로 바꿔 끼울 수 있고, (2) 테스트에서 모델을 부르지
 * 않고도 흐름을 검증할 수 있다. 테스트가 실제 API 를 때리면 비용도 나가고 결과도 매번 달라진다.</p>
 */
public interface AiClient {

    /**
     * 한 번 물어보고 답을 받는다.
     *
     * <p><b>예외를 던지지 않는다.</b> 모델 호출 실패로 화면이 500 이 되면 안 된다 —
     * 실패는 {@link AiAnswer#ok()} false 와 사유로 돌려주고, 호출부가 안내 문구를 보여준다.</p>
     */
    AiAnswer ask(AiRequest request);

    /** 지금 실제로 모델을 부를 수 있는 상태인지 (키·설정·한도). */
    boolean available();
}
