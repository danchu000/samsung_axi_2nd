package com.ssa.lms.attendance.web;

import com.ssa.lms.attendance.entity.AttendanceStatus;
import com.ssa.lms.attendance.service.AttendanceService;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.Session;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.SessionRepository;
import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

/**
 * 관리자 출결 현황 — 과정 선택 → 수강생×차시 출결 매트릭스 조회, 접속 기반 재산정, 수동 보정.
 * 경로 {@code /admin/attendance/**} 는 SecurityConfig 의 {@code /admin/**}(ADMIN) 규칙으로 커버된다.
 */
@Controller
@RequestMapping("/admin/attendance")
@RequiredArgsConstructor
public class AttendanceAdminController {

    private static final String VIEW = "admin/admin-05-attendance/admin-attendance";

    private final AttendanceService attendanceService;
    private final CourseRepository courseRepository;
    private final SessionRepository sessionRepository;
    private final CourseQueryService courseQueryService;
    private final UserRepository userRepository;

    @GetMapping
    public String status(@RequestParam(required = false) Long courseId, Model model) {
        List<Course> courses = courseRepository.findAllByOrderByStartDateDesc();
        model.addAttribute("courses", courses);

        Course selected = resolveSelected(courses, courseId);
        model.addAttribute("selectedCourse", selected);
        model.addAttribute("statuses", AttendanceStatus.values());

        if (selected != null) {
            List<Session> sessions = sessionRepository
                    .findBySubjectCourseIdOrderBySubjectOrderNoAscSeqAsc(selected.getId()).stream()
                    .filter(s -> s.getLessonDate() != null)
                    .toList();
            List<User> trainees = userRepository.findAllById(
                            courseQueryService.findUserIdsByCourseId(selected.getId())).stream()
                    .sorted(Comparator.comparing(User::getName))
                    .toList();
            model.addAttribute("matrix",
                    AttendanceMatrixView.of(trainees, sessions, attendanceService.findByCourse(selected.getId())));
        }
        return VIEW;
    }

    @PostMapping("/recalculate")
    public String recalculate(@RequestParam Long courseId, RedirectAttributes ra) {
        int touched = attendanceService.recalculate(courseId);
        ra.addFlashAttribute("message", "접속 로그 기준으로 출결을 재산정했습니다. (갱신 " + touched + "건)");
        return "redirect:/admin/attendance?courseId=" + courseId;
    }

    @PostMapping("/{attendanceId}/override")
    public String override(@PathVariable Long attendanceId,
                           @RequestParam Long courseId,
                           @RequestParam AttendanceStatus status,
                           @RequestParam(required = false) String note,
                           RedirectAttributes ra) {
        attendanceService.override(attendanceId, status, note);
        ra.addFlashAttribute("message", "출결을 수동 보정했습니다.");
        return "redirect:/admin/attendance?courseId=" + courseId;
    }

    private Course resolveSelected(List<Course> courses, Long courseId) {
        if (courses.isEmpty()) {
            return null;
        }
        if (courseId == null) {
            return courses.get(0);
        }
        return courses.stream().filter(c -> c.getId().equals(courseId)).findFirst().orElse(courses.get(0));
    }
}
