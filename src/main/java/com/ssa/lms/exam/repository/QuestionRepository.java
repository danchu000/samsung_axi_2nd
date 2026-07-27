package com.ssa.lms.exam.repository;

import com.ssa.lms.exam.entity.Difficulty;
import com.ssa.lms.exam.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    Optional<Question> findByQuestionCode(String questionCode);

    boolean existsByQuestionCode(String questionCode);

    /**
     * 문제은행 목록 검색.
     * 화면(admin-evaluation-question-bank.html)의 필터: 제목 / 난이도 / 카테고리 / 상태.
     * null 파라미터는 조건에서 제외된다.
     */
    @Query("""
            select q from Question q
            where (:keyword is null or lower(q.questionText) like lower(concat('%', :keyword, '%')))
              and (:difficulty is null or q.difficulty = :difficulty)
              and (:categoryL is null or q.categoryL = :categoryL)
              and (:status is null or q.status = :status)
            """)
    Page<Question> search(@Param("keyword") String keyword,
                          @Param("difficulty") Difficulty difficulty,
                          @Param("categoryL") String categoryL,
                          @Param("status") Question.QuestionStatus status,
                          Pageable pageable);

    /**
     * 문제별 "사용중인 과정 수" — 그 문제가 편성된 시험들이 속한 과정의 distinct 개수.
     * 목록 렌더링에서 행마다 조회하면 N+1 이 되므로 id 묶음으로 한 번에 집계한다.
     * 반환: [questionId(Long), courseCount(Long)]
     */
    @Query("""
            select eq.question.id, count(distinct eq.exam.course.id)
            from ExamQuestion eq
            where eq.question.id in :questionIds
            group by eq.question.id
            """)
    List<Object[]> countUsedCourses(@Param("questionIds") Collection<Long> questionIds);

    /**
     * 문제별 "평균 성취도" — 채점 완료된 답안 중 정답 비율.
     * 반환: [questionId(Long), correctRatio(Double 0.0~1.0)]
     */
    @Query("""
            select a.question.id,
                   avg(case when a.correct = true then 1.0 else 0.0 end)
            from Answer a
            where a.correct is not null
              and a.question.id in :questionIds
            group by a.question.id
            """)
    List<Object[]> averageAchievement(@Param("questionIds") Collection<Long> questionIds);

    /**
     * 문제 코드 자동 채번용 — 가장 큰 코드 1건.
     *
     * <p><b>네이티브 쿼리를 쓰는 이유:</b> Question 에는 {@code @SQLRestriction("is_deleted = false")}
     * 가 걸려 있어 JPQL 로 조회하면 삭제된 행이 빠진다. 그런데 soft delete 라 DB 에는 그 행이
     * 그대로 남아 있고 {@code question_code} 유니크 제약도 살아 있다. JPQL 로 max 를 구하면
     * 삭제된 최대 코드를 다시 발급해 유니크 제약 위반(500)이 난다.</p>
     *
     * <p>재현: Q-0004 등록 → 삭제 → 새 문제 등록 시 다시 Q-0004 를 발급하려다 실패.</p>
     */
    @Query(value = "select max(question_code) from question", nativeQuery = true)
    Optional<String> findMaxQuestionCode();

    /** 채번 중복 검사도 삭제된 행까지 봐야 한다 (위와 같은 이유). */
    @Query(value = "select count(*) > 0 from question where question_code = :code", nativeQuery = true)
    boolean existsByQuestionCodeIncludingDeleted(@Param("code") String code);

    /** 사용중인(ACTIVE) 문제만 — 시험 출제 시 후보 조회. */
    List<Question> findByStatusAndDifficulty(Question.QuestionStatus status, Difficulty difficulty);
}
