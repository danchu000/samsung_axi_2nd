package com.ssa.lms.grading.repository;

import com.ssa.lms.grading.entity.GradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 성적 변경 이력. append-only — update/delete 를 하는 코드가 있으면 그게 버그다.
 * 화면의 "변경 이력" 탭(assignment-grading.html)이 이 데이터를 그대로 보여준다.
 */
public interface GradeHistoryRepository extends JpaRepository<GradeHistory, Long> {

    @Query("""
            select h from GradeHistory h
            join fetch h.changedBy
            where h.grade.id = :gradeId
            order by h.changedAt desc, h.id desc
            """)
    List<GradeHistory> findByGradeId(@Param("gradeId") Long gradeId);
}
