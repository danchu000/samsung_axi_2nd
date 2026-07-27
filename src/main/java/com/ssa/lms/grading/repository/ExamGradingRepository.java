package com.ssa.lms.grading.repository;

import com.ssa.lms.exam.entity.Answer;
import com.ssa.lms.exam.entity.Exam;
import com.ssa.lms.exam.entity.ExamAttempt;
import com.ssa.lms.exam.entity.Question;
import com.ssa.lms.grading.entity.Grade;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 시험 채점 화면이 쓰는 <b>읽기 전용</b> 조회 모음.
 *
 * <p>왜 {@code exam} 패키지의 리포지토리에 메서드를 추가하지 않았나 — 응시/제출 슬라이스와
 * 시험 생성 슬라이스가 각자 브랜치에서 같은 파일을 들고 있어 충돌 지점이 된다. Spring Data 의
 * {@code @Query} 는 리포지토리의 도메인 타입과 무관한 JPQL 도 실행할 수 있으므로
 * (ExamRepository.findRuleCandidates 가 이미 쓰는 방식) 채점이 필요한 조회는 전부 여기에 모았다.</p>
 *
 * <p>집계 쿼리를 굳이 만든 이유는 N+1 회피다. 시험 목록 한 페이지에 행마다 응시자·채점 수를
 * 세면 목록 하나에 수십 개의 쿼리가 나간다. 여기서는 id 묶음으로 한 번에 접는다.</p>
 */
public interface ExamGradingRepository extends Repository<ExamAttempt, Long> {

    /* ===================== 시험 ===================== */

    @Query("""
            select distinct e from Exam e
              left join fetch e.course c
              left join fetch e.instructor i
            where e.status in :statuses
              and (:courseId is null or c.id = :courseId)
              and (:instructor is null or lower(i.name) like lower(concat('%', :instructor, '%')))
              and (:keyword is null or lower(e.examName) like lower(concat('%', :keyword, '%')))
            order by e.id desc
            """)
    List<Exam> findExams(@Param("courseId") Long courseId,
                         @Param("statuses") Collection<Exam.ExamStatus> statuses,
                         @Param("instructor") String instructor,
                         @Param("keyword") String keyword);

    @Query("""
            select e from Exam e
              left join fetch e.course
              left join fetch e.instructor
            where e.id = :id
            """)
    Optional<Exam> findExam(@Param("id") Long id);

    /** 채점 팝업용 — 편성 문항과 문제 본문까지 한 번에. 보기는 별도 쿼리로 가져온다(카테시안 곱 방지). */
    @Query("""
            select distinct e from Exam e
              left join fetch e.course
              left join fetch e.instructor
              left join fetch e.examQuestions eq
              left join fetch eq.question
            where e.id = :id
            """)
    Optional<Exam> findExamWithQuestions(@Param("id") Long id);

    @Query("""
            select distinct q from Question q left join fetch q.choices
            where q.id in :ids
            """)
    List<Question> findQuestionsWithChoices(@Param("ids") Collection<Long> ids);

    /** 미응시자 행을 만들기 위한 이름 조회. A 소유 엔티티의 읽기 전용 조회다 (ExamRefRepository 와 같은 방침). */
    @Query("select u from com.ssa.lms.user.entity.User u where u.id in :ids order by u.name asc")
    List<com.ssa.lms.user.entity.User> findUsers(@Param("ids") Collection<Long> ids);

    /* ===================== 응시 회차 ===================== */

    /** 채점 대상 회차. 사용자까지 fetch 해서 목록에서 이름을 꺼낼 때 추가 쿼리가 안 나가게 한다. */
    @Query("""
            select a from ExamAttempt a
              join fetch a.user u
            where a.exam.id in :examIds and a.status in :statuses
            order by a.attemptNo asc
            """)
    List<ExamAttempt> findAttempts(@Param("examIds") Collection<Long> examIds,
                                   @Param("statuses") Collection<ExamAttempt.AttemptStatus> statuses);

    @Query("""
            select a from ExamAttempt a
              join fetch a.user
              join fetch a.exam e
              left join fetch e.course
            where a.id = :id
            """)
    Optional<ExamAttempt> findAttempt(@Param("id") Long id);

    /* ===================== 답안 ===================== */

    @Query("""
            select a from Answer a
              left join fetch a.choice
              join fetch a.question
            where a.attempt.id = :attemptId
            """)
    List<Answer> findAnswers(@Param("attemptId") Long attemptId);

    /**
     * 회차별 <b>수동 채점이 끝난</b> 문항 수. [attemptId(Long), count(Long)]
     *
     * <p>{@code autoGraded=false and score is not null} 이 "사람이 점수를 매겼다"의 정의다.
     * 응시자가 답만 쓰고 채점 전인 답안은 score 가 null 이라 세지 않는다.</p>
     */
    @Query("""
            select a.attempt.id, count(a.id) from Answer a
            where a.attempt.id in :attemptIds
              and a.autoGraded = false
              and a.score is not null
            group by a.attempt.id
            """)
    List<Object[]> countManualGraded(@Param("attemptIds") Collection<Long> attemptIds);

    /**
     * 시험별 수동 채점 대상 문항 수. [examId(Long), count(Long)]
     *
     * <p>{@code Question.isAutoGradable()} 을 JPQL 로 옮긴 것이라, 그쪽이 바뀌면 여기도 바꿔야 한다.
     * 그래서 자동 채점 유형 목록을 서비스가 파라미터로 넘긴다 (상수를 두 곳에 적지 않기 위해).</p>
     */
    @Query("""
            select eq.exam.id, count(eq.id) from ExamQuestion eq
            where eq.exam.id in :examIds
              and eq.question.questionType not in :autoTypes
            group by eq.exam.id
            """)
    List<Object[]> countManualQuestions(@Param("examIds") Collection<Long> examIds,
                                        @Param("autoTypes") Collection<Question.QuestionType> autoTypes);

    /* ===================== 성적 ===================== */

    /** 시험별 성적 상태 집계. [examId(Long), status(GradeStatus), count(Long)] */
    @Query("""
            select g.evalRefId, g.status, count(g.id) from Grade g
            where g.evalType = com.ssa.lms.grading.entity.Grade$EvalType.EXAM
              and g.evalRefId in :examIds
            group by g.evalRefId, g.status
            """)
    List<Object[]> countGradeStatuses(@Param("examIds") Collection<Long> examIds);

    /** 이 시험의 성적 행 전부 — 성적 목록/CSV 다운로드용. */
    @Query("""
            select g from Grade g
              join fetch g.user
              left join fetch g.gradedBy
              left join fetch g.confirmedBy
            where g.evalType = com.ssa.lms.grading.entity.Grade$EvalType.EXAM
              and g.evalRefId = :examId
            order by g.user.name asc, g.id asc
            """)
    List<Grade> findGradesByExam(@Param("examId") Long examId);
}
