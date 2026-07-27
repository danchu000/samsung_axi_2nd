package com.ssa.lms.content.repository;

import com.ssa.lms.content.entity.Content;
import com.ssa.lms.content.entity.ContentStatus;
import com.ssa.lms.content.entity.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentRepository extends JpaRepository<Content, Long> {

    List<Content> findByCourseIdOrderByOrderNoAscIdAsc(Long courseId);

    List<Content> findByCourseIdAndStatusOrderByOrderNoAscIdAsc(Long courseId, ContentStatus status);

    List<Content> findByCourseIdAndTypeOrderByOrderNoAscIdAsc(Long courseId, ContentType type);

    List<Content> findBySessionIdOrderByOrderNoAscIdAsc(Long sessionId);

    long countByCourseId(Long courseId);

    long countByCourseIdAndStatus(Long courseId, ContentStatus status);
}
