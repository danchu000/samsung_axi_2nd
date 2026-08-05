package com.ssa.lms.completion.web;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.completion.service.CompletionService;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.course.service.CourseQueryService.CourseOption;
import com.ssa.lms.demo.SampleInstructorData;
import com.ssa.lms.demo.SampleScreenData;
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
    private final SampleInstructorData sampleData;

    /**
     * 담당 과정의 이수 현황.
     *
     * <p>화면에 <b>엔티티 대신 조회용 {@link CourseOption} 을 넘긴다</b> — 담당 과정이 없는 강사에게
     * 예시 과정을 보여주려면 과정 하나를 만들어야 하는데, {@code Course} 는 id 가
     * {@code @GeneratedValue} 라 빌더로 채울 수 없고 영속 컨텍스트 밖 엔티티를 화면에 흘리면
     * 지연 로딩에서 터진다. 템플릿이 읽던 필드명(id/courseCode/courseName/cohort)은 그대로다.</p>
     */
    @GetMapping
    public String graduate(@RequestParam(required = false) Long courseId,
                           @AuthenticationPrincipal LoginUser user, Model model) {
        List<Long> myCourseIds = courseQueryService.findCourseIdsByInstructorId(user.getId());

        // 예시 과정 id 는 담당 과정 검사를 통과시킨다 — 실제 조회는 하지 않고 예시만 보여준다.
        boolean sampleCourse = SampleScreenData.isSampleId(courseId);
        if (courseId != null && !sampleCourse && !myCourseIds.contains(courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 과정이 아닙니다.");
        }

        List<CourseOption> courses = courseRepository.findAllById(myCourseIds).stream()
                .sorted(Comparator.comparing(Course::getStartDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(c -> new CourseOption(c.getId(), c.getCourseCode(), c.getCourseName(), c.getCohort()))
                .toList();

        // 담당 과정이 아예 없는 강사도 화면을 볼 수 있게 예시 과정을 끼워 넣는다.
        boolean noCourse = courses.isEmpty() && sampleData.isEnabled();
        if (noCourse) {
            courses = sampleData.sampleCourseOptions();
        }
        model.addAttribute("courses", courses);

        CourseOption selected = resolveSelected(courses, courseId);
        model.addAttribute("selectedCourse", selected);

        boolean sample = false;
        if (selected != null) {
            List<CompletionView> real = noCourse || sampleCourse
                    ? List.of() : completionService.viewsByCourse(selected.id());
            var filled = sampleData.fill(real, sampleData::completions);
            model.addAttribute("completions", filled.rows());
            sample = filled.sample();
        }
        model.addAttribute("sampleData", sample);
        return VIEW;
    }

    private CourseOption resolveSelected(List<CourseOption> courses, Long courseId) {
        if (courses.isEmpty()) {
            return null;
        }
        if (courseId == null) {
            return courses.get(0);
        }
        return courses.stream().filter(c -> c.id().equals(courseId)).findFirst().orElse(courses.get(0));
    }
}
