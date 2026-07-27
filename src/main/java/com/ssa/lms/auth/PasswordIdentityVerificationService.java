package com.ssa.lms.auth;

import com.ssa.lms.user.entity.AccessLog;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.AccessLogRepository;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 본인인증 기본 구현 — 비밀번호 재확인 방식.
 * 인증 성공은 access_log(IDENTITY_VERIFY) 에 기록되어 lastVerifiedAt 판정과 증빙에 쓰인다.
 *
 * TODO(A-인증 슬라이스): 외부 본인인증(PASS/SMS) 수단 추가 시 method 분기 확장.
 */
@Service
@RequiredArgsConstructor
public class PasswordIdentityVerificationService implements IdentityVerificationService {

    private final UserRepository userRepository;
    private final AccessLogRepository accessLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public String verify(Long userId, VerifyRequest request) {
        if (!VerifyRequest.METHOD_PASSWORD.equals(request.method())) {
            throw new IdentityVerificationException("지원하지 않는 인증 수단: " + request.method());
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityVerificationException("존재하지 않는 사용자: " + userId));
        if (!passwordEncoder.matches(request.credential(), user.getPassword())) {
            throw new IdentityVerificationException("본인인증 실패 — 비밀번호 불일치");
        }
        accessLogRepository.save(AccessLog.builder()
                .userId(user.getId())
                .loginId(user.getLoginId())
                .type(AccessLog.Type.IDENTITY_VERIFY)
                .occurredAt(LocalDateTime.now())
                .build());
        return VerifyRequest.METHOD_PASSWORD;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDateTime> lastVerifiedAt(Long userId) {
        return accessLogRepository
                .findTopByUserIdAndTypeOrderByOccurredAtDesc(userId, AccessLog.Type.IDENTITY_VERIFY)
                .map(AccessLog::getOccurredAt);
    }
}
