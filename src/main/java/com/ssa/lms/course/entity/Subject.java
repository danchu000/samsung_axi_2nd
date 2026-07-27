package com.ssa.lms.course.entity;

import com.ssa.lms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** 과목 — 과정(Course) 하위 구성 단위. 과목 아래에 차시(Session)가 있다. */
@Entity
@Table(name = "subject")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 과정 내 정렬 순서 (1부터) */
    @Column(name = "order_no", nullable = false)
    private int orderNo;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seq ASC")
    private List<Session> sessions = new ArrayList<>();

    @Builder
    private Subject(String name, String description, int orderNo) {
        this.name = name;
        this.description = description;
        this.orderNo = orderNo;
    }

    void setCourse(Course course) {
        this.course = course;
    }

    public void update(String name, String description, int orderNo) {
        this.name = name;
        this.description = description;
        this.orderNo = orderNo;
    }

    public void addSession(Session session) {
        sessions.add(session);
        session.setSubject(this);
    }
}
