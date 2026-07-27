package com.ssa.lms.course.web;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.course.service.EnrollmentException;
import com.ssa.lms.course.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 훈련생 "나의 과정" — 수강 이력 조회 + 모집중 과정 신청/취소.
 * 경로 {@code /trainee/**} 는 SecurityConfig 에서 TRAINEE·ADMIN 허용(신청/취소는 본인만 서비스에서 검증).
 */
@Controller
@RequiredArgsConstructor
public class MyCourseController {

    private final EnrollmentService enrollmentService;

    @GetMapping("/trainee/my-course")
    public String myCourse(@AuthenticationPrincipal LoginUser user, Model model) {
        model.addAttribute("enrollments", enrollmentService.myEnrollments(user.getId()));
        model.addAttribute("openCourses", enrollmentService.openCoursesFor(user.getId()));
        return "trainee/my-course";
    }

    @PostMapping("/trainee/courses/{courseId}/enroll")
    public String enroll(@AuthenticationPrincipal LoginUser user,
                         @PathVariable Long courseId, RedirectAttributes ra) {
        try {
            enrollmentService.apply(user.getId(), courseId);
            ra.addFlashAttribute("enrollMessage", "수강신청이 접수되었습니다. 관리자 승인을 기다려 주세요.");
        } catch (EnrollmentException e) {
            ra.addFlashAttribute("enrollError", e.getMessage());
        }
        return "redirect:/trainee/my-course";
    }

    @PostMapping("/trainee/enrollments/{enrollmentId}/cancel")
    public String cancel(@AuthenticationPrincipal LoginUser user,
                         @PathVariable Long enrollmentId, RedirectAttributes ra) {
        try {
            enrollmentService.cancel(enrollmentId, user.getId());
        } catch (EnrollmentException e) {
            ra.addFlashAttribute("enrollError", e.getMessage());
        }
        return "redirect:/trainee/my-course";
    }
}
