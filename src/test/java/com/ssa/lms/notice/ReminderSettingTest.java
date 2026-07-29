package com.ssa.lms.notice;

import com.ssa.lms.notice.entity.ReminderLog;
import com.ssa.lms.notice.entity.ReminderSetting;
import com.ssa.lms.notice.service.ReminderService;
import com.ssa.lms.notice.service.ReminderSettingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자가 바꾼 리마인드 설정이 <b>실제 발송에 반영되는지</b> 고정한다.
 *
 * <p>설정 화면이 있어도 발송 로직이 그 값을 안 읽으면 아무 의미가 없다.
 * 화면에서 저장은 되는데 알림은 예전 시점대로 나가는, 제일 알아채기 어려운 고장이다.</p>
 */
@SpringBootTest
@Transactional
class ReminderSettingTest {

    @Autowired ReminderSettingService settingService;
    @Autowired ReminderService reminderService;

    @Test
    @DisplayName("설정이 없어도 기본값으로 동작한다 — 없다고 알림 기능이 멈추면 안 된다")
    void 없으면_기본값() {
        ReminderSetting s = settingService.current();

        assertThat(s.getFirstNoticeHours()).isEqualTo(24);
        assertThat(s.getSecondNoticeHours()).isEqualTo(1);
        assertThat(s.getOverdueDays()).isEqualTo(3);
        assertThat(s.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("관리자가 넣은 시점이 그대로 저장된다")
    void 저장된다() {
        ReminderSetting s = settingService.save(48, 6, 7, true, true, false, true);

        assertThat(s.getFirstNoticeHours()).isEqualTo(48);
        assertThat(s.getSecondNoticeHours()).isEqualTo(6);
        assertThat(s.getOverdueDays()).isEqualTo(7);
        assertThat(s.isSurveyEnabled()).isFalse();
    }

    @Test
    @DisplayName("1·2차를 바꿔 넣으면 자동으로 바로잡는다 — 순서가 뒤집히면 나중 알림이 먼저 간다")
    void 순서를_바로잡는다() {
        ReminderSetting s = settingService.save(1, 24, 3, true, true, true, true);

        assertThat(s.getFirstNoticeHours()).as("1차가 더 이른 시점이어야 한다").isEqualTo(24);
        assertThat(s.getSecondNoticeHours()).isEqualTo(1);
    }

    @Test
    @DisplayName("범위를 벗어난 값은 허용값으로 맞춘다 — 저장을 거부하면 관리자가 헤맨다")
    void 범위를_맞춘다() {
        ReminderSetting s = settingService.save(99_999, 0, 999, true, true, true, true);

        assertThat(s.getFirstNoticeHours()).isEqualTo(ReminderSetting.MAX_HOURS);
        assertThat(s.getSecondNoticeHours()).isEqualTo(ReminderSetting.MIN_HOURS);
        assertThat(s.getOverdueDays()).isEqualTo(ReminderSetting.MAX_OVERDUE_DAYS);
    }

    @Test
    @DisplayName("전체를 끄면 한 건도 보내지 않는다 — 설정 화면의 스위치가 실제로 듣는지")
    void 끄면_안_보낸다() {
        settingService.save(24, 1, 3, true, true, true, false);

        int sent = 0;
        for (ReminderLog.ReminderStage stage : ReminderLog.ReminderStage.values()) {
            sent += reminderService.remindDue(LocalDateTime.now(), stage);
        }

        assertThat(sent)
                .as("화면에서 껐는데 계속 나가면 관리자가 손쓸 방법이 없다")
                .isZero();
    }

    @Test
    @DisplayName("대상을 모두 끄면 켜져 있어도 한 건도 안 나간다")
    void 대상을_모두_끄면_안_보낸다() {
        settingService.save(24, 1, 3, false, false, false, true);

        int sent = 0;
        for (ReminderLog.ReminderStage stage : ReminderLog.ReminderStage.values()) {
            sent += reminderService.remindDue(LocalDateTime.now(), stage);
        }

        assertThat(sent).isZero();
    }

    @Test
    @DisplayName("1·2차 간격이 1시간 미만이면 2차를 건너뛴다 — 같은 마감이 두 번 걸린다")
    void 간격이_붙으면_2차를_건너뛴다() {
        // 24시간 전과 24시간 전(=간격 0). 구간 폭이 1시간이라 같은 마감이 두 단계에 다 걸린다
        settingService.save(24, 24, 3, true, true, true, true);

        int second = reminderService.remindDue(
                LocalDateTime.now(), ReminderLog.ReminderStage.BEFORE_1H);

        assertThat(second)
                .as("단계가 다르면 ReminderLog 도 중복을 못 막는다")
                .isZero();
    }
}
