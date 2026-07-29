package com.ssa.lms.ai.service;

import com.ssa.lms.ai.entity.AiUsageLog;
import com.ssa.lms.ai.repository.AiUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 보존기간이 지난 <b>질문 본문만</b> 지운다.
 *
 * <p>[기능 4] 진단을 하려면 훈련생 질문을 저장해야 한다. 다만 <b>필요한 기간만</b>
 * 갖고 있어야 한다 — 3년 전에 뭘 물었는지는 진단에 쓸모가 없고, 남아 있으면
 * 유출 위험만 남는다.</p>
 *
 * <p><b>기록 자체는 지우지 않는다.</b> 호출 건수·토큰 같은 통계는 그대로 두고
 * 내용만 비운다. 이력을 통째로 지우면 "지난달에 얼마나 썼나"를 못 본다.</p>
 *
 * <p>진단은 하루 단위로 도는데 보존은 넉넉히 90일로 둔다 — 강사가 "지난달에는
 * 어땠나"를 되짚을 여지는 남겨야 한다.</p>
 */
@Service
@RequiredArgsConstructor
public class AiUsageCleaner {

    private static final Logger log = LoggerFactory.getLogger(AiUsageCleaner.class);

    private final AiUsageLogRepository repository;

    /** 질문 본문 보존 일수. 이 기간이 지나면 내용만 지운다. */
    @Value("${lms.ai.question-retention-days:90}")
    private int retentionDays;

    /** 매일 새벽 4시. 훈련생이 거의 안 쓰는 시간대다. */
    @Scheduled(cron = "${lms.ai.cleanup-cron:0 0 4 * * *}")
    @Transactional
    public void purgeExpiredQuestions() {
        LocalDateTime before = LocalDateTime.now().minusDays(retentionDays);
        List<AiUsageLog> expired = repository.findExpiredQuestions(before);
        if (expired.isEmpty()) return;

        expired.forEach(AiUsageLog::forgetQuestion);
        log.info("[AI] 보존기간({}일) 지난 질문 본문 {}건 삭제 — 통계는 유지",
                retentionDays, expired.size());
    }
}
