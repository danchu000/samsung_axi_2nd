package com.ssa.lms.ai.client;

import com.ssa.lms.ai.entity.AiUsageLog;
import com.ssa.lms.ai.repository.AiUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 사용량 기록을 <b>별도 트랜잭션</b>으로 남긴다.
 *
 * <p><b>왜 별도 빈인가 — 같은 클래스 안에 두면 조용히 꺼진다.</b><br>
 * 예전에는 {@code ClaudeAiClient.record()} 에 {@code @Transactional(REQUIRES_NEW)} 를 달아
 * 뒀는데, 같은 클래스의 {@code ask()} 가 {@code this.record(...)} 로 불렀다. 자기 호출은
 * 프록시를 타지 않으므로 <b>애너테이션이 아무 일도 하지 않았다.</b> 기록은 호출부의
 * 트랜잭션에 그대로 얹혔다.</p>
 *
 * <p>그게 운영에서 500 이 됐다. AI 서비스는 전부 {@code @Transactional(readOnly = true)} 인데,
 * {@link AiUsageLog} 는 id 전략이 {@code IDENTITY} 라 저장이 flush 를 기다리지 않고
 * <b>그 자리에서 INSERT</b> 된다. H2 는 읽기 전용 표시를 무시해서 로컬에선 멀쩡했지만,
 * PostgreSQL 은 트랜잭션을 READ ONLY 로 열기 때문에
 * {@code cannot execute INSERT in a read-only transaction} 으로 거절했다. 그러면 Hibernate 가
 * 트랜잭션을 rollback-only 로 표시하고, 커밋 시점에 {@code UnexpectedRollbackException} 이
 * 화면까지 올라온다 — 훈련생에게는 로드맵·커리큘럼 화면이 통째로 500 으로 보였다.</p>
 *
 * <p><b>예외를 여기서 삼키지 않는다.</b> 삼키면 실패한 INSERT 때문에 이 새 트랜잭션이
 * rollback-only 로 남고, 커밋할 때 결국 같은 예외가 밖으로 나간다. 트랜잭션은 깨끗하게
 * 롤백시키고, 무시할지는 부르는 쪽({@code ClaudeAiClient.record})이 정한다.</p>
 */
@Service
@RequiredArgsConstructor
public class AiUsageRecorder {

    private final AiUsageLogRepository usageRepo;

    /** 호출부 트랜잭션이 롤백돼도 "돈을 썼다"는 사실은 남아야 한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AiUsageLog entry) {
        usageRepo.save(entry);
    }
}
