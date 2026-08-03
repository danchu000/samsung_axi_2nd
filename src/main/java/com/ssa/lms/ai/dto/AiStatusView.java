package com.ssa.lms.ai.dto;

import com.ssa.lms.ai.client.AiFailReason;

import java.time.LocalDateTime;

/**
 * [관리자] AI 연동 상태 — 대시보드 배너에 그대로 뿌린다.
 *
 * <p><b>이 화면이 필요한 이유</b><br>
 * AI 호출이 실패하면 훈련생 화면은 "지금은 답변을 가져오지 못했어요"만 보여준다.
 * 의도한 동작이다 — 훈련생에게 크레딧 잔액을 말할 이유가 없다. 문제는 그러면
 * <b>아무도 관리자에게 알려주지 않는다는 것</b>이다. 크레딧이 떨어져도 화면은 멀쩡히
 * 200 으로 뜨기 때문에, 며칠 뒤 누가 신고할 때까지 기능이 죽은 줄 모른다.</p>
 *
 * <p><b>판정 기준은 "마지막 호출"이다.</b> 어제 한 번 실패했어도 오늘 성공했으면
 * 정상이다. 최근 실패를 계속 붙들고 있으면 이미 나은 문제로 빨간 배너가 남아
 * 곧 아무도 안 보게 된다. 반대로 크레딧 소진은 성공할 수가 없으니 배너가 계속 뜨고,
 * 충전하는 순간 다음 호출이 성공하면서 <b>저절로 사라진다.</b></p>
 *
 * @param level      심각도. 배너를 띄울지 조용히 넘길지를 가른다
 * @param title      한 줄 요약. 배지와 배너 제목에 같이 쓴다
 * @param detail     관리자가 실제로 할 일. <b>"오류가 발생했습니다" 같은 말은 쓰지 않는다</b>
 * @param consoleUrl 콘솔에서 처리해야 하는 건이면 링크. 아니면 null
 * @param at         판단 근거가 된 호출 시각. 호출 이력이 없으면 null
 */
