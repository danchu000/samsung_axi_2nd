package com.ssa.lms.course;

import com.ssa.lms.common.BaseTimeEntity;
import com.ssa.lms.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 강사-과정 매핑 (한 과정에 복수 강사 배정 가능) */
@Entity
@Table(name = "course_instructor", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_instructor", columnNames = {"course_id", "user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseInstructor extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    /** role = INSTRUCTOR 인 사용자 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User instructor;

    /** 대표 강사 여부 */
    @Column(nullable = false)
    private boolean primaryInstructor = false;

    @Builder
    private CourseInstructor(Course course, User instructor, boolean primaryInstructor) {
        this.course = course;
        this.instructor = instructor;
        this.primaryInstructor = primaryInstructor;
    }
}
