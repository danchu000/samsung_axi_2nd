package com.ssa.lms.completion.web;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.completion.service.CompletionService;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.service.CourseQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

/**
 * 강사 이수 관리 — 담당 과정 수강생의 이수 현황을 <b>읽기 전용</b>으로 조회한다.
 * 이수 기준 설정/자동 판정/확정/이수증 발급은 관리자 화면 소관이다.
 *
 * <p>권한 경계: 담당하지 않는 과정 id 로 접근하면 403. 관리자 서비스({@link CompletionService})를
 * 재사용하고 담당 과정으로만 범위를 좁힌다. 경로 {@code /instructor/**} 는 SecurityConfig 규칙으로 커버된다.</p>
 */
@Controller
@RequestMapping("/instructor/graduate")
@RequiredArgsConstructor
public class InstructorCompletionController {

    private static final String VIEW = "instructor/graduate";

    private final CompletionService completionService;
    private final CourseQueryService courseQueryService;
    private final CourseRepository courseRepository;

    @GetMapping
    public String graduate(@RequestParam(required = false) Long courseId,
                           @AuthenticationPrincipal LoginUser user, Model model) {
        List<Long> myCourseIds = courseQueryService.findCourseIdsByInstructorId(user.getId());

        if (courseId != null && !myCourseIds.contains(courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 과정이 아닙니다.");
        }

        List<Course> courses = courseRepository.findAllById(myCourseIds).stream()
                .sorted(Comparator.comparing(Course::getStartDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        model.addAttribute("courses", courses);

        Course selected = resolveSelected(courses, courseId);
        model.addAttribute("selectedCourse", selected);
        if (selected != null) {
            model.addAttribute("completions", completionService.viewsByCourse(selected.getId()));
        }
        return VIEW;
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
