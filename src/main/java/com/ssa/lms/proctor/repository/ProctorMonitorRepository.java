package com.ssa.lms.proctor.repository;

import com.ssa.lms.exam.entity.Exam;
import com.ssa.lms.exam.entity.ExamAttempt;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 시험 모니터링 <b>읽기 전용</b> 집계 조회.
 *
 * <p>{@code ExamAttemptRepository}(응시 슬라이스 소유 파일)를 건드리지 않으려고 따로 뒀다.
 * {@code Repository} 를 직접 상속해 save/delete 가 아예 노출되지 않는다 — 감독 기능은
 * 응시 데이터를 <b>읽기만</b> 한다. 유일한 쓰기는 무효 처리인데 그건 조회한 엔티티의
 * {@code voidAttempt()} 를 호출해 dirty checking 으로 처리한다.</p>
 *
 * <p>집계를 화면에서 행마다 돌리면 N+1 이 난다. 그래서 목록 화면이 필요한 수치는
 * (시험 목록 1회 + 상태별 응시 수 1회 + 응시자 수 1회) 로 끝낸다.</p>
 */
public interface ProctorMonitorRepository extends Repository<ExamAttempt, Long> {

    /* ===== 모니터링 목록 ===== */

    /**
     * 전체 시험 (관리자). DRAFT 는 아직 응시가 없으므로 서비스가 statuses 에서 빼고 넘긴다.
     * (JPQL 에 enum 리터럴을 박으면 Hibernate 버전별로 파싱이 갈린다 — HANDOFF §8 참고)
     */
    @Query("""
            select distinct e from Exam e
              join fetch e.course c
              left join fetch e.instructor i
            where e.status in :statuses
            order by e.windowStart desc
            """)
    List<Exam> findMonitoredExams(@Param("statuses") Collection<Exam.ExamStatus> statuses);

    /** 담당 과정 한정 (강사). 권한정의서 △ — 담당하지 않는 과정의 응시자는 볼 수 없다. */
    @Query("""
            select distinct e from Exam e
              join fetch e.course c
              left join fetch e.instructor i
            where e.status in :statuses
              and c.id in :courseIds
            order by e.windowStart desc
            """)
    List<Exam> findMonitoredExamsByCourses(@Param("statuses") Collection<Exam.ExamStatus> statuses,
                                           @Param("courseIds") Collection<Long> courseIds);

    /** [examId, status, count] — 시험별 상태 분포. */
    @Query("""
            select a.exam.id, a.status, count(a.id)
            from ExamAttempt a
            where a.exam.id in :examIds
            group by a.exam.id, a.status
            """)
    List<Object[]> countAttemptsByExamAndStatus(@Param("examIds") Collection<Long> examIds);

    /** [examId, 응시 기록이 있는 훈련생 수] — 미응시 = 수강생 수 - 이 값. */
    @Query("""
            select a.exam.id, count(distinct a.user.id)
            from ExamAttempt a
            where a.exam.id in :examIds
            group by a.exam.id
            """)
    List<Object[]> countDistinctAttemptUsersByExam(@Param("examIds") Collection<Long> examIds);

    /* ===== 실시간 감독 화면 ===== */

    @Query("""
            select e from Exam e
              join fetch e.course c
              left join fetch e.instructor i
            where e.id = :examId
            """)
    Optional<Exam> findExamForMonitoring(@Param("examId") Long examId);

    /** 시험 한 건의 응시자 전원. 이름 표시 때문에 user 를 fetch join 한다. */
    @Query("""
            select a from ExamAttempt a
              join fetch a.user u
            where a.exam.id = :examId
            order by u.name asc, a.attemptNo asc
            """)
    List<ExamAttempt> findAttemptsByExam(@Param("examId") Long examId);

    /**
     * [attemptId, severity, count] — 응시자별 이상행위 카운트.
     *
     * <p>{@code ExamEventLogRepository} 는 append-only 계약을 지키려고 조회 두 개만 두고 잠갔다.
     * 감독 화면용 집계는 여기(읽기 전용 리포지토리)에 둬서 그 계약을 건드리지 않는다.</p>
     */
    @Query("""
            select l.attempt.id, l.severity, count(l.id)
            from ExamEventLog l
            where l.attempt.id in :attemptIds
            group by l.attempt.id, l.severity
            """)
    List<Object[]> countEventsByAttemptAndSeverity(@Param("attemptIds") Collection<Long> attemptIds);

    /** 무효 처리/경고 발송 대상 1건. 과정 담당 판정까지 해야 해서 exam.course 까지 끌고 온다. */
    @Query("""
            select a from ExamAttempt a
              join fetch a.user u
              join fetch a.exam e
              join fetch e.course c
            where a.id = :attemptId
            """)
    Optional<ExamAttempt> findAttemptWithExamAndUser(@Param("attemptId") Long attemptId);
}
