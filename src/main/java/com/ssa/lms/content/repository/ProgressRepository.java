package com.ssa.lms.content.repository;

import com.ssa.lms.content.entity.ContentStatus;
import com.ssa.lms.content.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProgressRepository extends JpaRepository<Progress, Long> {

    Optional<Progress> findByUserIdAndContentId(Long userId, Long contentId);

    List<Progress> findByUserId(Long userId);

    /** 특정 과정에 속한 콘텐츠에 대한 이 사용자의 진도 기록. */
    @Query("""
            select p from Progress p
            where p.user.id = :userId and p.content.course.id = :courseId
            """)
    List<Progress> findByUserIdAndCourseId(@Param("userId") Long userId,
                                           @Param("courseId") Long courseId);

    /**
     * 과정 진도율 계산용 — 사용자의 진도율 합계.
     * 분모(콘텐츠 총 개수)는 {@link ContentRepository#countByCourseIdAndStatus} 로 구한다.
     */
    @Query("""
            select coalesce(sum(p.progressRate), 0) from Progress p
            where p.user.id = :userId and p.content.course.id = :courseId
              and p.content.status = :status
            """)
    long sumProgressRate(@Param("userId") Long userId,
                         @Param("courseId") Long courseId,
                         @Param("status") ContentStatus status);

    /** 과정 내 필수 콘텐츠(활성) 총 개수. */
    @Query("""
            select count(c) from Content c
            where c.course.id = :courseId and c.status = :status and c.required = true
            """)
    long countRequiredContents(@Param("courseId") Long courseId,
                               @Param("status") ContentStatus status);

    /** 과정 내 필수 콘텐츠(활성) 중 이 사용자가 완료한 개수. */
    @Query("""
            select count(p) from Progress p
            where p.user.id = :userId and p.content.course.id = :courseId
              and p.content.status = :status and p.content.required = true and p.completed = true
            """)
    long countCompletedRequired(@Param("userId") Long userId,
                                @Param("courseId") Long courseId,
                                @Param("status") ContentStatus status);
}
