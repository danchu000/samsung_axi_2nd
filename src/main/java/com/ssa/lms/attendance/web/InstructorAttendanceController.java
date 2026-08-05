package com.ssa.lms.attendance.web;

import com.ssa.lms.attendance.service.AttendanceService;
import com.ssa.lms.auth.LoginUser;
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
 * 강사 출결현황 — 담당 과정에 한해 수강생×차시 출결 매트릭스를 <b>읽기 전용</b>으로 조회한다.
 * 출결 정정(수동 보정)은 관리자 화면에서만 가능하며 이 화면은 안내만 한다.
 *
 * <p>권한 경계: 담당하지 않는 과정 id 로 접근하면 403. 관리자 서비스({@link AttendanceService})를
 * 재사용하고 {@link CourseQueryService#findCourseIdsByInstructorId}/{@code isInstructorOf} 로 범위만 좁힌다.
 * 경로 {@code /instructor/**} 는 SecurityConfig 의 (INSTRUCTOR,ADMIN) 규칙으로 커버된다.</p>
 */
@Controller
@RequestMapping("/instructor/attendance")
@RequiredArgsConstructor
public class InstructorAttendanceController {

    private static final String VIEW = "instructor/attendance";

    private final AttendanceService attendanceService;
    private final CourseQueryService courseQueryService;
    private final CourseRepository courseRepository;
    private final SampleInstructorData sampleData;

    @GetMapping
    public String status(@RequestParam(required = false) Long courseId,
                         @AuthenticationPrincipal LoginUser user, Model model) {
        List<Long> myCourseIds = courseQueryService.findCourseIdsByInstructorId(user.getId());

        // 예시 과정 id 는 통과시킨다 — 실제 조회 없이 예시 매트릭스만 보여준다.
        boolean sampleCourse = SampleScreenData.isSampleId(courseId);
        // URL 조작으로 남의 담당 과정 접근 차단
        if (courseId != null && !sampleCourse && !myCourseIds.contains(courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 과정이 아닙니다.");
        }

        List<CourseOption> courses = courseRepository.findAllById(myCourseIds).stream()
                .sorted(Comparator.comparing(Course::getStartDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(c -> new CourseOption(c.getId(), c.getCourseCode(), c.getCourseName(), c.getCohort()))
                .toList();

        boolean noCourse = courses.isEmpty() && sampleData.isEnabled();
        if (noCourse) {
            courses = sampleData.sampleCourseOptions();
        }
        model.addAttribute("courses", courses);

        CourseOption selected = resolveSelected(courses, courseId);
        model.addAttribute("selectedCourse", selected);

        boolean sample = false;
        if (selected != null) {
            AttendanceMatrixView matrix = noCourse || sampleCourse
                    ? null : attendanceService.matrixOf(selected.id());
            if ((matrix == null || matrix.isEmpty()) && sampleData.isEnabled()) {
                matrix = sampleData.attendanceMatrix();
                sample = true;
            }
            model.addAttribute("matrix", matrix);
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
