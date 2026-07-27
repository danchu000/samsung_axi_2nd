package com.ssa.lms.exam.repository;

import com.ssa.lms.exam.entity.Exam;
import com.ssa.lms.exam.entity.Question;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 훈련생 응시 화면 전용 조회.
 *
 * ExamRepository(시험 생성 슬라이스 소유 파일)를 건드리지 않으려고 따로 뒀다.
 * 규칙(ExamQuestionRule)은 여기서 절대 읽지 않는다 — 실제 출제 문항의 단일 진실은
 * {@code Exam.examQuestions} 뿐이고, 규칙은 확정 전 조건일 뿐이라 응시 화면이 보면 안 된다.
 */
public interface ExamTakeRepository extends Repository<Exam, Long> {

    /**
     * 내가 수강 중인 과정의 공개 시험 목록.
     * DRAFT(작성중)/ARCHIVED(보관)는 훈련생에게 보이지 않는다 — 서비스가 statuses 로 걸러 넘긴다.
     * (JPQL in 절에 enum 리터럴을 박으면 Hibernate 버전별로 파싱이 갈려 파라미터로 받는다)
     * course/session 을 fetch join 하는 이유: 카드마다 과정명·차시명을 쓰는데 LAZY 면 N+1.
     */
    @Query("""
            select distinct e from Exam e
              join fetch e.course c
              left join fetch e.session s
            where c.id in :courseIds
              and e.status in :statuses
            order by e.windowStart asc
            """)
    List<Exam> findVisibleByCourses(@Param("courseIds") Collection<Long> courseIds,
                                    @Param("statuses") Collection<Exam.ExamStatus> statuses);

    /** 시험별 확정 문항 수. 0 이면 "규칙만 있고 확정 안 됨" → 응시 진입 불가. */
    @Query("""
            select eq.exam.id, count(eq.id)
            from ExamQuestion eq
            where eq.exam.id in :examIds
            group by eq.exam.id
            """)
    List<Object[]> countExamQuestions(@Param("examIds") Collection<Long> examIds);

    /** 시험별 문항 유형 분포 — 카드의 "객관식 + 주관식" 문구를 만든다. */
    @Query("""
            select eq.exam.id, eq.question.questionType, count(eq.id)
            from ExamQuestion eq
            where eq.exam.id in :examIds
            group by eq.exam.id, eq.question.questionType
            """)
    List<Object[]> countExamQuestionTypes(@Param("examIds") Collection<Long> examIds);

    /** 응시 시작 시점 — 확정 문항과 보기까지 한 번에. */
    @Query("""
            select distinct e from Exam e
              join fetch e.course
              left join fetch e.examQuestions eq
              left join fetch eq.question q
            where e.id = :id
            """)
    Optional<Exam> findWithExamQuestions(@Param("id") Long id);

    /** 객관식 보기 로딩 — 위 쿼리에 컬렉션을 하나 더 겹치면 카테시안 곱이 되므로 분리했다. */
    @Query("""
            select distinct q from Question q
              left join fetch q.choices
            where q.id in :questionIds
            """)
    List<Question> findQuestionsWithChoices(@Param("questionIds") Collection<Long> questionIds);
}
