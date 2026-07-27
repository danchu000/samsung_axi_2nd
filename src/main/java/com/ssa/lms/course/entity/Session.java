package com.ssa.lms.course.entity;

import com.ssa.lms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 차시 — 과목(Subject) 하위의 학습 단위. 콘텐츠(VOD/문서), 출결, 진도가 차시 기준으로 기록된다.
 * 클래스명 Session, 필드 seq/name 은 B 엔티티가 읽는 계약 (a-requests.md P0-3).
 * (테이블명은 DB 예약어 충돌을 피해 course_session 유지)
 */
@Entity
@Table(name = "course_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Session extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    /** 과목 내 차시 번호 (1부터) */
    @Column(nullable = false)
    private int seq;

    /** 차시명 */
    @Column(nullable = false, length = 200)
    private String name;

    /** 진행 예정일 (일정표) — 미정이면 null */
    @Column(name = "lesson_date")
    private LocalDate lessonDate;

    /** 인정 학습시간(분) — 출결/이수 판정에 사용 */
    @Column(name = "learning_minutes")
    private Integer learningMinutes;

    @Builder
    private Session(int seq, String name, LocalDate lessonDate, Integer learningMinutes) {
        this.seq = seq;
        this.name = name;
        this.lessonDate = lessonDate;
        this.learningMinutes = learningMinutes;
    }

    void setSubject(Subject subject) {
        this.subject = subject;
    }

    public void update(int seq, String name, LocalDate lessonDate, Integer learningMinutes) {
        this.seq = seq;
        this.name = name;
        this.lessonDate = lessonDate;
        this.learningMinutes = learningMinutes;
    }
}
