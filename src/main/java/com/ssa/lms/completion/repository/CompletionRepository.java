package com.ssa.lms.completion.repository;

import com.ssa.lms.completion.entity.Completion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompletionRepository extends JpaRepository<Completion, Long> {

    List<Completion> findByCourseId(Long courseId);

    /** 수강생 본인 이수 전체(훈련생 이수관리 화면). */
    List<Completion> findByTraineeId(Long traineeId);

    Optional<Completion> findByCourseIdAndTraineeId(Long courseId, Long traineeId);
}
