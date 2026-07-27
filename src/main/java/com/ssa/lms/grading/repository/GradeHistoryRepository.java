package com.ssa.lms.grading.repository;

import com.ssa.lms.grading.entity.GradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * 성적 변경 이력. <b>append-only</b> — 수정/삭제 메서드를 여기에 추가하지 마라.
 * 내역서 증빙 요건이라 한 번 남긴 정정 이력은 되돌릴 수 없어야 한다.
 *
 * <p>화면의 "변경 이력" 탭이 이 데이터를 그대로 보여준다
 * (과제: assignment-grading.html, 시험: 채점 팝업).</p>
 */
public interface GradeHistoryRepository extends JpaRepository<GradeHistory, Long> {

    /** 단건 조회 — 과제 채점 화면의 변경 이력 탭. */
    @Query("""
            select h from GradeHistory h
            join fetch h.changedBy
            where h.grade.id = :gradeId
            order by h.changedAt desc, h.id desc
            """)
    List<GradeHistory> findByGradeId(@Param("gradeId") Long gradeId);

    /**
     * 여러 성적의 이력을 한 번에 — 시험 채점 목록에서 행마다 조회하면 N+1 이라
     * id 묶음으로 모은다. 응시자 이름까지 함께 fetch 한다.
     */
    @Query("""
            select h from GradeHistory h
              join fetch h.changedBy
              join fetch h.grade g
              join fetch g.user
            where h.grade.id in :gradeIds
            order by h.changedAt desc, h.id desc
            """)
    List<GradeHistory> findByGrades(@Param("gradeIds") Collection<Long> gradeIds);

    /** 성적별 이력 건수. [gradeId(Long), count(Long)] */
    @Query("""
            select h.grade.id, count(h.id) from GradeHistory h
            where h.grade.id in :gradeIds
            group by h.grade.id
            """)
    List<Object[]> countByGrades(@Param("gradeIds") Collection<Long> gradeIds);
}
