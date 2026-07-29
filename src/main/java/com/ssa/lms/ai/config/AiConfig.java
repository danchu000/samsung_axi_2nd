package com.ssa.lms.ai.config;

import com.ssa.lms.ai.client.AiClient;
import com.ssa.lms.ai.client.ClaudeAiClient;
import com.ssa.lms.ai.client.DisabledAiClient;
import com.ssa.lms.ai.repository.AiUsageLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * AI 연동 배선.
 *
 * <p><b>키가 없으면 앱이 그냥 뜬다.</b> 실제 호출 구현 대신 안내 구현을 꽂는다 —
 * 배포 현장에서 키를 아직 못 받았다고 서비스 전체가 죽으면 안 된다.
 * 어느 쪽이 꽂혔는지는 기동 로그 한 줄로 남긴다. 조용히 안내 응답만 나오면
 * "AI가 고장 났다"로 오해하고 원인을 찾느라 시간을 버린다.</p>
 *
 * <p>키 값 자체는 절대 로그에 남기지 않는다. 준비 여부만 남긴다.</p>
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    public AiClient aiClient(AiProperties props,
                             AiUsageLogRepository usageRepo,
                             RestClient.Builder builder) {
        if (!props.isUsable()) {
            log.warn("[AI] 비활성 — {}. AI 기능은 안내 문구로 대체됩니다.",
                    props.isEnabled() ? "lms.ai.enabled=true 이지만 API 키가 비어 있음"
                                      : "lms.ai.enabled=false");
            return new DisabledAiClient();
        }
        log.info("[AI] 활성 — model={} 일일한도={} (사용자당 {})",
                props.getModel(), props.getDailyRequestLimit(), props.getDailyRequestLimitPerUser());
        return new ClaudeAiClient(props, usageRepo, builder);
    }
}
