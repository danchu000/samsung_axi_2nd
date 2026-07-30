package com.ssa.lms.course.web;

import com.ssa.lms.course.service.EnrollmentException;
import com.ssa.lms.course.service.EnrollmentService;
import com.ssa.lms.course.service.screening.MockApplicant;
import com.ssa.lms.course.service.screening.MockApplicantCatalog;
import com.ssa.lms.course.service.screening.SuitabilityEvaluator;
import com.ssa.lms.course.service.screening.SuitabilityGrade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * 관리자 통합 수강신청 승인 — 전체 과정의 승인 대기(APPLIED) 신청을 한 화면에서 승인/반려한다.
 * 과정 상세의 개별 승인({@link EnrollmentAdminController})과 같은 서비스를 쓰고, 처리 후 이 목록으로 돌아온다.
 *
 * <p>화면 상단의 <b>적합도 판정</b> 영역은 정부 제안 자료용 시범 구성이다. 선발 편람 배점
 * (서류 40 + 면접 60)에 따른 산출 로직({@link SuitabilityEvaluator})은 실제로 동작하고,
 * 신청자 정보만 예시 데이터({@link MockApplicantCatalog})를 쓴다 — DB 에는 저장되지 않는다.
 * 실제 승인 대기 건은 아래 별도 영역에서 종전대로 승인/반려한다.</p>
 */
@Controller
@RequestMapping("/admin/enrollments")
@RequiredArgsConstructor
public class EnrollmentApprovalController {

    private final EnrollmentService enrollmentService;
    private final SuitabilityEvaluator suitabilityEvaluator;
    private final MockApplicantCatalog mockApplicantCatalog;

    /** 승인 대기 목록 + 적합도 판정 시범 영역. */
    @GetMapping("/pending")
    public String pending(Model model) {
        model.addAttribute("pendingEnrollments", enrollmentService.pendingEnrollments());

        List<MockApplicant> mocks = mockApplicantCatalog.applicants();
        List<ApplicantScreeningView> applicants = new ArrayList<>(mocks.size());
        for (int i = 0; i < mocks.size(); i++) {
            MockApplicant m = mocks.get(i);
            applicants.add(ApplicantScreeningView.of(i, m, suitabilityEvaluator.evaluate(m.input())));
        }
        model.addAttribute("applicants", applicants);
        model.addAttribute("gradeSummary", summarize(applicants));
        return "admin/admin-03-courses/enrollment-approval";
    }

    /** 등급별 인원 요약 — 선언 순서(아주 적합 → 부적합) 그대로 카드로 뿌린다. */
    private List<GradeSummaryView> summarize(List<ApplicantScreeningView> applicants) {
        List<GradeSummaryView> summary = new ArrayList<>();
        for (SuitabilityGrade grade : SuitabilityGrade.values()) {
            long count = applicants.stream().filter(a -> a.result().grade() == grade).count();
            summary.add(GradeSummaryView.of(grade, count));
        }
        return summary;
    }

    @PostMapping("/{enrollmentId}/approve")
    public String approve(@PathVariable Long enrollmentId, RedirectAttributes ra) {
        try {
            enrollmentService.approve(enrollmentId);
            ra.addFlashAttribute("message", "수강신청을 승인했습니다.");
        } catch (EnrollmentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/enrollments/pending";
    }

    @PostMapping("/{enrollmentId}/reject")
    public String reject(@PathVariable Long enrollmentId, RedirectAttributes ra) {
        try {
            enrollmentService.reject(enrollmentId);
            ra.addFlashAttribute("message", "수강신청을 반려했습니다.");
        } catch (EnrollmentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/enrollments/pending";
    }
}
