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

/**
 * A-1(내 정보/관리자 관리) 도메인 시드 — 관리자 관리 화면 시연용 <b>추가 관리자 계정</b>.
 *
 * <p>기본 시더({@code LocalDataInitializer}, @Order(0))가 {@code admin} 1명을 만들므로,
 * 관리자 목록·비활성화·"마지막 활성 관리자 보호"를 시연하려면 관리자가 여럿 필요하다.
 * PARALLEL-P3 규칙에 따라 공용 시더는 동결하고 별도 러너(@Order(30))로 보강한다.</p>
 */
@Slf4j
@Component
@Profile("local")
@Order(30)
@RequiredArgsConstructor
public class AdminAccountLocalDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin("admin2", "이관리", "female", "admin2@ssa.local", "010-2222-0002", "1990-02-02");
        seedAdmin("admin3", "박운영", "male", "admin3@ssa.local", "010-2222-0003", "1985-07-07");
    }

    private void seedAdmin(String loginId, String name, String gender, String email,
                           String phone, String birthDate) {
        if (userRepository.existsByLoginId(loginId)) {
            return;
        }
        userRepository.save(User.builder()
                .loginId(loginId).password(passwordEncoder.encode("1234"))
                .name(name).role(Role.ADMIN).status(UserStatus.ACTIVE)
                .email(email).phone(phone).birthDate(birthDate).gender(gender)
                .education("4년제 대학 졸업")
                .build());
        log.info("[local] 관리자 시드 생성 — {} (ADMIN/ACTIVE)", loginId);
    }
}
