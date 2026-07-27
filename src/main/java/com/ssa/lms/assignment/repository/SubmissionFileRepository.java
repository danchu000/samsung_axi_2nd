package com.ssa.lms.assignment.repository;

import com.ssa.lms.assignment.entity.SubmissionFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubmissionFileRepository extends JpaRepository<SubmissionFile, Long> {

    List<SubmissionFile> findBySubmissionId(Long submissionId);

    /** 다운로드 권한 판정에 제출자·과제 정보가 필요하다. */
    @Query("""
            select f from SubmissionFile f
            join fetch f.submission s
            join fetch s.user
            join fetch s.courseAssignment
            where f.id = :id
            """)
    Optional<SubmissionFile> findDetailById(@Param("id") Long id);
}
