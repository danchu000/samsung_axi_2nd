package com.ssa.lms.course;

import com.ssa.lms.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 차시 — 과목(Subject) 하위의 학습 단위. 콘텐츠(VOD/문서), 출결, 진도가 차시 기준으로 기록된다.
 * (클래스명은 jakarta.servlet HttpSession 과의 혼동을 피해 CourseSession, 테이블은 course_session)
 */
@Entity
@Table(name = "course_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(nullable = false, length = 200)
    private String title;

    /** 과목 내 차시 번호 (1부터) */
    @Column(name = "order_no", nullable = false)
    private int orderNo;

    /** 진행 예정일 (일정표) — 미정이면 null */
    @Column(name = "lesson_date")
    private LocalDate lessonDate;

    /** 인정 학습시간(분) — 출결/이수 판정에 사용 */
    @Column(name = "learning_minutes")
    private Integer learningMinutes;

    @Builder
    private CourseSession(String title, int orderNo, LocalDate lessonDate, Integer learningMinutes) {
        this.title = title;
        this.orderNo = orderNo;
        this.lessonDate = lessonDate;
        this.learningMinutes = learningMinutes;
    }

    void setSubject(Subject subject) {
        this.subject = subject;
    }

    public void update(String title, int orderNo, LocalDate lessonDate, Integer learningMinutes) {
        this.title = title;
        this.orderNo = orderNo;
        this.lessonDate = lessonDate;
        this.learningMinutes = learningMinutes;
    }
}
