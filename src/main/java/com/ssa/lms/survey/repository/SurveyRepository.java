package com.ssa.lms.survey.repository;

import com.ssa.lms.survey.entity.Survey;
import com.ssa.lms.survey.entity.SurveyQuestion;
import java.time.LocalDateTime;
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

    /**
     * 문항까지 한 번에 — 응답 화면과 수정 폼이 N+1 없이 쓰도록.
     *
     * 주의: 여기서 q.choices 까지 같이 fetch join 하면
     * MultipleBagFetchException("cannot simultaneously fetch multiple bags") 이 난다.
     * List 컬렉션(bag) 두 개를 동시에 조인할 수 없기 때문이다.
     * 보기는 {@link #fetchChoices} 로 한 번 더 조회해 영속성 컨텍스트에 채운다.
     */
    @Query("""
            select distinct s from Survey s
            left join fetch s.questions
            where s.id = :id
            """)
    Optional<Survey> findWithQuestions(@Param("id") Long id);

    /**
     * 위 조회 직후 호출해 보기를 채운다. 같은 트랜잭션 안이면 1차 캐시에 올라가
     * 이후 q.getChoices() 가 추가 쿼리 없이 동작한다.
     */
    @Query("""
            select distinct q from SurveyQuestion q
            left join fetch q.choices
            where q.survey.id = :id
            """)
    List<SurveyQuestion> fetchChoices(@Param("id") Long id);

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

    /** 마감이 구간 안에 드는 진행중 설문 — 리마인드 대상 산출용. */
    @Query("""
            select s from Survey s
            where s.status = com.ssa.lms.survey.entity.Survey$SurveyStatus.ONGOING
              and s.endAt between :from and :to
            """)
    List<Survey> findEndingBetween(@Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);
}
