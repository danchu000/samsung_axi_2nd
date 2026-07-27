package com.ssa.lms.survey.repository;

import com.ssa.lms.survey.entity.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SurveyRepository extends JpaRepository<Survey, Long> {

    /**
     * 관리자 설문 목록 검색.
     * 화면(admin-attendance-survey.html)의 필터: 과정 / 상태 / 설문명.
     * null 파라미터는 조건에서 제외된다.
     */
    @Query("""
            select s from Survey s
            left join fetch s.course
            left join fetch s.session
            where (:courseId is null or s.course.id = :courseId)
              and (:status is null or s.status = :status)
              and (:keyword is null or lower(s.title) like lower(concat('%', :keyword, '%')))
            order by s.endAt asc, s.id asc
            """)
    List<Survey> search(@Param("courseId") Long courseId,
                        @Param("status") Survey.SurveyStatus status,
                        @Param("keyword") String keyword);

    /**
     * 훈련생 목록용 — DRAFT(작성중)는 훈련생에게 노출하지 않는다.
     * 제외할 상태는 파라미터로 넘긴다 (JPQL 안에 중첩 enum 리터럴을 쓰면 벤더별로 파싱이 갈린다).
     */
    @Query("""
            select s from Survey s
            left join fetch s.course
            left join fetch s.session
            where s.status <> :excluded
            order by s.endAt asc, s.id asc
            """)
    List<Survey> findVisibleToTrainee(@Param("excluded") Survey.SurveyStatus excluded);

    /** 문항·보기까지 한 번에 — 응답 화면과 수정 폼이 N+1 없이 쓰도록. */
    @Query("""
            select distinct s from Survey s
            left join fetch s.questions q
            left join fetch q.choices
            where s.id = :id
            """)
    Optional<Survey> findWithQuestions(@Param("id") Long id);

    /**
     * 설문별 문항 수. 목록에서 행마다 questions 를 건드리면 N+1 이라 id 묶음으로 집계한다.
     * 반환: [surveyId(Long), questionCount(Long)]
     */
    @Query("""
            select q.survey.id, count(q.id)
            from SurveyQuestion q
            where q.survey.id in :surveyIds
            group by q.survey.id
            """)
    List<Object[]> countQuestions(@Param("surveyIds") Collection<Long> surveyIds);
}
