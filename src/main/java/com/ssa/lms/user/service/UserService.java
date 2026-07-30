package com.ssa.lms.user.service;

import com.ssa.lms.auth.web.SignupForm;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 회원(강사/훈련생) 가입 로직.
 *
 * <ul>
 *   <li>비밀번호는 bcrypt 해시로 저장 (내역서 증빙 요건)</li>
 *   <li>가입 직후 상태는 {@link UserStatus#PENDING} — 관리자 승인 후 로그인 가능</li>
 *   <li>개인정보 수집·제3자 제공 동의 시각을 기록 (내역서 필수)</li>
 *   <li>ADMIN 은 self-signup 불가 (관리자 등록은 관리자 화면에서만)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User signup(SignupForm form) {
        Role role = parseSelfSignupRole(form.getRole());

        if (userRepository.existsByLoginId(form.getLoginId())) {
            throw new DuplicateLoginIdException(form.getLoginId());
        }

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .loginId(form.getLoginId())
                .password(passwordEncoder.encode(form.getPassword()))
                .name(form.getName())
                .role(role)
                .status(UserStatus.PENDING)
                .email(form.getEmail())
                .phone(form.getPhone())
                .birthDate(form.getBirthDate())
                .gender(form.getGender())
                .education(form.getEducation())
                .privacyConsentAt(now)
                .thirdPartyConsentAt(now)
                .build();
        User saved = userRepository.save(user);
        // 가입 화면에 사진 업로드가 없으므로 기본 아바타(이니셜+색상 SVG)를 연결한다
        saved.assignProfileImage(DefaultAvatars.urlFor(saved.getId()));
        return saved;
    }

    /** 자가 가입이 허용되는 역할만 반환 (TRAINEE / INSTRUCTOR). */
    private Role parseSelfSignupRole(String role) {
        Role parsed;
        try {
            parsed = Role.valueOf(role);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("가입할 수 없는 회원 유형입니다: " + role);
        }
        if (parsed == Role.ADMIN) {
            throw new IllegalArgumentException("관리자는 회원가입으로 등록할 수 없습니다.");
        }
        return parsed;
    }
}
