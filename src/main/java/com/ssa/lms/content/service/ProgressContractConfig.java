package com.ssa.lms.content.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link ProgressQueryService} 기본 fallback — P2-A 의 실제 구현체가 없을 때만 등록된다
 * ({@code @ConditionalOnMissingBean}). 덕분에 P2-B(이수) 트랙이 P2-A 없이도 단독 부팅된다.
 *
 * <p>P2-A 가 {@code @Service} 구현체를 추가하면 이 빈은 자동으로 물러난다 —
 * 이 파일을 수정/삭제할 필요 없음(머지 충돌 방지). base 소유.</p>
 */
@Configuration
public class ProgressContractConfig {

    @Bean
    @ConditionalOnMissingBean(ProgressQueryService.class)
    public ProgressQueryService fallbackProgressQueryService() {
        return new ProgressQueryService() {
            @Override
            public int completedRatio(Long userId, Long courseId) {
                return 0; // 통합 전 임시값
            }

            @Override
            public boolean hasCompletedAll(Long userId, Long courseId) {
                return false;
            }
        };
    }
}
