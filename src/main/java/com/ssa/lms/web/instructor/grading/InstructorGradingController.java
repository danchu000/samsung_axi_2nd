package com.ssa.lms.web.instructor.grading;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.demo.SampleInstructorData;
import com.ssa.lms.demo.SampleScreenData;
import com.ssa.lms.export.ExcelDownload;
import com.ssa.lms.grading.dto.ManualScoreRequest;
import com.ssa.lms.grading.service.ExamGradingService;
import com.ssa.lms.grading.service.GradingException;
import com.ssa.lms.web.admin.grading.AdminGradingController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 강사 시험 채점 화면.
 *
 * <p>관리자 화면과 <b>로직은 같고 템플릿만 다르다</b> (강사 레이아웃/CSS 가 별도라
 * templates/instructor/* 를 그대로 전환해 쓴다). 채점 규칙·권한 판정은 전부
 * {@link ExamGradingService} 한 곳에 있고, 여기서는 admin=false 로만 호출한다 —
 * 강사 경로에서 관리자 권한이 새어 들어가지 않도록 하드코딩한다.</p>
 *
 * <p>접근 권한: SecurityConfig 의 {@code /instructor/**} → INSTRUCTOR, ADMIN.</p>
 */
@Slf4j
@Controller
@RequestMapping(InstructorGradingController.BASE)
@RequiredArgsConstructor
public class InstructorGradingController {

    public static final String BASE = "/instructor/grading";

    private static final String LIST_VIEW = "instructor/result";
    private static final String EXAM_VIEW = "instructor/result-grading";
    private static final String MODAL_VIEW = "instructor/grading-modal-result";

    private final ExamGradingService examGradingService;
    private final SampleInstructorData sampleData;

    @GetMapping
    public String list(@RequestParam(required = false) Long courseId,
                       @RequestParam(required = false) String keyword,
                       @AuthenticationPrincipal LoginUser loginUser,
                       Model model) {
        var rows = examGradingService.examList(
                courseId, null, keyword, loginUser.getId(), admin(loginUser), BASE);
        // 검색 결과가 0건인 것과 배정 자체가 없는 것은 다르다 — 조건 없이 비었을 때만 예시로 채운다.
        boolean unfiltered = courseId == null && (keyword == null || keyword.isBlank());
        var filled = unfiltered
                ? sampleData.fill(rows, () -> sampleData.examList(BASE))
                : new SampleScreenData.Filled<>(rows, false);
        model.addAttribute("rows", filled.rows());
        model.addAttribute("sampleData", filled.sample());
        return LIST_VIEW;
    }

    @GetMapping("/exams/{examId}")
    public String exam(@PathVariable Long examId,
                       @AuthenticationPrincipal LoginUser loginUser,
                       Model model) {
        if (SampleScreenData.isSampleId(examId)) {
            var summary = sampleData.examSummary(examId, BASE);
            if (summary == null) {
                return "redirect:" + BASE;
            }
            model.addAttribute("summary", summary);
            model.addAttribute("rows", sampleData.attemptList(examId, BASE));
            model.addAttribute("sampleData", true);
            return EXAM_VIEW;
        }
        model.addAttribute("summary",
                examGradingService.summary(examId, loginUser.getId(), admin(loginUser), BASE));
        model.addAttribute("rows",
                examGradingService.attemptList(examId, loginUser.getId(), admin(loginUser), BASE));
        model.addAttribute("sampleData", false);
        return EXAM_VIEW;
    }

    @GetMapping("/attempts/{attemptId}")
    public String attempt(@PathVariable Long attemptId,
                          @AuthenticationPrincipal LoginUser loginUser,
                          Model model) {
        if (SampleScreenData.isSampleId(attemptId)) {
            var detail = sampleData.attemptDetail(attemptId, BASE);
            if (detail == null) {
                return "redirect:" + BASE;
            }
            model.addAttribute("detail", detail);
            model.addAttribute("sampleData", true);
            return MODAL_VIEW;
        }
        model.addAttribute("detail", examGradingService.attemptDetail(
                attemptId, loginUser.getId(), admin(loginUser), BASE));
        model.addAttribute("sampleData", false);
        return MODAL_VIEW;
    }

    @PostMapping("/attempts/{attemptId}/scores")
    @ResponseBody
    public ResponseEntity<?> saveScores(@PathVariable Long attemptId,
                                        @RequestBody ManualScoreRequest request,
                                        @AuthenticationPrincipal LoginUser loginUser) {
        if (SampleScreenData.isSampleId(attemptId)) {
            return sampleNotSaved();
        }
        return AdminGradingController.handle(() -> examGradingService.saveScores(
                attemptId, request, loginUser.getId(), admin(loginUser), BASE));
    }

    @PostMapping("/attempts/{attemptId}/confirm")
    @ResponseBody
    public ResponseEntity<?> confirm(@PathVariable Long attemptId,
                                     @AuthenticationPrincipal LoginUser loginUser) {
        if (SampleScreenData.isSampleId(attemptId)) {
            return sampleNotSaved();
        }
        return AdminGradingController.handle(() -> examGradingService.confirm(
                attemptId, loginUser.getId(), admin(loginUser), BASE));
    }

    /**
     * 예시 응시에는 저장할 대상이 없다. 화면이 이미 규칙 위반(400)을 안내로 띄우므로
     * 같은 형식으로 돌려준다 — 별도 분기를 화면에 추가하지 않기 위함이다.
     */
    private static ResponseEntity<?> sampleNotSaved() {
        return AdminGradingController.handle(() -> {
            throw new GradingException("SAMPLE_DATA",
                    "화면 예시용 데이터라 저장되지 않습니다. 실제 응시 기록이 있으면 저장됩니다.");
        });
    }

    /** 성적 목록 다운로드 — 기본 형식 xlsx. 담당하지 않는 과정이면 서비스가 403 으로 막는다. */
    @GetMapping("/exams/{examId}/grades.xlsx")
    @ResponseBody
    public ResponseEntity<byte[]> gradesExcel(@PathVariable Long examId,
                                              @AuthenticationPrincipal LoginUser loginUser) {
        if (SampleScreenData.isSampleId(examId)) {
            return sampleNoFile();
        }
        byte[] body = examGradingService.gradeExcel(examId, loginUser.getId(), admin(loginUser));
        return ExcelDownload.attachment("성적_" + examGradingService.examName(examId), body);
    }

    /** 대체 형식 CSV. */
    @GetMapping("/exams/{examId}/grades.csv")
    @ResponseBody
    public ResponseEntity<byte[]> gradesCsv(@PathVariable Long examId,
                                            @AuthenticationPrincipal LoginUser loginUser) {
        if (SampleScreenData.isSampleId(examId)) {
            return sampleNoFile();
        }
        String csv = examGradingService.gradeCsv(examId, loginUser.getId(), admin(loginUser));
        String name = "성적_" + examGradingService.examName(examId) + ".csv";
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"grades.csv\"; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    /** 예시 시험에는 내려줄 성적 원본이 없다. 빈 파일을 주는 대신 이유를 알려준다. */
    private static ResponseEntity<byte[]> sampleNoFile() {
        return ResponseEntity.badRequest()
                .contentType(MediaType.parseMediaType("text/plain; charset=UTF-8"))
                .body("화면 예시용 시험이라 성적 파일을 만들 수 없습니다."
                        .getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 관리자가 강사 화면으로 들어온 경우에도 관리자 권한을 그대로 인정한다.
     * (SecurityConfig 가 /instructor/** 에 ADMIN 을 허용하고 있어 실제로 들어올 수 있다.)
     */
    private boolean admin(LoginUser loginUser) {
        return AdminGradingController.isAdmin(loginUser);
    }
}
