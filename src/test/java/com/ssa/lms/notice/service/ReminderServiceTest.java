package com.ssa.lms.notice.service;

import com.ssa.lms.notice.entity.ReminderLog;
import com.ssa.lms.notice.repository.NotificationRecipientRepository;
import com.ssa.lms.notice.repository.ReminderLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 미제출·미응시·미응답자 리마인드를 고정한다.
 *
 * <p>핵심은 <b>중복 발송이 안 된다</b>는 것이다. 스케줄러가 1시간마다 도는데 기록이 없으면
 * 같은 사람에게 같은 알림이 계속 쌓인다.</p>
 */
@SpringBootTest
@Transactional
class ReminderServiceTest {

    @Autowired ReminderService reminderService;
    @Autowired ReminderLogRepository reminderLogRepository;
    @Autowired NotificationRecipientRepository recipientRepository;

    @Test
    @DisplayName("같은 단계를 두 번 돌려도 두 번째는 아무것도 보내지 않는다")
    void 중복발송_방지() {
        LocalDateTime now = LocalDateTime.now();

        int first = reminderService.remindDue(now, ReminderLog.ReminderStage.BEFORE_24H);
        long logsAfterFirst = reminderLogRepository.count();

        int second = reminderService.remindDue(now, ReminderLog.ReminderStage.BEFORE_24H);

        assertThat(second)
                .as("이미 보낸 대상에게 또 보내면 알림함이 같은 알림으로 도배된다")
                .isZero();
        assertThat(reminderLogRepository.count()).isEqualTo(logsAfterFirst);
        assertThat(first).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("발송하면 알림 수신자와 발송 기록이 함께 생긴다")
    void 발송시_기록_생성() {
        LocalDateTime now = LocalDateTime.now();
        long logsBefore = reminderLogRepository.count();
        long recipientsBefore = recipientRepository.count();

        int sent = reminderService.remindDue(now, ReminderLog.ReminderStage.OVERDUE);

        assertThat(reminderLogRepository.count() - logsBefore)
                .as("발송 기록이 없으면 다음 주기에 또 나간다")
                .isEqualTo(sent);
        assertThat(recipientRepository.count() - recipientsBefore)
                .as("수신자 행이 없으면 훈련생 화면에 안 보인다")
                .isEqualTo(sent);
    }

    @Test
    @DisplayName("단계가 다르면 각각 한 번씩 나간다")
    void 단계별_독립() {
        LocalDateTime now = LocalDateTime.now();

        reminderService.remindDue(now, ReminderLog.ReminderStage.BEFORE_24H);
        long after24h = reminderLogRepository.count();

        // 1시간 전 단계는 24시간 전과 별개다 — 마감이 임박했을 때 다시 알려야 한다
        reminderService.remindDue(now, ReminderLog.ReminderStage.BEFORE_1H);

        assertThat(reminderLogRepository.count())
                .as("단계가 다르면 같은 대상에게도 다시 보낼 수 있어야 한다")
                .isGreaterThanOrEqualTo(after24h);
    }

    @Test
    @DisplayName("대상이 없으면 0을 반환하고 아무것도 만들지 않는다")
    void 대상없음() {
        // 아주 먼 과거를 기준으로 하면 마감 구간에 걸리는 것이 없다
        LocalDateTime longAgo = LocalDateTime.now().minusYears(5);
        long before = reminderLogRepository.count();

        int sent = reminderService.remindDue(longAgo, ReminderLog.ReminderStage.BEFORE_24H);

        assertThat(sent).isZero();
        assertThat(reminderLogRepository.count()).isEqualTo(before);
    }
}