public record AiStatusView(
        Level level,
        String title,
        String detail,
        String consoleUrl,
        LocalDateTime at
) {

    public enum Level { OK, INFO, WARN, ERROR }

    private static final String BILLING_URL = "https://console.anthropic.com/settings/billing";
    private static final String KEYS_URL = "https://console.anthropic.com/settings/keys";

    /**
     * 배너를 띄울 상태인지.
     *
     * <p>정상이거나 "그냥 꺼둔 것"이면 배지 하나로 충분하다. 아무 문제 없는데
     * 배너가 늘 떠 있으면 진짜 문제가 생겼을 때 눈에 안 들어온다.</p>
     */
    public boolean isAlert() {
        return level == Level.WARN || level == Level.ERROR;
    }

    /** 헤더 배지 CSS 클래스 (dashboard-visual.css 의 {@code .badge.*}). */
    public String badgeClass() {
        return switch (level) {
            case OK -> "ok";
            case INFO -> "info";
            case WARN -> "warn";
            case ERROR -> "danger";
        };
    }

    /** 배지에 넣을 짧은 말. 제목은 배지에 담기엔 길다. */
    public String badgeText() {
        return switch (level) {
            case OK -> "정상";
            case INFO -> "꺼짐";
            case WARN -> "확인 필요";
            case ERROR -> "조치 필요";
        };
    }

    /* ===== 호출 이력을 보기 전에 결정되는 상태 ===== */

    /** 설정으로 꺼둔 상태. 장애가 아니므로 경고하지 않는다. */
    public static AiStatusView disabled() {
        return new AiStatusView(Level.INFO,
                "AI 기능이 꺼져 있습니다",
                "서버 환경변수 LMS_AI_ENABLED=true 로 켤 수 있습니다.",
                null, null);
    }

    /**
     * 켜두었는데 키가 없는 상태.
     *
     * <p>꺼둔 것과 갈라야 한다 — 이건 <b>켜려고 했는데 안 켜진 것</b>이라
     * 배포 때 {@code .env} 를 빠뜨린 사고일 가능성이 높다.</p>
     */
    public static AiStatusView noKey() {
        return new AiStatusView(Level.WARN,
                "API 키가 등록되지 않았습니다",
                "AI 기능을 켜두었지만 키가 비어 있어 호출되지 않습니다. "
                        + "서버 .env 에 ANTHROPIC_API_KEY 를 추가한 뒤 재시작해 주세요.",
                KEYS_URL, null);
    }

    /* ===== 마지막 호출로 판정하는 상태 ===== */

    /**
     * 켜져 있지만 아직 한 번도 안 불린 상태.
     *
     * <p>"정상"이라고 단정하지 않는다 — 키가 실제로 통하는지는 한 번 불러 봐야 안다.
     * 검증 안 된 것을 검증된 것처럼 보여주면 배포 직후에 가장 크게 속는다.</p>
     */
    public static AiStatusView neverCalled() {
        return new AiStatusView(Level.OK,
                "AI 기능이 켜져 있습니다",
                "아직 호출 이력이 없어 실제 연동은 확인되지 않았습니다.",
                null, null);
    }

    public static AiStatusView healthy(LocalDateTime at) {
        return new AiStatusView(Level.OK, "정상 작동 중", null, null, at);
    }

    /**
     * 마지막 호출이 실패한 상태.
     *
     * @param dailyLimit 자체 하루 상한. 문구에 실제 숫자를 박아야 관리자가 판단할 수 있다
     */
    public static AiStatusView failed(String reason, LocalDateTime at, int dailyLimit) {
        return switch (reason == null ? "" : reason) {

            case AiFailReason.CREDIT_EXHAUSTED -> new AiStatusView(Level.ERROR,
                    "AI 크레딧이 소진되어 호출이 거부되고 있습니다",
                    "Anthropic 콘솔에서 크레딧을 충전해 주세요. "
                            + "충전하면 다음 호출부터 자동으로 복구되며 서버 재시작은 필요 없습니다.",
                    BILLING_URL, at);

            case AiFailReason.INVALID_KEY -> new AiStatusView(Level.ERROR,
                    "API 키가 유효하지 않습니다",
                    "키가 폐기되었거나 잘못 입력됐습니다. 콘솔에서 키를 재발급해 "
                            + "서버 .env 의 ANTHROPIC_API_KEY 를 교체한 뒤 재시작해 주세요.",
                    KEYS_URL, at);

            case AiFailReason.NO_ACCESS -> new AiStatusView(Level.ERROR,
                    "AI 호출이 거부됐습니다 (크레딧 또는 키 권한)",
                    "콘솔에서 크레딧 잔액과 API 키 권한을 함께 확인해 주세요. "
                            + "둘 중 어느 쪽인지는 서버 로그의 [AI] 항목에 남아 있습니다.",
                    BILLING_URL, at);

            case AiFailReason.RATE_LIMITED -> new AiStatusView(Level.WARN,
                    "오늘 사용할 수 있는 호출 횟수를 모두 썼습니다",
                    "요금 사고를 막기 위한 자체 한도(하루 " + dailyLimit + "건)입니다. "
                            + "내일 0시에 초기화되며, 부족하면 LMS_AI_DAILY_LIMIT 을 올릴 수 있습니다.",
                    null, at);

            case AiFailReason.UPSTREAM_LIMIT -> new AiStatusView(Level.WARN,
                    "요청이 몰려 일시적으로 실패했습니다",
                    "Anthropic 쪽 속도 제한입니다. 잠시 후 자동으로 정상화되며 별도 조치는 필요 없습니다.",
                    null, at);

            default -> new AiStatusView(Level.WARN,
                    "마지막 AI 호출이 실패했습니다",
                    "모델 응답 오류입니다. 반복되면 서버 로그에서 [AI] 항목을 확인해 주세요.",
                    null, at);
        };
    }
}
