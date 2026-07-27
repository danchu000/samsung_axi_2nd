package com.ssa.lms.auth;

import com.ssa.lms.user.entity.AccessLog;
import com.ssa.lms.user.repository.AccessLogRepository;
import com.ssa.lms.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 로그인 성공/실패를 access_log 에 기록 (내역서 보안 증빙: 접속 로그).
 * 성공 시 users.last_login_at 도 갱신.
 */
@Component
@RequiredArgsConstructor
public class AccessLogEventListener {

    private final AccessLogRepository accessLogRepository;
    private final UserRepository userRepository;

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if (!(principal instanceof LoginUser loginUser)) {
            return;
        }
        accessLogRepository.save(AccessLog.builder()
                .userId(loginUser.getId())
                .loginId(loginUser.getUsername())
                .type(AccessLog.Type.LOGIN)
                .ipAddress(clientIp())
                .userAgent(userAgent())
                .occurredAt(LocalDateTime.now())
                .build());
        userRepository.findById(loginUser.getId())
                .ifPresent(user -> user.recordLogin(LocalDateTime.now()));
    }

    @EventListener
    @Transactional
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String attemptedLoginId = String.valueOf(event.getAuthentication().getPrincipal());
        accessLogRepository.save(AccessLog.builder()
                .loginId(attemptedLoginId)
                .type(AccessLog.Type.LOGIN_FAIL)
                .ipAddress(clientIp())
                .userAgent(userAgent())
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private String clientIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }

    private String userAgent() {
        HttpServletRequest request = currentRequest();
        return request != null ? request.getHeader("User-Agent") : null;
    }
}
