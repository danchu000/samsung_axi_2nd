package com.ssa.lms.dashboard.service;

import com.ssa.lms.exam.entity.ExamAttempt;
import com.ssa.lms.proctor.entity.ExamEventLog;
import com.ssa.lms.proctor.repository.ProctorMonitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 대시보드용 이상행위(WARN/CRITICAL) 건수 집계.
 *
 * <p>감독 슬라이스가 이미 만들어 둔 <b>읽기 전용</b> 리포지토리
 * {@link ProctorMonitorRepository#countEventsByAttemptAndSeverity} 를 그대로 쓴다.
 * attemptId × severity 로 묶어 한 번에 세기 때문에 시험 수만큼 쿼리가 늘지 않는다.</p>
 *
 * <p>응시 회차 조회는 시험당 1회다 — 대시보드가 세는 대상은 "지금 응시가 진행 중인 시험"
 * 몇 건뿐이라(감독 목록에서 inProgress &gt; 0 인 행) 전체 시험을 훑지 않는다.</p>
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProctorAnomalyCounter {

    private final ProctorMonitorRepository proctorMonitorRepository;

    /**
     * 시험별 이상행위 건수 (WARN + CRITICAL).
     *
     * @param examIds 집계 대상 시험. 비어 있으면 빈 맵
     * @return examId → 이상행위 건수. 이벤트가 없는 시험은 0
     */
    public Map<Long, Long> countByExam(List<Long> examIds) {
        Map<Long, Long> result = new HashMap<>();
        if (examIds == null || examIds.isEmpty()) {
            return result;
        }

        Map<Long, Long> examByAttempt = new HashMap<>();
        List<Long> attemptIds = new ArrayList<>();
        for (Long examId : examIds) {
            result.put(examId, 0L);
            for (ExamAttempt attempt : proctorMonitorRepository.findAttemptsByExam(examId)) {
                attemptIds.add(attempt.getId());
                examByAttempt.put(attempt.getId(), examId);
            }
        }
        if (attemptIds.isEmpty()) {
            return result;
        }

        for (Object[] row : proctorMonitorRepository.countEventsByAttemptAndSeverity(attemptIds)) {
            ExamEventLog.Severity severity = (ExamEventLog.Severity) row[1];
            // INFO 는 정상 흐름 로그(입장/저장)라 이상행위가 아니다. 여기 섞으면 전부 위험해 보인다.
            if (severity == ExamEventLog.Severity.INFO) {
                continue;
            }
            Long examId = examByAttempt.get((Long) row[0]);
            if (examId != null) {
                result.merge(examId, (Long) row[2], Long::sum);
            }
        }
        return result;
    }
}
