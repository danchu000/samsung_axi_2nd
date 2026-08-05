package com.ssa.lms.ai;

import com.ssa.lms.ai.client.AiUsageRecorder;
import com.ssa.lms.ai.entity.AiUsageLog;
import com.ssa.lms.ai.repository.AiUsageLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AI 사용량 기록이 <b>호출부 트랜잭션에 얹히지 않는다</b>는 것을 고정한다.
 *
 * <h3>왜 이 테스트가 필요한가 — 운영에서만 터졌던 500</h3>
 * 모든 AI 서비스(AiRoadmapService·AiCurriculumService·AiQnaService …)는
 * {@code @Transactional(readOnly = true)} 인데, 그 안에서 부르는 {@code AiClient.ask()} 가
 * {@code ai_usage_log} 를 저장한다. {@link AiUsageLog} 는 id 전략이 {@code IDENTITY} 라
 * 이 INSERT 는 flush 를 기다리지 않고 <b>그 자리에서 실행</b>된다.
 *
 * <p>H2 는 {@code Connection.setReadOnly(true)} 를 무시해서 로컬·테스트에선 멀쩡했다.
 * PostgreSQL 은 트랜잭션을 READ ONLY 로 열기 때문에
 * {@code cannot execute INSERT in a read-only transaction} 으로 거절하고, 그러면 Hibernate 가
 * 트랜잭션을 rollback-only 로 표시해 커밋에서 {@code UnexpectedRollbackException} 이 난다 —
 * 훈련생에게는 로드맵·커리큘럼 화면이 통째로 500 이었다.</p>
 *
 * <p>{@code @Transactional(REQUIRES_NEW)} 는 원래도 달려 있었지만 같은 클래스 안에서
 * {@code this.record(...)} 로 불려 <b>프록시를 타지 않아 무시됐다.</b> 그래서
 * {@link AiUsageRecorder} 를 별도 빈으로 뺐다.</p>
 */
@SpringBootTest
@Import(ReadOnlyTxUsageLogTest.ProbeConfig.class)
class ReadOnlyTxUsageLogTest {

    @Autowired Probe probe;
    @Autowired AiUsageLogRepository usageRepo;

    @TestConfiguration
    static class ProbeConfig {
        @Bean
        Probe probe(AiUsageRecorder recorder, DataSource dataSource) {
            return new Probe(recorder, dataSource);
        }
    }

    /**
     * 핵심 회귀 검사 — 기록이 <b>별도 트랜잭션</b>에서 커밋되는지.
     *
     * <p>호출부가 롤백돼도 사용량은 남아야 한다. 예전처럼 자기 호출이라 프록시를 못 타면
     * 기록이 호출부 트랜잭션에 얹혀 <b>같이 사라진다</b> — 그때 이 테스트가 깨진다.</p>
     */
    @Test
    @DisplayName("사용량 기록은 호출부가 롤백돼도 살아남는다 (별도 트랜잭션)")
    void 별도_트랜잭션으로_기록된다() {
        String purpose = "TX-PROBE-" + System.nanoTime();

        assertThatThrownBy(() -> probe.기록하고_롤백(purpose))
                .isInstanceOf(IllegalStateException.class);

        assertThat(usageRepo.findAll())
                .as("호출부와 같은 트랜잭션에 얹히면 롤백에 같이 쓸려 간다 — REQUIRES_NEW 가 안 먹은 것")
                .anyMatch(l -> purpose.equals(l.getPurpose()));
    }

    /**
     * 위 구조가 왜 필요했는지를 남겨 두는 검사.
     * 읽기 전용 트랜잭션에서는 커넥션이 read-only 로 표시된다 — PostgreSQL 이면 INSERT 거절 사유가 된다.
     */
    @Test
    @DisplayName("읽기 전용 트랜잭션은 커넥션을 read-only 로 표시한다")
    void 읽기전용_트랜잭션은_커넥션을_읽기전용으로_만든다() {
        assertThat(probe.읽기전용_커넥션인가())
                .as("여기가 false 가 되면 위 회귀 검사의 전제가 사라진 것이다")
                .isTrue();
    }

    static class Probe {

        private final AiUsageRecorder recorder;
        private final DataSource dataSource;

        Probe(AiUsageRecorder recorder, DataSource dataSource) {
            this.recorder = recorder;
            this.dataSource = dataSource;
        }

        /** 호출부가 기록 직후에 터지는 상황. 사용량은 남아야 한다. */
        @Transactional
        public void 기록하고_롤백(String purpose) {
            recorder.record(AiUsageLog.failure(purpose, 1L, "test-model", "API_ERROR", 1, 10));
            throw new IllegalStateException("호출부 롤백");
        }

        /** 운영의 AI 서비스와 같은 모양. */
        @Transactional(readOnly = true)
        public boolean 읽기전용_커넥션인가() {
            Connection con = DataSourceUtils.getConnection(dataSource);
            try {
                return con.isReadOnly();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            } finally {
                DataSourceUtils.releaseConnection(con, dataSource);
            }
        }
    }
}
