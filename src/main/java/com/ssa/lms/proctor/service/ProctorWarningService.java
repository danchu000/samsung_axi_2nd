package com.ssa.lms.proctor.service;

import com.ssa.lms.exam.entity.ExamAttempt;
import com.ssa.lms.exam.repository.ExamRefRepository;
import com.ssa.lms.proctor.dto.ProctorWarningRow;
import com.ssa.lms.proctor.entity.ProctorWarning;
import com.ssa.lms.proctor.repository.ProctorWarningRepository;
import com.ssa.lms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이상행위 경고 발송 — 권한정의서(1) 16행: <b>관리자 O / 강사 △(경고까지)</b>.
 *
 * <p>{@code ExamEventLog} 는 시스템이 자동 수집한 정황이고 이 테이블은 사람이 내린 조치다.
 * 둘을 섞지 않는다 — 로그를 사람이 쓰기 시작하면 append-only 증빙이 무너진다.</p>
 *
 * <p>발송 메시지는 신뢰할 수 없는 입력이라 길이를 잘라 저장한다
 * ({@code ProctorWarning.message} 는 500자 컬럼이고, 넘치면 DB 예외로 500 이 난다).</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProctorWarningService {

    private static final int MESSAGE_MAX = 500;
    private static final String DEFAULT_MESSAGE = "부정행위가 의심됩니다. 시험 화면을 벗어나지 마세요.";

    private final ProctorWarningRepository proctorWarningRepository;
    private final ProctorMonitorService proctorMonitorService;
    private final ExamRefRepository examRefRepository;

    /**
     * 경고 발송. 관리자·강사 모두 가능하되 <b>강사는 담당 과정만</b>이다.
     * 담당 판정은 {@link ProctorMonitorService#assertCanMonitor} 가 한다.
     */
    @Transactional
    public ProctorWarningRow send(Long attemptId, ProctorViewer viewer, String message) {
        if (!viewer.isAdmin() && !viewer.isInstructor()) {
            throw new ProctorAccessDeniedException("경고 발송 권한이 없습니다.");
        }
        ExamAttempt attempt = proctorMonitorService.requireAttempt(attemptId);
        proctorMonitorService.assertCanMonitor(attempt.getExam(), viewer);

        User sender = examRefRepository.findUser(viewer.userId())
                .orElseThrow(() -> new IllegalStateException("발송자를 찾을 수 없습니다: " + viewer.userId()));

        ProctorWarning saved = proctorWarningRepository.save(ProctorWarning.builder()
                .attempt(attempt)
                .sentBy(sender)
                .message(normalize(message))
                .sentAt(LocalDateTime.now())
                .build());
        return toRow(saved, sender.getName());
    }

    /** 감독 화면의 경고 이력. */
    public List<ProctorWarningRow> findByAttempt(Long attemptId, ProctorViewer viewer) {
        ExamAttempt attempt = proctorMonitorService.requireAttempt(attemptId);
        proctorMonitorService.assertCanMonitor(attempt.getExam(), viewer);
        return proctorWarningRepository.findByAttemptIdOrderBySentAtDesc(attemptId).stream()
                .map(w -> toRow(w, w.getSentBy().getName()))
                .toList();
    }

    /**
     * 응시자 화면 폴링 — 아직 확인하지 않은 경고.
     *
     * <p>URL 의 attemptId 만 바꿔 남의 경고를 보는 걸 막아야 해서 본인 회차인지 직접 확인한다
     * (응시 슬라이스의 {@code loadAttempt} 와 같은 원칙).</p>
     */
    public List<ProctorWarningRow> findPendingForTrainee(Long attemptId, Long userId) {
        ExamAttempt attempt = proctorMonitorService.requireAttempt(attemptId);
        assertOwner(attempt, userId);
        return proctorWarningRepository
                .findByAttemptIdAndAcknowledgedAtIsNullOrderBySentAtAsc(attemptId).stream()
                .map(w -> toRow(w, w.getSentBy().getName()))
                .toList();
    }

    /** 응시자가 경고를 확인함. 여기서 갱신하는 건 acknowledgedAt 뿐이다. */
    @Transactional
    public void acknowledge(Long warningId, Long userId) {
        ProctorWarning warning = proctorWarningRepository.findById(warningId)
                .orElseThrow(() -> new IllegalArgumentException("경고를 찾을 수 없습니다: " + warningId));
        assertOwner(warning.getAttempt(), userId);
        if (warning.getAcknowledgedAt() == null) {
            warning.acknowledge(LocalDateTime.now());
        }
    }

    private void assertOwner(ExamAttempt attempt, Long userId) {
        if (!attempt.getUser().getId().equals(userId)) {
            throw new ProctorAccessDeniedException("본인의 응시 회차가 아닙니다.");
        }
    }

    private String normalize(String message) {
        if (message == null || message.isBlank()) {
            return DEFAULT_MESSAGE;
        }
        String trimmed = message.strip();
        return trimmed.length() <= MESSAGE_MAX ? trimmed : trimmed.substring(0, MESSAGE_MAX);
    }

    private ProctorWarningRow toRow(ProctorWarning warning, String senderName) {
        return new ProctorWarningRow(
                warning.getId(),
                warning.getAttempt().getId(),
                warning.getMessage(),
                senderName,
                warning.getSentAt().format(ProctorLabels.DATE_TIME),
                warning.getAcknowledgedAt() == null
                        ? null : warning.getAcknowledgedAt().format(ProctorLabels.DATE_TIME),
                warning.getAcknowledgedAt() != null);
    }
}
