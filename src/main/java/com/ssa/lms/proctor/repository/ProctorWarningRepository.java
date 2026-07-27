package com.ssa.lms.proctor.repository;

import com.ssa.lms.proctor.entity.ProctorWarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * 감독관이 발송한 경고.
 *
 * <p>{@code ExamEventLog}(시스템 자동 수집, append-only)와 달리 이쪽은 사람이 내린 조치라
 * "응시자가 확인함"(acknowledgedAt) 갱신이 허용된다. 다만 message/sentBy/sentAt 은 바꾸지 않는다 —
 * 제재 이력이라 사후 수정이 되면 증빙 가치가 없어진다.</p>
 */
public interface ProctorWarningRepository extends JpaRepository<ProctorWarning, Long> {

    List<ProctorWarning> findByAttemptIdOrderBySentAtDesc(Long attemptId);

    /** 응시자 화면 폴링용 — 아직 확인하지 않은 경고만. */
    List<ProctorWarning> findByAttemptIdAndAcknowledgedAtIsNullOrderBySentAtAsc(Long attemptId);

    /** [attemptId, 경고 수] — 감독 화면이 행마다 세지 않도록 한 번에. */
    @Query("""
            select w.attempt.id, count(w.id)
            from ProctorWarning w
            where w.attempt.id in :attemptIds
            group by w.attempt.id
            """)
    List<Object[]> countByAttemptIds(@Param("attemptIds") Collection<Long> attemptIds);
}
