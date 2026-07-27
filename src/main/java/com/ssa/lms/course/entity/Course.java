package com.ssa.lms.course.entity;

import com.ssa.lms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 훈련 과정. 내역서 요건에 따라 데이터는 과정(course) 단위로 독립 수집되며,
 * 하위 도메인(시험/과제/출결 등)은 모두 course_id 를 FK 로 갖는다.
 * soft delete 적용 (3년 보존 요건).
 *
 * courseCode/courseName/cohort 필드명은 B 엔티티가 읽는 계약 (a-requests.md P0-3).
 */
@Entity
@Table(name = "course", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_code", columnNames = "course_code")
})
@SQLDelete(sql = "UPDATE course SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 과정 코드 — 화면 표기 형식: COURSE-2024-001 (PK 와 별개 컬럼) */
    @Column(name = "course_code", nullable = false, length = 30)
    private String courseCode;

    @Column(name = "course_name", nullable = false, length = 200)
    private String courseName;

    /** 기수 — 화면 표기 형식: "1기" */
    @Column(length = 20)
    private String cohort;

    /** 과정 분류 (ex. 클라우드, AI, 웹개발 …) */
    @Column(length = 50)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** 정원 */
    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseStatus status;

    /** 이수 기준: 진도율(%) — 상세 이수 기준(출석·평가 반영)은 Phase 2에서 확장 */
    @Column(name = "completion_progress_rate", nullable = false)
    private int completionProgressRate = 80;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNo ASC")
    private List<Subject> subjects = new ArrayList<>();

    @Builder
    private Course(String courseCode, String courseName, String cohort, String category,
                   String description, LocalDate startDate, LocalDate endDate, int capacity,
                   CourseStatus status, Integer completionProgressRate) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.cohort = cohort;
        this.category = category;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.capacity = capacity;
        this.status = status != null ? status : CourseStatus.DRAFT;
        this.completionProgressRate = completionProgressRate != null ? completionProgressRate : 80;
    }

    public void update(String courseName, String cohort, String category, String description,
                       LocalDate startDate, LocalDate endDate, int capacity, Integer completionProgressRate) {
        this.courseName = courseName;
        this.cohort = cohort;
        this.category = category;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.capacity = capacity;
        if (completionProgressRate != null) {
            this.completionProgressRate = completionProgressRate;
        }
    }

    public void changeStatus(CourseStatus status) {
        this.status = status;
    }

    public void addSubject(Subject subject) {
        subjects.add(subject);
        subject.setCourse(this);
    }
}
