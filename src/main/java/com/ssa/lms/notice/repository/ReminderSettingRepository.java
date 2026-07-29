package com.ssa.lms.notice.repository;

import com.ssa.lms.notice.entity.ReminderSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderSettingRepository extends JpaRepository<ReminderSetting, Long> {
}
