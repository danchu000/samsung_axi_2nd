package com.ssa.lms.ai.client;

/**
 * AI 가 꺼져 있거나 키가 없을 때 쓰는 구현.
 *
 * <p>호출을 시도조차 하지 않고 안내 응답만 돌려준다. 이 구현이 있어야
 * <b>키 없이도 앱이 정상 기동</b>하고, 화면은 "준비 중" 안내를 보여줄 수 있다.
 * 배포 환경에 키가 빠졌다고 서비스가 죽으면 안 된다.</p>
 */
public class DisabledAiClient implements AiClient {

    @Override
    public AiAnswer ask(AiRequest request) {
        return AiAnswer.disabled();
    }

    @Override
    public boolean available() {
        return false;
    }
}
