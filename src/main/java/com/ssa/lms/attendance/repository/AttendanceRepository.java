package com.ssa.lms.attendance.repository;

import com.ssa.lms.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByCourseId(Long courseId);

    List<Attendance> findByCourseIdAndTraineeId(Long courseId, Long traineeId);

    Optional<Attendance> findBySessionIdAndTraineeId(Long sessionId, Long traineeId);
}
