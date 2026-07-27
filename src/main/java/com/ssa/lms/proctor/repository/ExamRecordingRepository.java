package com.ssa.lms.proctor.repository;

import com.ssa.lms.proctor.entity.ExamRecording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 시험 녹화 파일 조회.
 *
 * <p>녹화물은 개인영상정보다. 목록 조회 단계에서 이미 과정 단위로 걸러야 담당 강사가
 * 남의 과정 녹화를 훑는 일이 없다 — 그래서 courseIds 를 받는 조회를 따로 뒀다.</p>
 */
public interface ExamRecordingRepository extends JpaRepository<ExamRecording, Long> {

    @Query("""
            select r from ExamRecording r
              join fetch r.attempt a
              join fetch a.user u
              join fetch a.exam e
              join fetch e.course c
            order by r.recordedAt desc
            """)
    List<ExamRecording> findAllForList();

    @Query("""
            select r from ExamRecording r
              join fetch r.attempt a
              join fetch a.user u
              join fetch a.exam e
              join fetch e.course c
            where c.id in :courseIds
            order by r.recordedAt desc
            """)
    List<ExamRecording> findByCoursesForList(@Param("courseIds") Collection<Long> courseIds);

    /** 스트리밍 엔드포인트용 — 권한 판정에 과정까지 필요하다. */
    @Query("""
            select r from ExamRecording r
              join fetch r.attempt a
              join fetch a.user u
              join fetch a.exam e
              join fetch e.course c
            where r.id = :id
            """)
    Optional<ExamRecording> findForStreaming(@Param("id") Long id);

    boolean existsByAttemptId(Long attemptId);
}
