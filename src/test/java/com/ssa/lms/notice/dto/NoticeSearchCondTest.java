package com.ssa.lms.notice.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeSearchCondTest {

    @Test
    void customEndDateIncludesTheEntireSelectedDay() {
        NoticeSearchCond cond = new NoticeSearchCond(null, "custom",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), " 공지 ");

        assertThat(cond.fromOrNull()).isEqualTo(LocalDate.of(2026, 7, 1).atStartOfDay());
        assertThat(cond.toOrNull()).isEqualTo(LocalDate.of(2026, 8, 1).atStartOfDay());
        assertThat(cond.keywordOrNull()).isEqualTo("공지");
    }
}
