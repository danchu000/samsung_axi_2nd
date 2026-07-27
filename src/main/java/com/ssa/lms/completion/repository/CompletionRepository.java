package com.ssa.lms.completion.repository;

import com.ssa.lms.completion.entity.Completion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompletionRepository extends JpaRepository<Completion, Long> {

    List<Completion> findByCourseId(Long courseId);

    Optional<Completion> findByCourseIdAndTraineeId(Long courseId, Long traineeId);
}
