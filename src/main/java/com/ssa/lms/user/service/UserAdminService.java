package com.ssa.lms.user.service;

import com.ssa.lms.user.entity.AccessLog;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.AccessLogRepository;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자용 사용자 관리 로직 — 가입 승인/반려, 계정 상태 변경, 목록/상세 조회.
 *
 * <ul>
 *   <li><b>승인(PENDING→ACTIVE)</b>: 이게 없으면 신규 가입 계정이 로그인 불가
 *       ({@code LoginUser.isEnabled()} 가 ACTIVE 만 허용).</li>
 *   <li><b>반려</b>: 가입 거부 — soft delete 로 처리(3년 보존 요건, {@code @SQLDelete}).</li>
 *   <li>ADMIN 계정은 승인/반려 대상이 아니다(자가 가입 불가, 관리자 화면에서만 등록).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final AccessLogRepository accessLogRepository;

    /* ===== 조회 ===== */

    @Transactional(readOnly = true)
    public List<User> findPending() {
        return userRepository.findByStatusOrderByCreatedAtAsc(UserStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return userRepository.countByStatus(UserStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<User> findByRole(Role role) {
        return userRepository.findByRoleOrderByNameAsc(role);
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + id));
    }

    /** 접속 이력(로그인/로그아웃/실패/본인인증) — 최신순 페이지. */
    @Transactional(readOnly = true)
    public Page<AccessLog> findAccessLogs(Long userId, Pageable pageable) {
        return accessLogRepository.findByUserIdOrderByOccurredAtDesc(userId, pageable);
    }

    /* ===== 승인/반려 ===== */

    /** 가입 승인 — PENDING 계정만 ACTIVE 로 전환. */
    @Transactional
    public void approve(Long id) {
        User user = get(id);
        if (user.getStatus() != UserStatus.PENDING) {
            throw new IllegalStateException("승인 대기 상태의 계정만 승인할 수 있습니다.");
        }
        user.changeStatus(UserStatus.ACTIVE);
    }

    /** 가입 반려 — PENDING 계정만 soft delete. */
    @Transactional
    public void reject(Long id) {
        User user = get(id);
        if (user.getStatus() != UserStatus.PENDING) {
            throw new IllegalStateException("승인 대기 상태의 계정만 반려할 수 있습니다.");
        }
        userRepository.delete(user); // @SQLDelete → is_deleted 마킹 (물리 삭제 아님)
    }

    /* ===== 계정 상태 변경(목록의 활성/비활성) ===== */

    @Transactional
    public void changeStatus(Long id, UserStatus status) {
        User user = get(id);
        user.changeStatus(status);
    }

    /** 강사/훈련생 계정 삭제 — soft delete. */
    @Transactional
    public void delete(Long id) {
        userRepository.delete(get(id));
    }

    /* ===== 프로필 수정 (User 보유 필드만) ===== */

    @Transactional
    public void updateProfile(Long id, String name, String email, String phone, String birthDate,
                              String gender, String education, UserStatus status) {
        User user = get(id);
        user.updateProfile(name, email, phone, birthDate, gender, education);
        if (status != null) {
            user.changeStatus(status);
        }
    }
}
