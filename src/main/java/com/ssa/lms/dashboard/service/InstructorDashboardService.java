package com.ssa.lms.dashboard.service;

import com.ssa.lms.assignment.dto.AssignmentSearchCond;
import com.ssa.lms.assignment.dto.CourseAssignmentRow;
import com.ssa.lms.assignment.service.CourseAssignmentService;
import com.ssa.lms.attendance.service.AttendanceService;
import com.ssa.lms.completion.service.CompletionService;
import com.ssa.lms.completion.web.CompletionView;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.course.service.CourseService;
import com.ssa.lms.dashboard.dto.DashboardFormat;
import com.ssa.lms.dashboard.dto.InstructorDashboardView;
import com.ssa.lms.grading.dto.ExamGradingRow;
import com.ssa.lms.grading.service.ExamGradingService;
import com.ssa.lms.notice.dto.NoticeListRow;
import com.ssa.lms.notice.service.NoticeService;
import com.ssa.lms.notice.service.NoticeVisibilityService;
import com.ssa.lms.proctor.dto.MonitoringRow;
import com.ssa.lms.proctor.service.ProctorMonitorService;
import com.ssa.lms.proctor.service.ProctorViewer;
import com.ssa.lms.support.dto.QnaListRow;
import com.ssa.lms.support.dto.QnaSearchCond;
import com.ssa.lms.support.dto.TutoringRoomListRow;
import com.ssa.lms.support.service.QnaService;
import com.ssa.lms.support.service.TutoringService;
import com.ssa.lms.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 강사 대시보드 집계.
 *
 * <p><b>담당 과정 한정이 이 서비스의 핵심 제약이다</b> (권한정의서 △). 담당하지 않는 과정의
 * 시험/과제/응시자/문의가 한 건이라도 섞이면 권한 사고다. 그래서 스코프를 이 클래스에서
 * 직접 재구현하지 않고, 이미 스코프 판정이 들어 있는 서비스를 호출한다:</p>
 * <ul>
 *   <li>{@link ExamGradingService#examList} — {@code admin=false} 로 부르면 담당 과정만 돌려준다</li>
 *   <li>{@link ProctorMonitorService#monitoringList} — {@code ProctorViewer} 가 INSTRUCTOR 면 담당 과정만</li>
 *   <li>{@link CourseAssignmentService#search} — {@code cond.withGrader(id)} 로 본인이 채점자인 과제만</li>
 *   <li>과정 목록 / Q&A — {@link CourseQueryService#isInstructorOf} 로 직접 거른다</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstructorDashboardService {

    private static final int LIST_ROWS = 5;
    private static final int NOTICE_ROWS = 5;

    private static final String GRADING_PREFIX = "/instructor/grading";
    private static final String ASSIGNMENT_PREFIX = "/instructor/assignments/";
    private static final String MONITOR_PREFIX = "/instructor/proctor/exams/";
    private static final String RECORDINGS_URL = "/instructor/proctor/recordings";
    private static final String NOTICE_PREFIX = "/instructor/notice/";
    private static final String TUTORING_URL = "/admin/support/tutoring";
    private static final String QNA_PREFIX = "/admin/support/qna/";

    private final CourseService courseService;                // A
    private final CourseQueryService courseQueryService;      // A
    private final AttendanceService attendanceService;        // A
    private final CompletionService completionService;        // A
    private final ExamGradingService examGradingService;
    private final CourseAssignmentService courseAssignmentService;
    private final ProctorMonitorService proctorMonitorService;
    private final ProctorAnomalyCounter anomalyCounter;
    private final QnaService qnaService;
    private final TutoringService tutoringService;
    private final NoticeService noticeService;
    private final NoticeVisibilityService noticeVisibilityService;

    public InstructorDashboardView load(Long instructorId, String instructorName) {
        List<Course> courses = myCourses(instructorId);

        List<ExamGradingRow> examRows =
                examGradingService.examList(null, null, null, instructorId, false, GRADING_PREFIX);
        List<CourseAssignmentRow> assignmentRows = courseAssignmentService.search(
                AssignmentSearchCond.empty().withGrader(instructorId), ASSIGNMENT_PREFIX);
        List<MonitoringRow> monitoringRows = proctorMonitorService.monitoringList(
                new ProctorViewer(instructorId, Role.INSTRUCTOR), MONITOR_PREFIX);
        List<InstructorDashboardView.SupportItem> supports = supports(courses, instructorId);

        long ungradedExams = examRows.stream().filter(r -> r.ungraded() > 0).count();
        long pendingAssignments = assignmentRows.stream().filter(a -> "pending".equals(a.status())).count();
        long liveExams = monitoringRows.stream().filter(r -> r.inProgress() > 0).count();

        return new InstructorDashboardView(
                DashboardFormat.nullToDash(instructorName),
                DashboardFormat.today(LocalDate.now()),
                new InstructorDashboardView.Kpi(
                        String.valueOf(pendingAssignments),
                        String.valueOf(ungradedExams),
                        String.valueOf(supports.size()),
                        String.valueOf(liveExams)),
                courseCards(courses),
                todayEvals(examRows, assignmentRows),
                supports.stream().limit(LIST_ROWS).toList(),
                proctors(monitoringRows),
                notices(instructorId));
    }

    /* ===================== 담당 과정 ===================== */

    private List<Course> myCourses(Long instructorId) {
        return courseService.findAll().stream()
                .filter(c -> courseQueryService.isInstructorOf(instructorId, c.getId()))
                .toList();
    }

    private List<InstructorDashboardView.CourseCard> courseCards(List<Course> courses) {
        List<InstructorDashboardView.CourseCard> cards = new ArrayList<>();
        for (Course c : courses) {
            List<CompletionView> completions = completionService.viewsByCourse(c.getId());
            long notPassed = completions.stream().filter(v -> !"PASS".equals(v.result())).count();

            cards.add(new InstructorDashboardView.CourseCard(
                    c.getCourseCode(),
                    c.getCourseName(),
                    c.getStatus().getLabel(),
                    "수강생 " + courseQueryService.findUserIdsByCourseId(c.getId()).size() + "명",
                    "평균 출결률 " + averageAttendanceRate(c.getId()),
                    completions.isEmpty()
                            ? "이수 판정 이력 없음"
                            : "미이수 " + notPassed + "명 / " + completions.size() + "명",
                    "/instructor/attendance?courseId=" + c.getId(),
                    "/instructor/graduate?courseId=" + c.getId(),
                    "/instructor/courses/" + c.getId()));
        }
        return cards;
    }

    /** 출결 기록이 없으면 "-". 0% 로 찍으면 전원 결석으로 읽힌다. */
    private String averageAttendanceRate(Long courseId) {
        Map<Long, Integer> rates = attendanceService.attendanceRates(courseId);
        if (rates.isEmpty()) {
            return DashboardFormat.NONE;
        }
        return DashboardFormat.percent(
                (int) Math.round(rates.values().stream().mapToInt(Integer::intValue).average().orElse(0)));
    }

    /* ===================== 오늘의 평가 업무 ===================== */

    private List<InstructorDashboardView.EvalItem> todayEvals(List<ExamGradingRow> examRows,
                                                              List<CourseAssignmentRow> assignmentRows) {
        List<InstructorDashboardView.EvalItem> items = new ArrayList<>();
        for (ExamGradingRow r : examRows) {
            if (r.ungraded() <= 0) {
                continue;
            }
            items.add(new InstructorDashboardView.EvalItem(
                    "시험", r.title(),
                    "미채점 " + r.ungraded() + "명 · 종료 " + r.endDate(),
                    "채점하기", r.address()));
        }
        for (CourseAssignmentRow a : assignmentRows) {
            if (!"pending".equals(a.status())) {
                continue;
            }
            items.add(new InstructorDashboardView.EvalItem(
                    "과제", a.title(),
                    "제출 " + a.submitted() + "명 · 마감 " + a.endDate(),
                    "채점하기", a.address()));
        }
        return items.stream().limit(LIST_ROWS).toList();
    }

    /* ===================== 응답 현황 ===================== */

    /**
     * 미응답 문의 = 담당 과정의 Q&A(미답변/진행중) + 본인이 담당인 튜터링 방(대기/진행중).
     *
     * <p>Q&A 는 과정 단위로 걸러야 해서 담당 과정 수만큼 조회한다. 강사 한 명의 담당 과정은
     * 보통 한 자릿수라 허용 범위다 — 과정이 많아지면 {@code QnaSearchCond} 에 courseIds(복수)
     * 조건 추가를 검토할 것.</p>
     */
    private List<InstructorDashboardView.SupportItem> supports(List<Course> courses, Long instructorId) {
        List<InstructorDashboardView.SupportItem> items = new ArrayList<>();

        for (Course c : courses) {
            List<QnaListRow> rows = qnaService.searchAll(
                    new QnaSearchCond(null, null, null, c.getId(), null, null));
            for (QnaListRow q : rows) {
                if (!"pending".equals(q.statusCode()) && !"progress".equals(q.statusCode())) {
                    continue;
                }
                items.add(new InstructorDashboardView.SupportItem(
                        "QnA", q.title(),
                        q.statusLabel() + " · " + q.elapsed() + " · " + q.authorName(),
                        QNA_PREFIX + q.id()));
            }
        }

        for (TutoringRoomListRow r : tutoringService.findByInstructor(instructorId)) {
            if (!"대기".equals(r.statusLabel()) && !"진행중".equals(r.statusLabel())) {
                continue;
            }
            items.add(new InstructorDashboardView.SupportItem(
                    "튜터링", r.title(),
                    r.statusLabel() + " · 메시지 " + r.messageCount() + "건 · " + r.traineeName(),
                    TUTORING_URL));
        }
        return items;
    }

    /* ===================== 시험 감독 ===================== */

    private List<InstructorDashboardView.ProctorItem> proctors(List<MonitoringRow> monitoringRows) {
        List<MonitoringRow> live = monitoringRows.stream()
                .filter(r -> r.inProgress() > 0)
                .limit(LIST_ROWS)
                .toList();
        Map<Long, Long> anomalies = anomalyCounter.countByExam(live.stream().map(MonitoringRow::examId).toList());

        return live.stream()
                .map(r -> new InstructorDashboardView.ProctorItem(
                        r.courseName() + " - " + r.testName(),
                        "현재 응시자 " + r.inProgress() + "명 · 완료 " + r.completed()
                                + "명 · 이상행위 " + anomalies.getOrDefault(r.examId(), 0L) + "건",
                        r.detailUrl(),
                        RECORDINGS_URL))
                .toList();
    }

    /* ===================== 공지 ===================== */

    private List<InstructorDashboardView.NoticeItem> notices(Long instructorId) {
        List<NoticeListRow> rows = noticeService.searchPublished(
                noticeVisibilityService.instructorCourseIds(instructorId), null,
                PageRequest.of(0, NOTICE_ROWS,
                        Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("id")))).getContent();
        return rows.stream()
                .sorted(Comparator.comparing(NoticeListRow::pinned).reversed())
                .map(n -> new InstructorDashboardView.NoticeItem(
                        (n.pinned() ? "[고정] " : "") + n.title(), NOTICE_PREFIX + n.id()))
                .toList();
    }
}
