package com.ssa.lms.ai;

import com.ssa.lms.ai.client.AiFailReason;
import com.ssa.lms.ai.config.AiProperties;
import com.ssa.lms.ai.dto.AiStatusView;
import com.ssa.lms.ai.entity.AiUsageLog;
import com.ssa.lms.ai.repository.AiUsageLogRepository;
import com.ssa.lms.ai.service.AiStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * [관리자] AI 상태 배너 판정.
 *
 * <p><b>모델을 부르지 않는다.</b> 상태를 보려고 실제 호출을 하면 돈이 나가고,
 * 하필 크레딧이 떨어진 상황에서는 그 확인 호출마저 실패한다.</p>
 *
 * <p>여기서 지키려는 것은 <b>"관리자가 무엇을 해야 하는지"가 갈리는가</b>이다.
 * 크레딧 소진은 콘솔에서 충전하면 끝이고, 모델 오류는 기다리면 낫는다.
 * 둘이 같은 문구로 보이면 관리자는 매번 서버 로그를 뒤지게 된다.</p>
 */
class AiStatusServiceTest {

    private AiProperties props;
    private AiUsageLogRepository repository;
    private AiStatusService service;

    @BeforeEach
    void setUp() {
        props = new AiProperties();
        props.setEnabled(true);
        props.setApiKey("sk-ant-test-key");
        props.setDailyRequestLimit(200);

        repository = mock(AiUsageLogRepository.class);
        service = new AiStatusService(props, repository);
    }

    /** 편의 — 실패 기록 1건을 마지막 호출로 세운다. */
    private void lastCallFailed(String reason) {
        when(repository.findTopByOrderByCalledAtDescIdDesc())
                .thenReturn(Optional.of(AiUsageLog.failure(
                        "QNA", 1L, "claude-sonnet-5", reason, 120, 300)));
    }

    @Test
    @DisplayName("꺼져 있으면 안내만 하고, 호출 기록은 아예 보지 않는다")
    void disabled_doesNotLookAtHistory() {
        props.setEnabled(false);

        AiStatusView status = service.current();

        assertThat(status.level()).isEqualTo(AiStatusView.Level.INFO);
        assertThat(status.isAlert()).isFalse();
        /*
         * 꺼진 상태에서 지난달 실패 기록까지 읽으면 "크레딧 소진"이라고 안내하게 된다.
         * 관리자는 멀쩡한 결제를 확인하러 가고, 정작 꺼진 스위치는 못 찾는다.
         */
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("켜두었는데 키가 없으면 경고한다 — 꺼둔 것과 구분해야 한다")
    void enabledWithoutKey_warns() {
        props.setApiKey("");

        AiStatusView status = service.current();

        // 이건 "안 켠 것"이 아니라 "켜려다 만 것" — 배포 때 .env 를 빠뜨린 사고일 수 있다
        assertThat(status.level()).isEqualTo(AiStatusView.Level.WARN);
        assertThat(status.isAlert()).isTrue();
        assertThat(status.title()).contains("API 키");
    }

    @Test
    @DisplayName("호출 이력이 없으면 '정상'이라 단정하지 않는다")
    void neverCalled_doesNotClaimHealthy() {
        when(repository.findTopByOrderByCalledAtDescIdDesc()).thenReturn(Optional.empty());

        AiStatusView status = service.current();

        // 경고할 일은 아니지만, 키가 실제로 통하는지는 한 번 불러 봐야 안다
        assertThat(status.isAlert()).isFalse();
        assertThat(status.detail()).contains("확인되지 않았");
        assertThat(status.at()).isNull();
    }

    @Test
    @DisplayName("마지막 호출이 성공이면 배너를 띄우지 않는다 — 충전하면 저절로 사라진다")
    void lastCallSucceeded_clearsBanner() {
        when(repository.findTopByOrderByCalledAtDescIdDesc())
                .thenReturn(Optional.of(AiUsageLog.success(
                        "QNA", 1L, "claude-sonnet-5", 900, 220, 1500, 300, 500)));

        AiStatusView status = service.current();

        assertThat(status.level()).isEqualTo(AiStatusView.Level.OK);
        assertThat(status.isAlert()).isFalse();
        assertThat(status.at()).isNotNull();   // 언제 확인된 정상인지 같이 보여준다
    }

    @Test
    @DisplayName("크레딧 소진은 최고 심각도 + 콘솔 링크를 준다")
    void creditExhausted_isActionable() {
        lastCallFailed(AiFailReason.CREDIT_EXHAUSTED);

        AiStatusView status = service.current();

        assertThat(status.level()).isEqualTo(AiStatusView.Level.ERROR);
        assertThat(status.badgeClass()).isEqualTo("danger");
        assertThat(status.title()).contains("크레딧");
        // 관리자가 바로 갈 곳이 없으면 배너가 그냥 나쁜 소식일 뿐이다
        assertThat(status.consoleUrl()).contains("console.anthropic.com");
        assertThat(status.detail()).contains("재시작은 필요 없습니다");
    }

    @Test
    @DisplayName("키 오류와 크레딧 소진은 서로 다른 조치를 안내한다")
    void invalidKey_tellsDifferentAction() {
        lastCallFailed(AiFailReason.INVALID_KEY);

        AiStatusView status = service.current();

        assertThat(status.level()).isEqualTo(AiStatusView.Level.ERROR);
        // 충전이 아니라 재발급 + 재시작이다. 둘을 같은 문구로 뭉개면 안 된다
        assertThat(status.detail()).contains("재발급").contains("재시작");
        assertThat(status.detail()).doesNotContain("충전");
    }

    @Test
    @DisplayName("자체 하루 한도는 경고 수준이고, 실제 한도 숫자를 문구에 넣는다")
    void ownDailyLimit_isWarnWithNumber() {
        props.setDailyRequestLimit(150);
        lastCallFailed(AiFailReason.RATE_LIMITED);

        AiStatusView status = service.current();

        // 장애가 아니라 우리가 건 안전장치다 — 빨간 배너로 놀라게 할 일이 아니다
        assertThat(status.level()).isEqualTo(AiStatusView.Level.WARN);
        assertThat(status.detail()).contains("150건");
        assertThat(status.consoleUrl()).isNull();   // 콘솔에서 할 일이 없다
    }

    @Test
    @DisplayName("Anthropic 쪽 속도 제한은 기다리면 낫는다고 알린다")
    void upstreamLimit_isTransient() {
        lastCallFailed(AiFailReason.UPSTREAM_LIMIT);

        AiStatusView status = service.current();

        assertThat(status.level()).isEqualTo(AiStatusView.Level.WARN);
        assertThat(status.detail()).contains("자동으로 정상화");
    }

    @Test
    @DisplayName("모르는 실패 사유도 조용히 넘기지 않는다")
    void unknownReason_stillWarns() {
        lastCallFailed("SOMETHING_NEW_FROM_UPSTREAM");

        AiStatusView status = service.current();

        /*
         * 분류에 없는 코드가 OK 로 떨어지면, API 스펙이 바뀌어 기능이 죽어도
         * 화면은 "정상"이라고 말한다. 모르면 모른다고 하고 로그를 보라고 한다.
         */
        assertThat(status.isAlert()).isTrue();
        assertThat(status.detail()).contains("서버 로그");
    }
}
