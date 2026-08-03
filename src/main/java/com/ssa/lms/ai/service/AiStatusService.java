package com.ssa.lms.ai.service;

import com.ssa.lms.ai.config.AiProperties;
import com.ssa.lms.ai.dto.AiStatusView;
import com.ssa.lms.ai.entity.AiUsageLog;
import com.ssa.lms.ai.repository.AiUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [관리자] AI 연동 상태 판정 — 대시보드 배너의 근거.
 *
 * <p><b>AI 를 부르지 않는다.</b> 상태를 보려고 모델을 부르면 돈이 나가고, 하필
 * 크레딧이 떨어진 상황에서는 그 확인 호출마저 실패한다. 이미 쌓여 있는
 * {@link AiUsageLog} 를 읽기만 한다.</p>
 *
 * <p><b>판정은 마지막 호출 1건으로 한다.</b> 기간을 잡고 실패율을 세는 방법도 있지만,
 * 관리자가 알고 싶은 건 "지금 되냐"이지 "지난주에 몇 번 실패했냐"가 아니다.
 * 마지막 호출만 보면 회복도 자동으로 반영된다 — 충전하면 다음 성공 호출이
 * 배너를 지운다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiStatusService {

    private final AiProperties props;
    private final AiUsageLogRepository repository;

    /**
     * 지금 상태.
     *
     * <p>순서가 중요하다. 꺼져 있거나 키가 없으면 <b>호출 기록을 볼 필요가 없다</b> —
     * 지난달 실패 기록이 남아 있다고 "크레딧 소진"이라 안내하면 엉뚱한 곳을 고치게 된다.</p>
     */
    public AiStatusView current() {
        if (!props.isEnabled()) {
            return AiStatusView.disabled();
        }
        if (!props.isUsable()) {
            return AiStatusView.noKey();   // 켜두었는데 키가 빈 상태 — 배포 사고일 가능성
        }

        AiUsageLog last = repository.findTopByOrderByCalledAtDescIdDesc().orElse(null);
        if (last == null) {
            return AiStatusView.neverCalled();
        }
        return last.isSuccess()
                ? AiStatusView.healthy(last.getCalledAt())
                : AiStatusView.failed(last.getFailReason(), last.getCalledAt(),
                        props.getDailyRequestLimit());
    }
}
