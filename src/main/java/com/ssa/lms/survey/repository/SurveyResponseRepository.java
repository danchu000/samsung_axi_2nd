package com.ssa.lms.survey.repository;

import com.ssa.lms.survey.entity.SurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Long> {

    /**
     * 그 사용자가 이 설문에 이미 응답했는지.
     * 훈련생 목록의 파생 상태(SUBMITTED)와 재제출 차단에 함께 쓴다.
     *
     * 익명 설문은 user 가 null 로 저장되므로 여기서 항상 false 가 된다 —
     * 익명 설문의 재제출 차단은 DB 로 할 수 없다(누가 냈는지 모르니까).
     */
    boolean existsBySurveyIdAndUserId(Long surveyId, Long userId);

    Optional<SurveyResponse> findBySurveyIdAndUserId(Long surveyId, Long userId);

    /** 한 사용자가 응답한 설문 id 목록 — 훈련생 목록에서 한 번에 조회해 파생 상태를 만든다. */
    @Query("select r.survey.id from SurveyResponse r where r.user.id = :userId")
    List<Long> findRespondedSurveyIds(@Param("userId") Long userId);

    /**
     * 설문별 응답 건수(응답률 분자). 익명 응답도 행은 남으므로 함께 집계된다.
     * 반환: [surveyId(Long), responseCount(Long)]
     */
    @Query("""
            select r.survey.id, count(r.id)
            from SurveyResponse r
            where r.survey.id in :surveyIds
            group by r.survey.id
            """)
    List<Object[]> countBySurveyIds(@Param("surveyIds") Collection<Long> surveyIds);
}
