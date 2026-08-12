package com.ssa.lms.course.web;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.demo.SampleScreenData;
import com.ssa.lms.course.service.CurriculumService;
import com.ssa.lms.course.service.InstructorCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 강사 담당 과정/훈련생 (읽기 위주). 경로 {@code /instructor/**} 는 SecurityConfig 에서
 * INSTRUCTOR·ADMIN 허용 — 개별 과정 소유권은 {@link InstructorCourseService} 가 검증한다
 * (담당 아닌 과정 id 로 접근 시 403, 없는 과정은 404).
 */
@Controller
@RequiredArgsConstructor
public class InstructorCourseController {

    private final InstructorCourseService instructorCourseService;
    private final CurriculumService curriculumService;
    private final com.ssa.lms.demo.SampleInstructorData sampleData;

    /** 담당 과정 목록. */
    @GetMapping("/instructor/courses")
    public String courses(@AuthenticationPrincipal LoginUser user, Model model) {
        var filled = sampleData.fill(
                instructorCourseService.myCourses(user.getId()), sampleData::myCourses);
        model.addAttribute("courses", filled.rows());
        model.addAttribute("sampleData", filled.sample());
        return "instructor/courses";
    }

    /** 담당 과정 상세 — 과목/차시 구성 조회 (읽기 전용). */
    @GetMapping("/instructor/courses/{id}")
    public String courseDetail(@AuthenticationPrincipal LoginUser user,
                               @PathVariable Long id, Model model) {
        // 담당 과정이 없는 강사의 목록은 예시 행이다 — 예시 id 는 실제 PK 가 아니라
        // DB 조회로 가면 404 가 되므로, 목록과 같은 예시 데이터로 상세를 렌더한다.
        if (SampleScreenData.isSampleId(id)) {
            var sample = sampleData.courseDetail(id);
            if (sample != null) {
                model.addAttribute("course", sample);
                model.addAttribute("subjects", sampleData.curriculum(id));
                model.addAttribute("sampleData", true);
                return "instructor/courses-detail";
            }
        }
        Course course = instructorCourseService.requireOwnedCourse(user.getId(), id);
        model.addAttribute("course", course);
        model.addAttribute("subjects", curriculumService.curriculum(id));
        model.addAttribute("sampleData", false);
        return "instructor/courses-detail";
    }

    /** 담당 훈련생 목록 — 과정 선택 셀렉트 포함. courseId 미지정 시 첫 담당 과정. */
    @GetMapping("/instructor/trainees")
    public String trainees(@AuthenticationPrincipal LoginUser user,
                           @RequestParam(required = false) Long courseId, Model model) {
        List<CourseQueryService.CourseOption> options = instructorCourseService.myCourseOptions(user.getId());
        // 담당 과정이 없는 강사도 화면을 볼 수 있게 예시 과정을 끼워 넣는다.
        boolean noCourse = options.isEmpty() && sampleData.isEnabled();
        if (noCourse) {
            options = sampleData.sampleCourseOptions();
        }
        model.addAttribute("courseOptions", options);

        Long selected = (courseId != null) ? courseId
                : (options.isEmpty() ? null : options.get(0).id());
        model.addAttribute("selectedCourseId", selected);

        List<CourseQueryService.UserDisplay> real =
                selected == null || SampleScreenData.isSampleId(selected)
                        ? List.of() : instructorCourseService.traineesOf(user.getId(), selected);
        var filled = sampleData.fill(real, sampleData::trainees);
        model.addAttribute("trainees", selected == null ? List.of() : filled.rows());
        model.addAttribute("sampleData", selected != null && filled.sample());
        return "instructor/trainees";
    }
}
