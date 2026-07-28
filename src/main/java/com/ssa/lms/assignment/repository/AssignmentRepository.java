package com.ssa.lms.assignment.repository;

import com.ssa.lms.assignment.entity.Assignment;
import com.ssa.lms.exam.entity.Difficulty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * 과제 "정의"(재사용 원본) 리포지토리.
 *
 * 훈련생이 보고 제출하는 단위는 여기가 아니라 {@code CourseAssignment} 다.
 * 이 테이블은 콘텐츠 은행(admin-evaluation-question-bank.html 의 "과제" 탭)과
 * 과정 배정 화면 Step 2 의 후보 목록에만 쓰인다.
 */
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    /**
     * 정의 검색 — 배정 화면 Step 2 필터(제목/난이도/카테고리/상태)에 1:1 대응.
     * null 파라미터는 조건에서 제외된다.
     */
    @Query("""
            select a from Assignment a
            where (:keyword is null or lower(a.title) like lower(concat('%', cast(:keyword as string), '%')))
              and (:difficulty is null or a.difficulty = :difficulty)
              and (:category is null or a.category = :category)
              and (:status is null or a.status = :status)
            order by a.id desc
            """)
    List<Assignment> search(@Param("keyword") String keyword,
                            @Param("difficulty") Difficulty difficulty,
                            @Param("category") String category,
                            @Param("status") Assignment.AssignmentStatus status);

    /**
     * 정의별 "사용중인 과정 수" — 콘텐츠 은행 목록 컬럼.
     * 행마다 조회하면 N+1 이므로 id 묶음으로 한 번에 집계한다.
     * 반환: [assignmentId(Long), courseCount(Long)]
     */
    @Query("""
            select ca.assignment.id, count(distinct ca.course.id)
            from CourseAssignment ca
            where ca.assignment.id in :assignmentIds
            group by ca.assignment.id
            """)
    List<Object[]> countUsedCourses(@Param("assignmentIds") Collection<Long> assignmentIds);
}
