package com.ssa.lms.exam.repository;

import com.ssa.lms.exam.entity.Difficulty;
import com.ssa.lms.exam.entity.Exam;
import com.ssa.lms.exam.entity.Question;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    /**
     * 시험 목록 검색.
     * 화면(admin-evaluation-test.html)의 필터: 과정 / 상태 / 강사명 / 시험명 검색어.
     * null 파라미터는 조건에서 제외된다.
     *
     * course/instructor 를 fetch join 하는 이유: 목록 행마다 과정명·강사명을 쓰는데
     * LAZY 로 두면 행 수만큼 추가 쿼리가 나간다 (N+1).
     *
     * statuses 는 null 을 허용하지 않는다. "전체"인 경우 서비스가 전체 상태 목록을 넣어준다
     * (JPQL 의 in 절에 null/빈 컬렉션을 넘기면 DB 별로 동작이 갈리기 때문).
     */
    @Query("""
            select distinct e from Exam e
              left join fetch e.course c
              left join fetch e.instructor i
            where (:courseId is null or c.id = :courseId)
              and e.status in :statuses
              and (:instructor is null or lower(i.name) like lower(concat('%', cast(:instructor as string), '%')))
              and (:keyword is null or lower(e.examName) like lower(concat('%', cast(:keyword as string), '%')))
            order by e.id desc
            """)
    List<Exam> search(@Param("courseId") Long courseId,
                      @Param("statuses") Collection<Exam.ExamStatus> statuses,
                      @Param("instructor") String instructor,
                      @Param("keyword") String keyword);

    /**
     * 시험별 편성 문항 수. 목록에서 행마다 세면 N+1 이라 id 묶음으로 한 번에 집계한다.
     * 반환: [examId(Long), questionCount(Long)]
     */
    @Query("""
            select eq.exam.id, count(eq.id)
            from ExamQuestion eq
            where eq.exam.id in :examIds
            group by eq.exam.id
            """)
    List<Object[]> countQuestions(@Param("examIds") Collection<Long> examIds);

    /** 수정 폼용 — 문항·규칙을 한 번에 끌어온다. */
    @Query("""
            select distinct e from Exam e
              left join fetch e.examQuestions eq
              left join fetch eq.question
            where e.id = :id
            """)
    Optional<Exam> findWithQuestions(@Param("id") Long id);

    /**
     * 자동 출제 규칙으로 뽑을 문항 후보.
     *
     * 이 쿼리를 QuestionRepository 가 아니라 여기에 둔 이유:
     * QuestionRepository 는 문제은행 슬라이스(PR #2) 소유 파일이라 손대지 않기로 했다.
     * Spring Data 의 @Query 는 리포지토리의 도메인 타입과 무관한 JPQL 도 실행할 수 있다.
     *
     * 태그는 쉼표 구분 문자열이라 부분일치로 본다 (Question.tags 는 암호화 컬럼이 아니라 LIKE 가능).
     */
    @Query("""
            select q from Question q
            where q.status = :status
              and (:difficulty is null or q.difficulty = :difficulty)
              and (:categoryL is null or q.categoryL = :categoryL)
              and (:categoryM is null or q.categoryM = :categoryM)
              and (:categoryS is null or q.categoryS = :categoryS)
              and (:tag is null or lower(q.tags) like lower(concat('%', cast(:tag as string), '%')))
            order by q.id asc
            """)
    List<Question> findRuleCandidates(@Param("status") Question.QuestionStatus status,
                                      @Param("difficulty") Difficulty difficulty,
                                      @Param("categoryL") String categoryL,
                                      @Param("categoryM") String categoryM,
                                      @Param("categoryS") String categoryS,
                                      @Param("tag") String tag,
                                      Pageable pageable);

    /** 응시 마감이 구간 안에 드는 공개 시험 — 리마인드 대상 산출용. */
    @Query("""
            select e from Exam e
              join fetch e.course
            where e.status = com.ssa.lms.exam.entity.Exam$ExamStatus.OPEN
              and e.windowEnd between :from and :to
            """)
    List<Exam> findWindowEndBetween(@Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);
}
