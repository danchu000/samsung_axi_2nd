package com.ssa.lms.config;

import com.ssa.lms.auth.LoginUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class JpaConfig {

    /** BaseEntity 의 createdBy/updatedBy 에 로그인 사용자 id 를 채운다 (비로그인/시스템 작업은 empty). */
    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof LoginUser loginUser)) {
                return Optional.empty();
            }
            return Optional.of(loginUser.getId());
        };
    }
}
