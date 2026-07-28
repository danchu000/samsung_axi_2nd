package com.ssa.lms.user.service;

import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 계정 관리 — 목록/등록/수정/활성·비활성.
 *
 * <ul>
 *   <li>관리자는 자가 가입이 불가하므로 이 화면에서만 신규 등록한다(상태는 바로 ACTIVE).</li>
 *   <li><b>마지막 활성 관리자</b>는 비활성화(정지)할 수 없다 — 시스템에 최소 1명의 관리자가
 *       남아야 잠금 상태를 피할 수 있다.</li>
 *   <li>비밀번호는 bcrypt, 개인정보(email/phone/birthDate)는 AES-256 저장(엔티티 컨버터).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<User> findAdmins() {
        return userRepository.findByRoleOrderByNameAsc(Role.ADMIN);
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다: " + id));
        if (user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("관리자 계정이 아닙니다: " + id);
        }
        return user;
    }

    /** 신규 관리자 등록 — 상태는 즉시 ACTIVE. */
    @Transactional
    public User create(String loginId, String rawPassword, String name, String email, String phone,
                       String birthDate, String gender, String education) {
        if (userRepository.existsByLoginId(loginId)) {
            throw new DuplicateLoginIdException(loginId);
        }
        User admin = User.builder()
                .loginId(loginId)
                .password(passwordEncoder.encode(rawPassword))
                .name(name)
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .email(email)
                .phone(phone)
                .birthDate(birthDate)
                .gender(gender)
                .education(education)
                .build();
        return userRepository.save(admin);
    }

    /** 관리자 정보 수정 — loginId/role 은 불변. */
    @Transactional
    public void update(Long id, String name, String email, String phone, String birthDate,
                       String gender, String education) {
        get(id).updateProfile(name, email, phone, birthDate, gender, education);
    }

    /** 비활성화(정지) — 마지막 활성 관리자면 거부. */
    @Transactional
    public void deactivate(Long id) {
        User admin = get(id);
        if (admin.getStatus() == UserStatus.ACTIVE && countActiveAdmins() <= 1) {
            throw new IllegalStateException("마지막 활성 관리자는 비활성화할 수 없습니다.");
        }
        admin.changeStatus(UserStatus.SUSPENDED);
    }

    /** 활성화. */
    @Transactional
    public void activate(Long id) {
        get(id).changeStatus(UserStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public long countActiveAdmins() {
        return userRepository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE);
    }
}
