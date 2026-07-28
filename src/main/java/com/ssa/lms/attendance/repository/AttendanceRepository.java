package com.ssa.lms.attendance.repository;

import com.ssa.lms.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByCourseId(Long courseId);

    List<Attendance> findByCourseIdAndTraineeId(Long courseId, Long traineeId);

    /** 수강생 본인 출결 전체(훈련생 출결현황 화면 — 과정별 그룹핑은 서비스에서). */
    List<Attendance> findByTraineeId(Long traineeId);

    Optional<Attendance> findBySessionIdAndTraineeId(Long sessionId, Long traineeId);
}
