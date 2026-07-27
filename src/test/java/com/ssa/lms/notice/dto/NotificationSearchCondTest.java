package com.ssa.lms.notice.dto;

import com.ssa.lms.notice.entity.Notification;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSearchCondTest {

    @Test
    void koreanPriorityMapsToDomainEnum() {
        NotificationSearchCond cond = new NotificationSearchCond("높음", "", null, null, "");
        assertThat(cond.priorityOrNull()).isEqualTo(Notification.Priority.HIGH);
        assertThat(cond.keywordOrNull()).isNull();
    }

    /** 쿼리스트링을 손으로 고친 값이 들어와도 500 이 나면 안 된다 (실행 검증에서 발견). */
    @Test
    void unknownPriorityIsTreatedAsNoFilter() {
        NotificationSearchCond cond = new NotificationSearchCond("bogus", "", null, null, "");
        assertThat(cond.priorityOrNull()).isNull();
    }

    @Test
    void lowercaseUrgentMapsToDomainEnum() {
        NotificationSearchCond cond = new NotificationSearchCond("urgent", "", null, null, "");
        assertThat(cond.priorityOrNull()).isEqualTo(Notification.Priority.URGENT);
    }
}
