package com.ssa.lms.dashboard.service;

import com.ssa.lms.assignment.dto.TraineeAssignmentCard;
import com.ssa.lms.assignment.service.SubmissionService;
import com.ssa.lms.attendance.service.AttendanceService;
import com.ssa.lms.content.service.ProgressQueryService;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.service.EnrollmentService;
import com.ssa.lms.course.web.MyEnrollmentView;
import com.ssa.lms.dashboard.dto.DashboardFormat;
import com.ssa.lms.dashboard.dto.TraineeDashboardView;
import com.ssa.lms.exam.dto.ExamTakeRow;
import com.ssa.lms.exam.service.ExamAttemptService;
import com.ssa.lms.grading.dto.GradeSummary;
import com.ssa.lms.grading.service.GradeQueryService;
import com.ssa.lms.notice.dto.NoticeListRow;
import com.ssa.lms.notice.dto.NotificationListRow;
import com.ssa.lms.notice.dto.NotificationSearchCond;
import com.ssa.lms.notice.service.NoticeService;
import com.ssa.lms.notice.service.NoticeVisibilityService;
import com.ssa.lms.notice.service.NotificationService;
import com.ssa.lms.survey.dto.TraineeSurveyRow;
import com.ssa.lms.survey.service.SurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 훈련생 대시보드 집계 — 전부 <b>본인(userId) 데이터만</b>.
 *
 * <p>스코프 판정을 여기서 새로 구현하지 않는다. 각 슬라이스가 이미 "본인 것만" 을 보장하는
 * 진입점을 제공한다:</p>
 * <ul>
 *   <li>수강 과정 → {@link EnrollmentService#myEnrollments}</li>
 *   <li>과제 → {@link SubmissionService#findMyAssignments}</li>
 *   <li>시험 → {@link ExamAttemptService#availableExams} (수강 중인 과정의 시험만)</li>
 *   <li>설문 → {@link SurveyService#findForTrainee}</li>
 *   <li>성적 → {@link GradeQueryService#findConfirmedGrades} (확정 성적만)</li>
 *   <li>공지 → {@link NoticeService#searchPublished} + {@link NoticeVisibilityService#traineeCourseIds}
 *       = 전체 공지 + 본인 수강 과정 공지</li>
 * </ul>
 *
 * <p><b>{@code Grade.passed} 는 확정 전 {@code null}(미정)이다.</b> null 을 "불합격"으로 찍으면
 * 채점이 안 끝난 훈련생에게 떨어졌다고 알리는 셈이라, 화면 문구는 합격/불합격/미정 세 가지로 낸다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TraineeDashboardService {

    private static final int TODO_ROWS = 6;
    private static final int NOTICE_ROWS = 5;
    /** 안 읽은 알림 카운트를 세는 범위. 전 건을 다 끌어오지 않기 위한 상한이다. */
    private static final int NOTIFICATION_SCAN = 200;

    private static final String ASSIGNMENT_URL = "/trainee/assignment";
    private static final String EXAM_URL = "/trainee/exam";
    private static final String SURVEY_URL = "/trainee/survey";
    private static final String NOTICE_URL = "/trainee/notice";
    private static final String LEARNING_URL = "/trainee/learning";

    /**
     * 마감시각 문자열 포맷 — 슬라이스마다 다르다.
     * 과제({@code SubmissionService.DEADLINE_FMT})는 {@code yyyy-MM-dd HH:mm},
     * 시험({@code ExamAttemptService.LIST_FORMAT})은 {@code yyyy.MM.dd HH:mm} 이다.
     * DTO 가 이미 문자열로 내려주는 값을 D-day 계산에만 쓰므로, 둘 다 시도하고 실패하면 D-day 를 생략한다.
     */
    private static final List<DateTimeFormatter> DUE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));

    private final EnrollmentService enrollmentService;          // A
    private final AttendanceService attendanceService;          // A
    private final ProgressQueryService progressQueryService;    // A
    private final SubmissionService submissionService;
    private final ExamAttemptService examAttemptService;
    private final SurveyService surveyService;
    private final GradeQueryService gradeQueryService;
    private final NoticeService noticeService;
    private final NoticeVisibilityService noticeVisibilityService;
    private final NotificationService notificationService;

    public TraineeDashboardView load(Long userId, String userName) {
        LocalDate today = LocalDate.now();

        List<MyEnrollmentView> enrollments = enrollmentService.myEnrollments(userId).stream()
                .filter(e -> e.status() == EnrollmentStatus.APPROVED
                        || e.status() == EnrollmentStatus.COMPLETED)
                .toList();

        List<TraineeDashboardView.Todo> todos = todos(userId, today);
        List<TraineeDashboardView.CourseCard> courses = courseCards(userId, enrollments, today);
        long unreadNotifications = unreadNotificationCount(userId);
        String recentGrade = recentGradeText(userId, enrollments);

        return new TraineeDashboardView(
                DashboardFormat.nullToDash(userName),
                DashboardFormat.today(today),
                stats(courses.size(), todos.size(), recentGrade, unreadNotifications),
                todos.stream().limit(TODO_ROWS).toList(),
                courses,
                notices(userId),
                NOTICE_URL);
    }

    /* ===================== 요약 타일 ===================== */

    private List<TraineeDashboardView.Stat> stats(int courseCount, int todoCount,
                                                  String recentGrade, long unread) {
        return List.of(
                new TraineeDashboardView.Stat("수강중 과정", courseCount + "개", "승인된 수강 과정"),
                new TraineeDashboardView.Stat("남은 과제·시험", todoCount + "건", "마감 전 미제출"),
                new TraineeDashboardView.Stat("최근 성적", recentGrade, "확정된 성적 기준"),
                new TraineeDashboardView.Stat("안 읽은 알림", unread + "건", "확인이 필요해요"));
    }

    /**
     * 최근 확정 성적. 확정된 성적이 하나도 없으면 "-".
     *
     * <p>{@code passed == null} 은 <b>미정</b>이다 — 불합격으로 바꿔 쓰지 않는다.
     * 확정 성적만 보는데도 null 이 나올 수 있어(총점만 확정하고 합격 기준이 없는 평가) 방어한다.</p>
     */
    private String recentGradeText(Long userId, List<MyEnrollmentView> enrollments) {
        GradeSummary latest = null;
        for (MyEnrollmentView e : enrollments) {
            for (GradeSummary g : gradeQueryService.findConfirmedGrades(userId, e.courseId())) {
                if (latest == null || after(g.confirmedAt(), latest.confirmedAt())) {
                    latest = g;
                }
            }
        }
        if (latest == null) {
            return DashboardFormat.NONE;
        }
        String score = latest.totalScore() == null ? DashboardFormat.NONE : latest.totalScore() + "점";
        return score + " (" + passedLabel(latest.passed()) + ")";
    }

    /** null = 미정. 절대 "불합격"으로 표시하지 않는다. */
    static String passedLabel(Boolean passed) {
        if (passed == null) {
            return "판정 미정";
        }
        return passed ? "합격" : "불합격";
    }

    private boolean after(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return false;
        }
        return b == null || a.isAfter(b);
    }

    /* ===================== 오늘 할 일 ===================== */

    private List<TraineeDashboardView.Todo> todos(Long userId, LocalDate today) {
        List<TraineeDashboardView.Todo> todos = new ArrayList<>();

        for (TraineeAssignmentCard a : submissionService.findMyAssignments(userId)) {
            // 이미 제출했고 재제출도 불가하면 할 일이 아니다.
            if ("SUBMITTED".equals(a.status()) || !a.canSubmit()) {
                continue;
            }
            LocalDateTime due = parse(a.deadline());
            todos.add(new TraineeDashboardView.Todo(
                    a.id(), "TASK", a.name(),
                    a.course() + (a.session() == null || a.session().isBlank() ? "" : " / " + a.session()),
                    a.deadline(), DashboardFormat.daysUntil(today, due), ASSIGNMENT_URL));
        }

        for (ExamTakeRow e : examAttemptService.availableExams(userId)) {
            if (!"available".equals(e.status()) && !"in_progress".equals(e.status())
                    && !"scheduled".equals(e.status())) {
                continue;
            }
            LocalDateTime end = parse(e.windowEnd());
            todos.add(new TraineeDashboardView.Todo(
                    e.id(), "EXAM", e.name(),
                    e.courseName() + " / " + DashboardFormat.nullToDash(e.sessionName()),
                    e.windowEnd(), DashboardFormat.daysUntil(today, end), EXAM_URL));
        }

        for (TraineeSurveyRow s : surveyService.findForTrainee(userId)) {
            if (!"ONGOING".equals(s.status())) {
                continue;
            }
            LocalDate end = parseIsoDate(s.endAt());
            todos.add(new TraineeDashboardView.Todo(
                    s.id(), "SURVEY", s.title(), s.course() + " / " + s.cohort(),
                    s.endAt(), DashboardFormat.daysUntil(today, end), SURVEY_URL + "/" + s.id()));
        }

        // 마감이 임박한 것부터. 마감일을 모르는 항목은 뒤로 민다.
        todos.sort(Comparator.comparing(t -> t.dday() == null ? Integer.MAX_VALUE : t.dday()));
        return todos;
    }

    /* ===================== 수강 과정 ===================== */

    private List<TraineeDashboardView.CourseCard> courseCards(Long userId,
                                                              List<MyEnrollmentView> enrollments,
                                                              LocalDate today) {
        List<TraineeDashboardView.CourseCard> cards = new ArrayList<>();
        for (MyEnrollmentView e : enrollments) {
            Integer dday = DashboardFormat.daysUntil(today, e.endDate());
            cards.add(new TraineeDashboardView.CourseCard(
                    String.valueOf(e.courseId()),
                    e.courseName(),
                    DashboardFormat.nullToDash(e.cohort()),
                    DashboardFormat.date(e.startDate()),
                    DashboardFormat.date(e.endDate()),
                    DashboardFormat.percent(progressQueryService.completedRatio(userId, e.courseId())),
                    DashboardFormat.percent(attendanceService.attendanceRate(userId, e.courseId())),
                    dday == null ? DashboardFormat.NONE : (dday >= 0 ? "D-" + dday : "종료"),
                    LEARNING_URL,
                    NOTICE_URL));
        }
        return cards;
    }

    /* ===================== 공지 (양식3: 훈련생 유의사항 등재) ===================== */

    /**
     * 초기화면 공지 미리보기.
     *
     * <p>정부 제출 문서(양식3)의 <b>"훈련생 유의사항 등재(정보제공)"</b> 요건이다. 기존 화면은
     * 하드코딩 이미지 5장이라 실제 정보 제공이 아니었다.</p>
     *
     * <p>노출 범위는 훈련생 공지 목록 화면({@code TraineeNoticeController})과 <b>같은 규칙·같은 쿼리</b>다
     * — 게시된 공지 중 "전체 공지(course=null) + 본인 수강 과정 공지"만. 정렬도 같다(고정 먼저, 최신순).
     * 규칙을 여기서 다시 짜면 두 화면이 갈려서 남의 과정 공지가 샐 수 있다.</p>
     */
    private List<TraineeDashboardView.NoticeItem> notices(Long userId) {
        List<NoticeListRow> rows = noticeService.searchPublished(
                noticeVisibilityService.traineeCourseIds(userId), null,
                PageRequest.of(0, NOTICE_ROWS,
                        Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("id")))).getContent();

        return rows.stream()
                .map(n -> new TraineeDashboardView.NoticeItem(
                        String.valueOf(n.id()),
                        n.title(),
                        DashboardFormat.nullToDash(n.category()),
                        n.date(),
                        n.pinned(),
                        n.courseName() == null ? "전체" : n.courseName(),
                        NOTICE_URL + "/" + n.id()))
                .toList();
    }

    /* ===================== 알림 ===================== */

    /**
     * 안 읽은 알림 수.
     *
     * <p>{@code NotificationService.search} 가 본인 수신 여부/읽음 여부를 붙여 주므로 그 값을 센다.
     * 전용 count 쿼리가 없어 최대 {@value #NOTIFICATION_SCAN} 건까지만 훑는다 — 알림이 그보다
     * 많아지면 {@code NotificationRecipientRepository} 에 count 조회 추가가 필요하다.</p>
     */
    private long unreadNotificationCount(Long userId) {
        List<NotificationListRow> rows = notificationService.search(
                new NotificationSearchCond(null, null, null, null, null), userId,
                PageRequest.of(0, NOTIFICATION_SCAN, Sort.by(Sort.Direction.DESC, "id"))).getContent();
        return rows.stream().filter(r -> "읽지않음".equals(r.readStatus())).count();
    }

    /* ===================== 내부 ===================== */

    private LocalDateTime parse(String text) {
        if (text == null || text.isBlank() || DashboardFormat.NONE.equals(text)) {
            return null;
        }
        for (DateTimeFormatter format : DUE_FORMATS) {
            try {
                return LocalDateTime.parse(text, format);
            } catch (RuntimeException ignored) {
                // 다음 포맷으로
            }
        }
        return null;
    }

    private LocalDate parseIsoDate(String text) {
        if (text == null || text.isBlank() || DashboardFormat.NONE.equals(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text, DashboardFormat.ISO_DATE);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
