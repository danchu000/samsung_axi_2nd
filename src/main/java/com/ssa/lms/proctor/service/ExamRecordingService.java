package com.ssa.lms.proctor.service;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.exam.entity.Exam;
import com.ssa.lms.exam.entity.ExamAttempt;
import com.ssa.lms.exam.repository.ExamRefRepository;
import com.ssa.lms.proctor.dto.RecordingRow;
import com.ssa.lms.proctor.entity.ExamEventLog;
import com.ssa.lms.proctor.entity.ExamRecording;
import com.ssa.lms.proctor.repository.ExamRecordingRepository;
import com.ssa.lms.proctor.repository.ProctorMonitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 시험 녹화 조회 · 스트리밍.
 *
 * <p><b>녹화 파일은 웹 루트에 두지 않는다.</b> {@code static/} 아래에 두면 URL 만 알면 누구나
 * 받을 수 있고, 녹화물은 개인영상정보라 그건 사고다. 파일은 별도 디렉터리에 두고
 * 인증·권한 검사를 통과한 요청에만 이 서비스가 스트림을 연다.</p>
 *
 * <p>저장 위치는 {@code lms.proctor.recording-dir} 로 바꿀 수 있다 (기본: 임시 디렉터리 하위).
 * 운영에서는 반드시 웹 루트 밖 경로로 지정할 것.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamRecordingService {

    private final ExamRecordingRepository examRecordingRepository;
    private final ProctorMonitorRepository proctorMonitorRepository;
    private final CourseQueryService courseQueryService;
    private final ExamRefRepository examRefRepository;

    @Value("${lms.proctor.recording-dir:#{systemProperties['java.io.tmpdir']}/lms-proctor-recordings}")
    private String recordingDir;

    /** 녹화 목록. 강사는 담당 과정 것만 보인다. */
    public List<RecordingRow> list(ProctorViewer viewer, String streamUrlPrefix) {
        List<ExamRecording> recordings = viewer.isAdmin()
                ? examRecordingRepository.findAllForList()
                : findForInstructor(viewer.userId());
        if (recordings.isEmpty()) {
            return List.of();
        }

        List<Long> attemptIds = recordings.stream()
                .map(r -> r.getAttempt().getId()).distinct().toList();
        Map<Long, Map<ExamEventLog.Severity, Long>> events = new HashMap<>();
        for (Object[] row : proctorMonitorRepository.countEventsByAttemptAndSeverity(attemptIds)) {
            events.computeIfAbsent((Long) row[0], k -> new HashMap<>())
                    .put((ExamEventLog.Severity) row[1], (Long) row[2]);
        }

        List<RecordingRow> rows = new ArrayList<>(recordings.size());
        for (ExamRecording recording : recordings) {
            ExamAttempt attempt = recording.getAttempt();
            Exam exam = attempt.getExam();
            Course course = exam.getCourse();
            Map<ExamEventLog.Severity, Long> counts =
                    events.getOrDefault(attempt.getId(), Map.of());

            rows.add(new RecordingRow(
                    recording.getId(),
                    attempt.getId(),
                    attempt.getUser().getId(),
                    attempt.getUser().getName(),
                    exam.getExamName(),
                    course.getCourseName(),
                    course.getCourseCode(),
                    recording.getRecordedAt().format(ProctorLabels.DATE_TIME),
                    ProctorLabels.duration(recording.getDurationSec() == null
                            ? 0 : recording.getDurationSec()),
                    ProctorLabels.fileSize(recording.getSizeBytes()),
                    recording.getStatus().name(),
                    ProctorLabels.recordingStatus(recording.getStatus()),
                    recording.getStatus() == ExamRecording.RecordingStatus.AVAILABLE,
                    countOf(counts.get(ExamEventLog.Severity.WARN)),
                    countOf(counts.get(ExamEventLog.Severity.CRITICAL)),
                    // 파일 경로는 절대 내려보내지 않는다 — 화면은 스트리밍 URL 만 안다
                    streamUrlPrefix + recording.getId()));
        }
        return rows;
    }

    /**
     * 재생용 스트림. 권한 검사를 통과해야만 파일 핸들이 열린다.
     *
     * <p>{@code filePath} 는 DB 값이지만 그대로 믿지 않는다. 정규화해서 기준 디렉터리 밖을
     * 가리키면 거부한다 — 상대 경로(`../`)가 섞여 들어오면 임의 파일 읽기가 된다.</p>
     */
    public Resource stream(Long recordingId, ProctorViewer viewer) {
        ExamRecording recording = examRecordingRepository.findForStreaming(recordingId)
                .orElseThrow(() -> new IllegalArgumentException("녹화를 찾을 수 없습니다: " + recordingId));
        assertCanView(recording, viewer);

        if (recording.getStatus() != ExamRecording.RecordingStatus.AVAILABLE) {
            throw new IllegalStateException("재생할 수 없는 녹화입니다: "
                    + ProctorLabels.recordingStatus(recording.getStatus()));
        }

        Path base = Paths.get(recordingDir).toAbsolutePath().normalize();
        Path target = base.resolve(recording.getFilePath()).normalize();
        if (!target.startsWith(base)) {
            log.warn("녹화 경로가 기준 디렉터리를 벗어났다 recordingId={}", recordingId);
            throw new ProctorAccessDeniedException("잘못된 녹화 경로입니다.");
        }
        FileSystemResource resource = new FileSystemResource(target);
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalStateException("녹화 파일이 없습니다: recordingId=" + recordingId);
        }
        return resource;
    }

    /** 감독 화면과 같은 규칙: 관리자 전체 / 강사는 담당 과정만. */
    private void assertCanView(ExamRecording recording, ProctorViewer viewer) {
        if (viewer.isAdmin()) {
            return;
        }
        Long courseId = recording.getAttempt().getExam().getCourse().getId();
        if (viewer.isInstructor() && courseQueryService.isInstructorOf(viewer.userId(), courseId)) {
            return;
        }
        throw new ProctorAccessDeniedException("담당 과정이 아닙니다.");
    }

    private List<ExamRecording> findForInstructor(Long instructorId) {
        List<Long> courseIds = examRefRepository.findAllCourses().stream()
                .map(Course::getId)
                .filter(courseId -> courseQueryService.isInstructorOf(instructorId, courseId))
                .toList();
        return courseIds.isEmpty() ? List.of() : examRecordingRepository.findByCoursesForList(courseIds);
    }

    private long countOf(Long value) {
        return value == null ? 0L : value;
    }
}
