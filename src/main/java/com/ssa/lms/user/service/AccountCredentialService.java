package com.ssa.lms.user.service;

import com.ssa.lms.user.entity.AccessLog;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.AccessLogRepository;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 최고 관리자가 <b>남의 계정</b>의 아이디/이메일/비밀번호를 바꾸는 경로.
 *
 * <p>{@link MyInfoService} 와의 차이가 핵심이다 — 본인 비밀번호 변경은 현재 비밀번호 확인이 필수지만,
 * 여기는 <b>대상자의 현재 비밀번호를 모르는 상태</b>에서 재설정하는 기능이다(분실 대응). 그래서
 * 호출 자격을 {@link SuperAdminPolicy} 로 좁히고, 수행 결과를 access_log 에 남긴다.</p>
 *
 * <ul>
 *   <li>대상 역할 제한 없음 — 관리자/강사/훈련생 모두 대상이 된다.</li>
 *   <li><b>최고 관리자 계정의 아이디는 변경 불가</b> — 아이디가 곧 최고 관리자 판별 기준이라
 *       바꾸는 순간 아무도 이 기능을 못 쓰게 된다. 반대로 다른 계정이 그 아이디를 가져가는 것도 막는다.</li>
 *   <li>빈 값은 "변경 안 함"이 아니라 필드별로 다르다: 비밀번호는 빈칸이면 유지, 이메일은 빈칸이면
 *       삭제(null). 아이디는 폼에서 필수라 빈칸으로 들어오지 않는다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountCredentialService {

    private final UserRepository userRepository;
    private final AccessLogRepository accessLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final SuperAdminPolicy superAdminPolicy;

    /** 대상 계정 조회 — 없으면 IllegalArgumentException(GlobalExceptionHandler 에서 404). */
    @Transactional(readOnly = true)
    public User get(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + id));
    }

    /**
     * 아이디/이메일/비밀번호를 한 번에 반영한다.
     *
     * @param actorLoginId 작업한 최고 관리자의 아이디 (감사 로그용)
     * @return 실제로 바뀐 항목 이름 목록 (아무것도 안 바뀌면 빈 리스트)
     * @throws DuplicateLoginIdException 새 아이디가 이미 사용 중
     * @throws IllegalStateException     최고 관리자 아이디를 바꾸거나 가져가려는 경우
     */
    @Transactional
    public List<String> apply(Long targetId, String newLoginId, String newEmail, String newRawPassword,
                              String actorLoginId, String ipAddress, String userAgent) {
        User target = get(targetId);
        List<String> changed = new ArrayList<>();

        applyLoginId(target, newLoginId, changed);
        applyEmail(target, newEmail, changed);
        applyPassword(target, newRawPassword, changed);

        if (changed.isEmpty()) {
            return changed;
        }

        accessLogRepository.save(AccessLog.builder()
                .userId(target.getId())
                .loginId(target.getLoginId())
                .type(AccessLog.Type.CREDENTIAL_CHANGE)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .occurredAt(LocalDateTime.now())
                .build());
        log.info("[credential] 최고 관리자 {} 가 계정 {}(id={}) 의 {} 을(를) 변경",
                actorLoginId, target.getLoginId(), target.getId(), String.join("/", changed));
        return changed;
    }

    private void applyLoginId(User target, String newLoginId, List<String> changed) {
        String value = newLoginId != null ? newLoginId.trim() : null;
        if (!StringUtils.hasText(value) || value.equals(target.getLoginId())) {
            return;
        }
        if (superAdminPolicy.isSuperAdmin(target.getLoginId())) {
            throw new IllegalStateException(
                    "최고 관리자 계정의 아이디는 변경할 수 없습니다. (이 아이디가 최고 관리자 판별 기준입니다)");
        }
        if (superAdminPolicy.isSuperAdmin(value)) {
            throw new IllegalStateException("최고 관리자 아이디는 다른 계정에 사용할 수 없습니다.");
        }
        if (userRepository.existsByLoginId(value)) {
            throw new DuplicateLoginIdException(value);
        }
        target.changeLoginId(value);
        changed.add("아이디");
    }

    private void applyEmail(User target, String newEmail, List<String> changed) {
        String value = StringUtils.hasText(newEmail) ? newEmail.trim() : null;
        if (Objects.equals(value, target.getEmail())) {
            return;
        }
        target.changeEmail(value);
        changed.add("이메일");
    }

    private void applyPassword(User target, String newRawPassword, List<String> changed) {
        if (!StringUtils.hasText(newRawPassword)) {
            return;
        }
        target.changePassword(passwordEncoder.encode(newRawPassword));
        changed.add("비밀번호");
    }
}
