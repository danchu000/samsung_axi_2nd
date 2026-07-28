package com.ssa.lms.survey.repository;

import com.ssa.lms.survey.entity.SurveyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 설문 결과 리포트 집계 전용.
 *
 * <p>문항 수만큼 쿼리를 날리지 않도록 설문 하나당 <b>유형별로 한 번씩</b>만 묶어서 집계한다
 * (문항 20개짜리 설문에서 행마다 조회하면 그대로 N+1). 반환은 전부 {@code Object[]} 라
 * 서비스에서 {@code Map} 으로 접어 쓴다.</p>
 *
 * <p>{@code a.response.survey.id} 로 타고 들어가므로 {@code SurveyResponse} 의
 * {@code @SQLRestriction(is_deleted = false)} 이 함께 걸린다 — 보존 삭제된 응답은 집계에서 빠진다.</p>
 */
public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, Long> {

    /**
     * 문항별 <b>응답자 수</b>. 복수 선택(MULTI)은 한 사람이 여러 행을 만들기 때문에
     * 행 수가 아니라 응답(제출) 기준으로 distinct 를 건다 — 비율 분모가 100%를 넘지 않게.
     * 반환: [questionId(Long), respondentCount(Long)]
     */
    @Query("""
            select a.question.id, count(distinct a.response.id)
            from SurveyAnswer a
            where a.response.survey.id = :surveyId
            group by a.question.id
            """)
    List<Object[]> countRespondentsByQuestion(@Param("surveyId") Long surveyId);

    /**
     * 보기별 선택 수 (SINGLE / MULTI).
     * 반환: [questionId(Long), choiceId(Long), count(Long)]
     */
    @Query("""
            select a.question.id, a.choice.id, count(a.id)
            from SurveyAnswer a
            where a.response.survey.id = :surveyId and a.choice is not null
            group by a.question.id, a.choice.id
            """)
    List<Object[]> countByChoice(@Param("surveyId") Long surveyId);

    /**
     * 척도값 분포 (SCALE). 평균도 이 분포에서 계산한다 (쿼리를 한 번 더 날리지 않으려고).
     * 반환: [questionId(Long), scaleValue(Integer), count(Long)]
     */
    @Query("""
            select a.question.id, a.scaleValue, count(a.id)
            from SurveyAnswer a
            where a.response.survey.id = :surveyId and a.scaleValue is not null
            group by a.question.id, a.scaleValue
            """)
    List<Object[]> countByScaleValue(@Param("surveyId") Long surveyId);

    /**
     * 주관식 원문 (TEXT). 집계할 수 없는 값이라 그대로 내린다.
     * <b>응답자 이름은 함께 내리지 않는다</b> — 익명 설문이 아니어도 자유 서술은 신원과 붙는 순간
     * 열람 범위가 달라진다.
     * 반환: [questionId(Long), answerText(String)]
     */
    @Query("""
            select a.question.id, a.answerText
            from SurveyAnswer a
            where a.response.survey.id = :surveyId and a.answerText is not null
            order by a.question.id asc, a.id asc
            """)
    List<Object[]> findTextAnswers(@Param("surveyId") Long surveyId);
}
