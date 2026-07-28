package com.ssa.lms.notice.service;

import com.ssa.lms.notice.dto.NotificationForm;
import com.ssa.lms.notice.entity.Notification;
import com.ssa.lms.notice.repository.NotificationRecipientRepository;
import com.ssa.lms.notice.repository.NotificationRepository;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 예약 알림 자동 발송을 고정한다.
 *
 * <p>이 기능이 없을 때는 {@code sendAt} 이 저장만 되고 시각이 도래해도 아무 일이 없었다.
 * 관리자가 "예약"으로 저장한 알림이 영영 나가지 않았다.</p>
 */
@SpringBootTest
@Transactional
class NotificationDispatchTest {

    @Autowired NotificationService notificationService;
    @Autowired NotificationRepository notificationRepository;
    @Autowired NotificationRecipientRepository recipientRepository;
    @Autowired UserRepository userRepository;

    private Long adminId;

    @BeforeEach
    void setUp() {
        adminId = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .findFirst()
                .map(User::getId)
                .orElseGet(() -> userRepository.save(User.builder()
                        .loginId("sched-admin-" + System.nanoTime())
                        .password("{noop}test")
                        .name("스케줄테스트관리자")
                        .role(Role.ADMIN)
                        .status(UserStatus.ACTIVE)
                        .build()).getId());
    }

    /** 예약 알림 1건 생성. sendAt 을 직접 지정한다. */
    private Long createScheduled(LocalDateTime sendAt) {
        NotificationForm form = new NotificationForm();
        form.setTitle("예약 발송 테스트");
        form.setContent("본문");
        form.setPriority("high");
        form.setStatus("scheduled");
        form.setTargetType("ALL");
        form.setSendAt(sendAt);
        return notificationService.create(form, adminId, Role.ADMIN);
    }

    @Test
    @DisplayName("발송 시각이 지난 예약 알림은 발송되고 수신자가 만들어진다")
    void 시각_도래시_발송() {
        Long id = createScheduled(LocalDateTime.now().minusMinutes(1));

        assertThat(notificationRepository.findById(id).orElseThrow().getStatus())
                .as("발송 전에는 SCHEDULED")
                .isEqualTo(Notification.NotificationStatus.SCHEDULED);
        assertThat(recipientRepository.findByNotificationIds(List.of(id)))
                .as("발송 전에는 수신자가 없어야 한다")
                .isEmpty();

        int sent = notificationService.dispatchDue(LocalDateTime.now());

        assertThat(sent).isGreaterThanOrEqualTo(1);
        assertThat(notificationRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(Notification.NotificationStatus.SENT);
        assertThat(recipientRepository.findByNotificationIds(List.of(id)))
                .as("발송되면 수신자가 펼쳐져야 한다")
                .isNotEmpty();
    }

    @Test
    @DisplayName("아직 시각이 안 된 예약 알림은 건드리지 않는다")
    void 시각_전에는_미발송() {
        Long id = createScheduled(LocalDateTime.now().plusHours(1));

        notificationService.dispatchDue(LocalDateTime.now());

        assertThat(notificationRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(Notification.NotificationStatus.SCHEDULED);
        assertThat(recipientRepository.findByNotificationIds(List.of(id))).isEmpty();
    }

    @Test
    @DisplayName("두 번 돌려도 수신자가 중복 생성되지 않는다 (멱등)")
    void 중복발송_방지() {
        Long id = createScheduled(LocalDateTime.now().minusMinutes(1));

        notificationService.dispatchDue(LocalDateTime.now());
        int firstCount = recipientRepository.findByNotificationIds(List.of(id)).size();

        // 이미 SENT 라 다시 잡히지 않아야 한다
        notificationService.dispatchDue(LocalDateTime.now());
        int secondCount = recipientRepository.findByNotificationIds(List.of(id)).size();

        assertThat(secondCount)
                .as("재실행해도 수신자가 늘면 uk_notification_recipient 위반 위험")
                .isEqualTo(firstCount);
    }

    @Test
    @DisplayName("발송할 예약이 없으면 0을 반환하고 아무것도 바꾸지 않는다")
    void 대상없음() {
        assertThat(notificationService.dispatchDue(LocalDateTime.now().minusYears(10)))
                .isZero();
    }
}
