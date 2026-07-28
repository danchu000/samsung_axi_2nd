package com.ssa.lms.notice.service;

import com.ssa.lms.assignment.entity.CourseAssignment;
import com.ssa.lms.assignment.repository.CourseAssignmentRepository;
import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.exam.entity.Exam;
import com.ssa.lms.exam.repository.ExamAttemptRepository;
import com.ssa.lms.exam.repository.ExamRepository;
import com.ssa.lms.notice.entity.Notification;
import com.ssa.lms.notice.entity.ReminderLog;
import com.ssa.lms.notice.repository.NotificationRecipientRepository;
import com.ssa.lms.notice.repository.NotificationRepository;
import com.ssa.lms.notice.repository.ReminderLogRepository;
import com.ssa.lms.notice.entity.NotificationRecipient;
import com.ssa.lms.survey.entity.Survey;
import com.ssa.lms.survey.repository.SurveyRepository;
import com.ssa.lms.survey.repository.SurveyResponseRepository;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 미제출·미응시·미응답자 독려 알림.
 *
 * <p>앨리스 항목의 "학습알림 — 24시간 전 / 1시간 전 리마인드", "미참여자 독려" 요건이다.</p>
 *
 * <p><b>메일은 아직 못 보낸다.</b> {@code spring-boot-starter-mail} 이 build.gradle 에 없고
 * 그 파일은 공동 소유라 A 합의가 필요하다. 지금은 인앱 알림({@link Notification})만 만든다.
 * 메일이 들어오면 {@link #notify} 안에서 발송 경로만 추가하면 된다.</p>
 *
 * <p><b>중복 발송 방지:</b> 스케줄러가 주기적으로 도는데 기록이 없으면 같은 사람에게
 * 같은 알림이 계속 쌓인다. {@link ReminderLog} 에 (사용자, 종류, 대상, 단계)를 남겨
 * 한 단계는 딱 한 번만 나가게 한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private static final DateTimeFormatter DUE_FORMAT =
            DateTimeFormatter.ofPattern("MM월 dd일 HH:mm");

    private final CourseAssignmentRepository courseAssignmentRepository;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final UserRepository userRepository;
    private final CourseQueryService courseQueryService;

    /**
     * 마감 임박 대상에게 리마인드를 보낸다.
     *
     * @param now   기준 시각 (테스트에서 주입할 수 있게 인자로 받는다)
     * @param stage 어느 단계인지 — 스케줄러가 24h/1h 각각 호출한다
     * @return 보낸 알림 수
     */
    @Transactional
    public int remindDue(LocalDateTime now, ReminderLog.ReminderStage stage) {
        Duration lead = switch (stage) {
            case BEFORE_24H -> Duration.ofHours(24);
            case BEFORE_1H -> Duration.ofHours(1);
            case OVERDUE -> Duration.ZERO;
        };

        // 마감이 [now+lead, now+lead+1시간) 구간에 드는 것만 — 스케줄러 주기(1시간)와 맞춘다.
        // 구간으로 잡지 않고 "남은 시간 <= lead" 로 하면 그 이후 모든 주기에서 계속 잡힌다.
        LocalDateTime from = stage == ReminderLog.ReminderStage.OVERDUE
                ? now.minusDays(3) : now.plus(lead);
        LocalDateTime to = stage == ReminderLog.ReminderStage.OVERDUE
                ? now : now.plus(lead).plusHours(1);

        int sent = 0;
        sent += remindAssignments(now, stage, from, to);
        sent += remindExams(now, stage, from, to);
        sent += remindSurveys(now, stage, from, to);
        return sent;
    }

    /* ===== 과제 미제출 ===== */

    private int remindAssignments(LocalDateTime now, ReminderLog.ReminderStage stage,
                                  LocalDateTime from, LocalDateTime to) {
        List<CourseAssignment> targets = courseAssignmentRepository.findDueBetween(from, to);
        if (targets.isEmpty()) {
            return 0;
        }
        List<Long> ids = targets.stream().map(CourseAssignment::getId).toList();
        Map<Long, Set<Long>> submitted = toPairMap(courseAssignmentRepository.findSubmittedPairs(ids));
        Map<Long, Set<Long>> alreadySent = toPairMap(
                reminderLogRepository.findSentPairs(ReminderLog.ReminderType.ASSIGNMENT, ids, stage));

        int sent = 0;
        for (CourseAssignment ca : targets) {
            Set<Long> done = submitted.getOrDefault(ca.getId(), Set.of());
            Set<Long> skip = alreadySent.getOrDefault(ca.getId(), Set.of());
            String title = ca.getAssignment().getTitle();
            for (Long userId : courseQueryService.findUserIdsByCourseId(ca.getCourse().getId())) {
                if (done.contains(userId) || skip.contains(userId)) {
                    continue;
                }
                sent += notify(userId, ReminderLog.ReminderType.ASSIGNMENT, ca.getId(), stage, now,
                        subject(stage, "과제", title),
                        body(stage, "과제", title, ca.getEndAt()));
            }
        }
        return sent;
    }

    /* ===== 시험 미응시 ===== */

    private int remindExams(LocalDateTime now, ReminderLog.ReminderStage stage,
                            LocalDateTime from, LocalDateTime to) {
        List<Exam> targets = examRepository.findWindowEndBetween(from, to);
        if (targets.isEmpty()) {
            return 0;
        }
        List<Long> ids = targets.stream().map(Exam::getId).toList();
        Map<Long, Set<Long>> attempted = toPairMap(examAttemptRepository.findAttemptedPairs(ids));
        Map<Long, Set<Long>> alreadySent = toPairMap(
                reminderLogRepository.findSentPairs(ReminderLog.ReminderType.EXAM, ids, stage));

        int sent = 0;
        for (Exam exam : targets) {
            if (exam.getCourse() == null) {
                continue;
            }
            Set<Long> done = attempted.getOrDefault(exam.getId(), Set.of());
            Set<Long> skip = alreadySent.getOrDefault(exam.getId(), Set.of());
            for (Long userId : courseQueryService.findUserIdsByCourseId(exam.getCourse().getId())) {
                if (done.contains(userId) || skip.contains(userId)) {
                    continue;
                }
                sent += notify(userId, ReminderLog.ReminderType.EXAM, exam.getId(), stage, now,
                        subject(stage, "시험", exam.getExamName()),
                        body(stage, "시험", exam.getExamName(), exam.getWindowEnd()));
            }
        }
        return sent;
    }

    /* ===== 설문 미응답 ===== */

    private int remindSurveys(LocalDateTime now, ReminderLog.ReminderStage stage,
                              LocalDateTime from, LocalDateTime to) {
        List<Survey> targets = surveyRepository.findEndingBetween(from, to);
        if (targets.isEmpty()) {
            return 0;
        }
        List<Long> ids = targets.stream().map(Survey::getId).toList();
        Map<Long, Set<Long>> responded = toPairMap(surveyResponseRepository.findRespondedPairs(ids));
        Map<Long, Set<Long>> alreadySent = toPairMap(
                reminderLogRepository.findSentPairs(ReminderLog.ReminderType.SURVEY, ids, stage));

        int sent = 0;
        for (Survey survey : targets) {
            // 익명 설문은 누가 응답했는지 알 수 없어 미응답자를 특정할 수 없다.
            if (survey.isAnonymous() || survey.getCourse() == null) {
                continue;
            }
            Set<Long> done = responded.getOrDefault(survey.getId(), Set.of());
            Set<Long> skip = alreadySent.getOrDefault(survey.getId(), Set.of());
            for (Long userId : courseQueryService.findUserIdsByCourseId(survey.getCourse().getId())) {
                if (done.contains(userId) || skip.contains(userId)) {
                    continue;
                }
                sent += notify(userId, ReminderLog.ReminderType.SURVEY, survey.getId(), stage, now,
                        subject(stage, "설문", survey.getTitle()),
                        body(stage, "설문", survey.getTitle(), survey.getEndAt()));
            }
        }
        return sent;
    }

    /* ===== 공통 ===== */

    /**
     * 알림 1건 생성 + 발송 기록.
     *
     * <p>메일 발송이 필요해지면 여기에 경로를 추가하면 된다 — 대상·본문이 이미 확정된 지점이다.</p>
     */
    private int notify(Long userId, ReminderLog.ReminderType type, Long targetRefId,
                       ReminderLog.ReminderStage stage, LocalDateTime now,
                       String title, String content) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return 0;
        }
        Notification notification = notificationRepository.save(Notification.builder()
                .title(title)
                .content(content)
                .priority(stage == ReminderLog.ReminderStage.BEFORE_1H
                        ? Notification.Priority.URGENT : Notification.Priority.HIGH)
                .targetType(Notification.TargetType.USER)
                .targetRefId(userId)
                .sendAt(now)
                .sender(user.get())   // 시스템 발송이지만 sender 가 필수라 수신자 본인으로 둔다
                .status(Notification.NotificationStatus.SENT)
                .build());

        recipientRepository.save(NotificationRecipient.builder()
                .notification(notification).user(user.get()).build());

        reminderLogRepository.save(ReminderLog.builder()
                .user(user.get()).reminderType(type).targetRefId(targetRefId)
                .stage(stage).sentAt(now).build());
        return 1;
    }

    private String subject(ReminderLog.ReminderStage stage, String kind, String name) {
        return switch (stage) {
            case BEFORE_24H -> "[마감 24시간 전] " + kind + " · " + name;
            case BEFORE_1H -> "[마감 임박] " + kind + " · " + name;
            case OVERDUE -> "[미제출 안내] " + kind + " · " + name;
        };
    }

    private String body(ReminderLog.ReminderStage stage, String kind, String name, LocalDateTime due) {
        String dueText = due == null ? "-" : due.format(DUE_FORMAT);
        return switch (stage) {
            case BEFORE_24H -> name + " " + kind + "의 마감이 하루 남았습니다. (마감 " + dueText + ")";
            case BEFORE_1H -> name + " " + kind + "의 마감이 1시간 남았습니다. (마감 " + dueText + ")";
            case OVERDUE -> name + " " + kind + "을(를) 아직 제출하지 않았습니다. (마감 " + dueText + ")";
        };
    }

    /** [키, 사용자id] 쌍 목록을 키별 사용자 집합으로 모은다. */
    private Map<Long, Set<Long>> toPairMap(List<Object[]> rows) {
        Map<Long, Set<Long>> map = new HashMap<>();
        for (Object[] row : rows) {
            map.computeIfAbsent(((Number) row[0]).longValue(), k -> new HashSet<>())
                    .add(((Number) row[1]).longValue());
        }
        return map;
    }
}
