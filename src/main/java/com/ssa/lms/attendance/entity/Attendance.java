package com.ssa.lms.attendance.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.Session;
import com.ssa.lms.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

/**
 * 훈련생의 차시(Session)/일자별 출결 기록.
 *
 * <p>기본값은 <b>접속 기반</b>으로 자동 산정한다(해당 차시 진행일에 로그인/접속 이력이 있으면 출석).
 * 관리자가 수동 보정하면 {@code manual=true} 로 표시하고, 이후 재산정 시 자동 로직이 덮어쓰지 않는다.</p>
 *
 * <p>(user_id, session_id) 단위로 1행. 삭제는 soft delete(3년 보존 요건).</p>
 */
@Entity
@Table(name = "attendance", uniqueConstraints = {
        @UniqueConstraint(name = "uk_attendance_user_session", columnNames = {"user_id", "session_id"})
}, indexes = {
        @Index(name = "idx_attendance_course_user", columnList = "course_id, user_id")
})
@SQLDelete(sql = "UPDATE attendance SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 과정 — 과정 단위 출결 조회를 위해 비정규화(차시→과목→과정 대신 직접 보관). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private Session session;

    /** role = TRAINEE 인 수강생 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User trainee;

    /** 출결 기준 일자(차시 진행 예정일 스냅샷). 차시 일정 미정이면 null. */
    @Column(name = "attendance_date")
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    /** 해당 일자 접속(로그인) 횟수 — 산정 근거. */
    @Column(name = "access_count", nullable = false)
    private int accessCount;

    /** 관리자 수동 보정 여부 — true 면 자동 재산정에서 제외. */
    @Column(name = "manual", nullable = false)
    private boolean manual;

    @Column(length = 200)
    private String note;

    @Builder
    private Attendance(Course course, Session session, User trainee, LocalDate attendanceDate,
                       AttendanceStatus status, int accessCount, boolean manual, String note) {
        this.course = course;
        this.session = session;
        this.trainee = trainee;
        this.attendanceDate = attendanceDate;
        this.status = status != null ? status : AttendanceStatus.ABSENT;
        this.accessCount = accessCount;
        this.manual = manual;
        this.note = note;
    }

    /** 접속 기반 자동 산정 결과 반영(수동 보정 건은 호출부에서 제외). */
    public void applyAuto(AttendanceStatus status, int accessCount, LocalDate attendanceDate) {
        this.status = status;
        this.accessCount = accessCount;
        this.attendanceDate = attendanceDate;
        this.manual = false;
    }

    /** 관리자 수동 보정. */
    public void override(AttendanceStatus status, String note) {
        this.status = status;
        this.note = note;
        this.manual = true;
    }
}
