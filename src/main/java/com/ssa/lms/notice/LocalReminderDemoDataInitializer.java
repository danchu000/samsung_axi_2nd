package com.ssa.lms.notice;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * local 프로필 <b>독려 메일 검증용</b> 시드 (개발자 B) — <b>메일 주소가 없는 훈련생</b> 1명.
 *
 * <p><b>왜 필요한가:</b> 훈련생 이메일은 선택 입력이라 비어 있는 계정이 실제로 생긴다
 * (관리자 수기 등록, 동의 없이 가입 등). 그런 계정에 메일을 보내려 하면
 * {@code MailParseException} 이 나고, 그게 위로 새면 <b>그 뒤 사람들 발송이 통째로 멈춘다.</b>
 * 기존 시드 계정은 전부 이메일이 있어서 이 경로를 한 번도 밟아보지 못한 상태였다.</p>
 *
 * <p>{@code ReminderMailSender} 는 주소가 없으면 {@code NO_ADDRESS} 로 걸러내고
 * 인앱 알림만 나가야 한다. 그 동작을 실제로 확인하려고 이 계정을 심는다.</p>
 *
 * <p><b>A 소유 파일을 건드리지 않기 위해</b> {@code UserLocalDataInitializer} 에 추가하지 않고
 * B 패키지에 따로 뒀다. A 소유 리포지토리도 주입하지 않고 {@link EntityManager} 로만 쓴다.
 * local 프로필 한정이라 운영 데이터에 영향이 없다.</p>
 */
@Slf4j
@Component
@Profile("local")
@Order(100)
@RequiredArgsConstructor
public class LocalReminderDemoDataInitializer {

    private static final String LOGIN_ID = "trainee_nomail";

    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager em;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        Long exists = em.createQuery(
                        "select count(u) from User u where u.loginId = :id", Long.class)
                .setParameter("id", LOGIN_ID)
                .getSingleResult();
        if (exists > 0) {
            return;
        }

        Course course = em.createQuery(
                        "select c from Course c where c.courseCode = :code", Course.class)
                .setParameter("code", "COURSE-2026-001")
                .getResultStream().findFirst().orElse(null);
        if (course == null) {
            log.warn("[local] 데모 과정이 없어 메일 미보유 훈련생 시드를 건너뛴다");
            return;
        }

        // email 을 아예 넘기지 않는다 — 이게 이 시드의 전부다.
        User noMail = User.builder()
                .loginId(LOGIN_ID)
                .password(passwordEncoder.encode("1234"))
                .name("무메일훈련생")
                .role(Role.TRAINEE)
                .status(UserStatus.ACTIVE)
                .phone("010-9999-9999")
                .privacyConsentAt(LocalDateTime.now())
                .build();
        em.persist(noMail);

        // 리마인드 대상은 "승인/수료 상태 수강생"이라 APPROVED 로 넣어야 발송 대상에 든다.
        em.persist(Enrollment.builder()
                .trainee(noMail).course(course)
                .status(EnrollmentStatus.APPROVED)
                .appliedAt(LocalDateTime.now().minusDays(9))
                .build());

        log.info("[local] 메일 주소 없는 훈련생 시드 생성 — loginId={} (독려 메일 NO_ADDRESS 경로 검증용)",
                LOGIN_ID);
    }
}
