package com.ssa.lms.web.instructor.assignment;

import com.ssa.lms.assignment.dto.AssignmentSearchCond;
import com.ssa.lms.assignment.dto.CourseAssignmentRow;
import com.ssa.lms.assignment.dto.GradingForm;
import com.ssa.lms.assignment.repository.AssignmentLookupRepository;
import com.ssa.lms.assignment.service.AssignmentGradingService;
import com.ssa.lms.assignment.service.CourseAssignmentService;
import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.demo.SampleScreenData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 강사 과제 채점 화면.
 *
 * 관리자 화면과 데이터·서비스는 같고, 목록만 <b>본인이 담당하는 과정의 과제</b>로 좁힌다
 * (권한정의서의 △ = 담당 과정 한정). 관리자가 이 URL 로 들어오면 필터 없이 전체를 본다.
 *
 * <p><b>2026-08-05 수정:</b> 원래는 "본인이 <i>채점자로 지정된</i> 과제" 로 좁혔는데,
 * 과제 배정에서 채점자 지정이 선택 항목이라 채점자를 비워 둔 과제는 담당 강사에게도
 * 한 건도 보이지 않았다 (운영에서 목록이 통째로 비어 보인 원인). 관리자 화면과 같은
 * 담당 과정 기준으로 통일했다 — {@code CourseAssignmentService.searchForInstructor}.</p>
 */
@Controller
@RequestMapping("/instructor/assignments")
@RequiredArgsConstructor
public class InstructorAssignmentController {

    private static final String LIST_VIEW = "instructor/assignment";
    private static final String GRADING_VIEW = "instructor/assignment-grading";
    private static final String MODAL_VIEW = "admin/admin-04-evaluation/grading-modal";
    private static final String GRADING_URL_PREFIX = "/instructor/assignments/";

    private final CourseAssignmentService courseAssignmentService;
    private final AssignmentGradingService gradingService;
    private final AssignmentLookupRepository lookupRepository;
    private final SampleScreenData sampleScreenData;

    @GetMapping
    public String list(@RequestParam(required = false) Long courseId,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String keyword,
                       @AuthenticationPrincipal LoginUser loginUser,
                       Model model) {
        AssignmentSearchCond cond = new AssignmentSearchCond(courseId, status, null, keyword);
        boolean instructor = loginUser != null
                && loginUser.getRole() == com.ssa.lms.user.entity.Role.INSTRUCTOR;
        List<CourseAssignmentRow> rows = instructor
                ? courseAssignmentService.searchForInstructor(cond, GRADING_URL_PREFIX, loginUser.getId())
                : courseAssignmentService.search(cond, GRADING_URL_PREFIX);

        // 필터를 건 결과가 0건인 것과 배정 자체가 없는 것은 다르다.
        // 조건 없이 비었을 때만 예시로 채운다 — 검색 결과가 예시로 바뀌면 오해한다.
        boolean unfiltered = courseId == null
                && cond.derivedStatusOrNull() == null && cond.keywordOrNull() == null;
        var filled = unfiltered
                ? sampleScreenData.fill(rows, () -> sampleScreenData.courseAssignments(GRADING_URL_PREFIX))
                : new SampleScreenData.Filled<>(rows, false);

        model.addAttribute("rows", filled.rows());
        model.addAttribute("sampleData", filled.sample());
        model.addAttribute("cond", cond);
        model.addAttribute("courseOptions", lookupRepository.findCourseOptions());
        model.addAttribute("instructorOptions", lookupRepository.findInstructorOptions());
        return LIST_VIEW;
    }

