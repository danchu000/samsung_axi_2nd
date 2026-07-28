package com.ssa.lms.user.service;

import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 본인(관리자/강사/훈련생 공통) 프로필 조회·수정 및 비밀번호 변경.
 *
 * <ul>
 *   <li>수정 가능한 항목은 <b>User 엔티티가 실제로 보유한 필드만</b>
 *       (이름·이메일·전화·생년월일·성별·학력). loginId·role·status 는 본인이 바꿀 수 없다.</li>
 *   <li>비밀번호 변경은 <b>현재 비밀번호 확인 필수</b> — bcrypt {@code matches} 로 검증.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class MyInfoService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User get(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + id));
    }

    /** 본인 프로필 수정 — 상태/역할/아이디는 대상에서 제외. */
    @Transactional
    public void updateProfile(Long id, String name, String email, String phone, String birthDate,
                              String gender, String education) {
        User user = get(id);
        user.updateProfile(name, email, phone, birthDate, gender, education);
    }

    /**
     * 비밀번호 변경. 현재 비밀번호가 일치하지 않으면 {@link IllegalArgumentException}.
     */
    @Transactional
    public void changePassword(Long id, String currentRaw, String newRaw) {
        User user = get(id);
        if (currentRaw == null || !passwordEncoder.matches(currentRaw, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        if (newRaw == null || newRaw.isBlank()) {
            throw new IllegalArgumentException("새 비밀번호를 입력해 주세요.");
        }
        user.changePassword(passwordEncoder.encode(newRaw));
    }
}
