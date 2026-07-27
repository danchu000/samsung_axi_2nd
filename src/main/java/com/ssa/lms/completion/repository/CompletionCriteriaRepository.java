package com.ssa.lms.completion.repository;

import com.ssa.lms.completion.entity.CompletionCriteria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompletionCriteriaRepository extends JpaRepository<CompletionCriteria, Long> {

    Optional<CompletionCriteria> findByCourseId(Long courseId);
}