    /**
     * 채점 화면.
     *
     * <p>예시 배정이면 예시 채점 데이터를 렌더한다 — 목록만 예시로 채우고 「채점하기」에서
     * 막으면 정작 채점 화면을 캡처할 수 없다. 저장은 아래 {@link #saveGrading} 이 막는다.</p>
     */
    @GetMapping("/{id}/grading")
    public String grading(@PathVariable Long id, Model model) {
        if (SampleScreenData.isSampleId(id)) {
            var summary = sampleScreenData.gradingSummary(id);
            if (summary == null) {
                // 예시 기능이 꺼져 있거나 모르는 예시 id. 500 대신 목록으로 되돌린다.
                return "redirect:/instructor/assignments";
            }
            model.addAttribute("summary", summary);
            model.addAttribute("students", sampleScreenData.gradingStudents(id, GRADING_URL_PREFIX));
            model.addAttribute("notSubmitted", sampleScreenData.gradingNotSubmitted(id));
            model.addAttribute("sampleData", true);
            return GRADING_VIEW;
        }
        model.addAttribute("summary", gradingService.loadSummary(id));
        model.addAttribute("students", gradingService.loadStudents(id, GRADING_URL_PREFIX));
        model.addAttribute("notSubmitted", courseAssignmentService.findNotSubmittedUsers(id));
        model.addAttribute("sampleData", false);
        return GRADING_VIEW;
    }

    @GetMapping("/{courseAssignmentId}/submissions/{submissionId}/grading")
    public String gradingModal(@PathVariable Long courseAssignmentId,
                               @PathVariable Long submissionId,
                               Model model) {
        if (SampleScreenData.isSampleId(courseAssignmentId)) {
            var sample = sampleScreenData.submissionDetail(courseAssignmentId, submissionId);
            if (sample == null) {
                return "redirect:/instructor/assignments";
            }
            GradingForm sampleForm = new GradingForm();
            sampleForm.setScore(sample.currentScore());
            sampleForm.setFeedback(sample.currentFeedback());
            sampleForm.setMemo(sample.currentMemo());
            model.addAttribute("detail", sample);
            model.addAttribute("form", sampleForm);
            model.addAttribute("courseAssignmentId", courseAssignmentId);
            model.addAttribute("actionPrefix", GRADING_URL_PREFIX);
            model.addAttribute("sampleData", true);
            return MODAL_VIEW;
        }
        var detail = gradingService.loadSubmissionDetail(submissionId);
        // 폼을 기존 값으로 채워둔다. th:field 가 value/텍스트를 직접 쓰기 때문에
        // 템플릿에서 th:value/th:text 로 덧씌우면 무시되거나 충돌한다.
        GradingForm form = new GradingForm();
        form.setScore(detail.currentScore());
        form.setFeedback(detail.currentFeedback());
        form.setMemo(detail.currentMemo());
        model.addAttribute("detail", detail);
        model.addAttribute("form", form);
        model.addAttribute("courseAssignmentId", courseAssignmentId);
        model.addAttribute("actionPrefix", GRADING_URL_PREFIX);
        return MODAL_VIEW;
    }

    @PostMapping("/{courseAssignmentId}/submissions/{submissionId}/grading")
    public String saveGrading(@PathVariable Long courseAssignmentId,
                              @PathVariable Long submissionId,
                              @Valid @ModelAttribute("form") GradingForm form,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal LoginUser loginUser,
                              Model model) {
        if (SampleScreenData.isSampleId(courseAssignmentId)) {
            // 예시 제출물에는 저장할 대상이 없다. 팝업을 그대로 다시 그리고 이유만 알려준다.
            var sample = sampleScreenData.submissionDetail(courseAssignmentId, submissionId);
            if (sample == null) {
                return "redirect:/instructor/assignments";
            }
            bindingResult.reject("grade.sample",
                    "화면 예시용 데이터라 저장되지 않습니다. 실제 제출물이 있으면 저장됩니다.");
            model.addAttribute("detail", sample);
            model.addAttribute("courseAssignmentId", courseAssignmentId);
            model.addAttribute("actionPrefix", GRADING_URL_PREFIX);
            model.addAttribute("sampleData", true);
            return MODAL_VIEW;
        }
        if (!bindingResult.hasErrors()) {
            try {
                gradingService.grade(submissionId, form, loginUser.getId());
                model.addAttribute("saved", true);
            } catch (IllegalArgumentException e) {
                bindingResult.reject("grade.failed", e.getMessage());
            }
        }
        model.addAttribute("detail", gradingService.loadSubmissionDetail(submissionId));
        model.addAttribute("courseAssignmentId", courseAssignmentId);
        model.addAttribute("actionPrefix", GRADING_URL_PREFIX);
        return MODAL_VIEW;
    }
}
