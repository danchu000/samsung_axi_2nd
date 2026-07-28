package com.ssa.lms.user;

import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * A-1(사용자 관리) 도메인 시드 — 가입 승인 화면 시연용 <b>PENDING 계정</b>.
 *
 * <p>PARALLEL.md 규칙: 공용 {@code LocalDataInitializer}(@Order(0)) 는 동결이므로 도메인 시드는
 * 별도 러너로 추가한다(A-1 = {@code @Order(10)}). 기본 시더가 먼저 실행되어 계정이 이미 있으므로
 * 여기서는 승인 대기 계정만 보강한다.</p>
 */
@Slf4j
@Component
@Profile("local")
@Order(10)
@RequiredArgsConstructor
public class UserLocalDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.countByStatus(UserStatus.PENDING) > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();

        userRepository.save(User.builder()
                .loginId("trainee_pending").password(passwordEncoder.encode("1234"))
                .name("박훈련").role(Role.TRAINEE).status(UserStatus.PENDING)
                .email("trainee.pending@ssa.local").phone("010-3333-3333").birthDate("2000-05-05")
                .profileImageUrl("/static/img/avatars/trainee_pending.svg")
                .privacyConsentAt(now).thirdPartyConsentAt(now)
                .build());

        userRepository.save(User.builder()
                .loginId("instructor_pending").password(passwordEncoder.encode("1234"))
                .name("최강사").role(Role.INSTRUCTOR).status(UserStatus.PENDING)
                .email("instructor.pending@ssa.local").phone("010-4444-4444").birthDate("1988-09-09")
                .profileImageUrl("/static/img/avatars/instructor_pending.svg")
                .privacyConsentAt(now).thirdPartyConsentAt(now)
                .build());

        log.info("[local] 승인 대기 시드 생성 — trainee_pending / instructor_pending (PENDING)");
    }
}
