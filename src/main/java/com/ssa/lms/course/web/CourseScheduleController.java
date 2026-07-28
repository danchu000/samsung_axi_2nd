package com.ssa.lms.course.web;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.course.service.CourseScheduleService;
import com.ssa.lms.course.service.InstructorCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 일정 관리 — 차시 lessonDate 기반 일정 목록.
 * <ul>
 *   <li>관리자 {@code GET /admin/courses/schedule} — 전체 과정 선택 (literal 경로라
 *       {@code /admin/courses/{id}} 보다 우선 매칭된다).</li>
 *   <li>강사 {@code GET /instructor/scheduler} — 담당 과정 한정.</li>
 * </ul>
 */
@Controller
@RequiredArgsConstructor
public class CourseScheduleController {

    private final CourseScheduleService courseScheduleService;
    private final InstructorCourseService instructorCourseService;
    private final CourseQueryService courseQueryService;

    /** 관리자 일정 관리 — 전체 과정. */
    @GetMapping("/admin/courses/schedule")
    public String adminSchedule(@RequestParam(required = false) Long courseId, Model model) {
        List<CourseQueryService.CourseOption> options = courseQueryService.findAllCourseOptions();
        model.addAttribute("courseOptions", options);

        Long selected = (courseId != null) ? courseId
                : (options.isEmpty() ? null : options.get(0).id());
        model.addAttribute("selectedCourseId", selected);
        model.addAttribute("schedule",
                selected == null ? List.of() : courseScheduleService.scheduleOf(selected));
        return "admin/admin-03-courses/admin-courses-schedule";
    }

    /** 강사 일정 관리 — 담당 과정 한정 (담당 아닌 courseId → 403). */
    @GetMapping("/instructor/scheduler")
    public String instructorSchedule(@AuthenticationPrincipal LoginUser user,
                                     @RequestParam(required = false) Long courseId, Model model) {
        List<CourseQueryService.CourseOption> options = instructorCourseService.myCourseOptions(user.getId());
        model.addAttribute("courseOptions", options);

        Long selected = (courseId != null) ? courseId
                : (options.isEmpty() ? null : options.get(0).id());
        model.addAttribute("selectedCourseId", selected);
        if (selected != null) {
            instructorCourseService.requireOwnedCourse(user.getId(), selected);   // 권한 경계
            model.addAttribute("schedule", courseScheduleService.scheduleOf(selected));
        } else {
            model.addAttribute("schedule", List.of());
        }
        return "instructor/scheduler";
    }
}
