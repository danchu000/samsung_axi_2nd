package com.ssa.lms.course.repository;

import com.ssa.lms.course.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findBySubjectIdOrderBySeq(Long subjectId);

    List<Session> findBySubjectCourseIdOrderBySubjectOrderNoAscSeqAsc(Long courseId);
}
