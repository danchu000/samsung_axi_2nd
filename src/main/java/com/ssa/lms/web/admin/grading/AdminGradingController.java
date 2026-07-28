package com.ssa.lms.web.admin.grading;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.export.ExcelDownload;
import com.ssa.lms.grading.dto.AttemptGradingDetail;
import com.ssa.lms.grading.dto.ManualScoreRequest;
import com.ssa.lms.grading.service.ExamGradingService;
import com.ssa.lms.grading.service.GradingException;
import com.ssa.lms.user.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 관리자/강사 시험 채점 화면.
 *
 * <p>접근 권한: SecurityConfig 의 {@code /admin/evaluation/**} → ADMIN, INSTRUCTOR.
 * 그 위에 서비스가 "강사는 담당 과정만"을 한 번 더 본다 (권한정의서 △ 규칙). URL 의 examId 만
 * 바꿔서 남의 과정 성적을 열 수 없어야 하기 때문이다.</p>
 *
 * <p>화면은 기존 정적 화면을 전환해서 쓴다 (새로 만들지 않는다 — CLAUDE.md).</p>
 * <ul>
 *   <li>{@code admin-04-evaluation/admin-evaluation-result.html} : 시험 채점 목록</li>
 *   <li>{@code admin-04-evaluation/result-grading.html} : 응시자별 채점 현황</li>
 *   <li>{@code admin-04-evaluation/grading-modal-result.html} : 채점 팝업 (window.open 으로 열리는 독립 페이지)</li>
 *   <li>{@code admin-04-evaluation/admin-evaluation-test-grading.html} : 성적 목록 + 변경 이력 + CSV</li>
 * </ul>
 */
@Slf4j
@Controller
@RequestMapping(AdminGradingController.BASE)
@RequiredArgsConstructor
public class AdminGradingController {

    public static final String BASE = "/admin/evaluation/grading";

    private static final String LIST_VIEW = "admin/admin-04-evaluation/admin-evaluation-result";
    private static final String EXAM_VIEW = "admin/admin-04-evaluation/result-grading";
    private static final String MODAL_VIEW = "admin/admin-04-evaluation/grading-modal-result";
    private static final String GRADES_VIEW = "admin/admin-04-evaluation/admin-evaluation-test-grading";

    private final ExamGradingService examGradingService;

    /* ===================== 화면 ===================== */

    /** 시험 채점 목록. 필터/페이징은 기존 화면 JS(assignments.js)가 그대로 처리한다. */
    @GetMapping
    public String list(@RequestParam(required = false) Long courseId,
                       @RequestParam(required = false) String instructor,
                       @RequestParam(required = false) String keyword,
                       @AuthenticationPrincipal LoginUser loginUser,
                       Model model) {
        model.addAttribute("rows", examGradingService.examList(
                courseId, instructor, keyword, loginUser.getId(), isAdmin(loginUser), BASE));
        return LIST_VIEW;
    }

    /** 시험별 채점 현황 카드 + 응시자 목록. */
    @GetMapping("/exams/{examId}")
    public String exam(@PathVariable Long examId,
                       @AuthenticationPrincipal LoginUser loginUser,
                       Model model) {
        boolean admin = isAdmin(loginUser);
        model.addAttribute("summary",
                examGradingService.summary(examId, loginUser.getId(), admin, BASE));
        model.addAttribute("rows",
                examGradingService.attemptList(examId, loginUser.getId(), admin, BASE));
        return EXAM_VIEW;
    }

    /** 채점 팝업 (window.open 으로 열리는 독립 페이지). */
    @GetMapping("/attempts/{attemptId}")
    public String attempt(@PathVariable Long attemptId,
                          @AuthenticationPrincipal LoginUser loginUser,
                          Model model) {
        model.addAttribute("detail", examGradingService.attemptDetail(
                attemptId, loginUser.getId(), isAdmin(loginUser), BASE));
        return MODAL_VIEW;
    }

    /** 성적 목록 + 변경 이력. */
    @GetMapping("/exams/{examId}/grades")
    public String grades(@PathVariable Long examId,
                         @AuthenticationPrincipal LoginUser loginUser,
                         Model model) {
        boolean admin = isAdmin(loginUser);
        model.addAttribute("summary",
                examGradingService.summary(examId, loginUser.getId(), admin, BASE));
        model.addAttribute("rows", examGradingService.gradeList(examId, loginUser.getId(), admin));
        model.addAttribute("histories", examGradingService.historyList(examId, loginUser.getId(), admin));
        return GRADES_VIEW;
    }

    /* ===================== 채점 API (팝업이 fetch 로 호출) ===================== */

    /**
     * 수동 채점 저장.
     *
     * <p>확정된 성적이면 {@code reason} 이 필수다 — 없으면 400 {@code REASON_REQUIRED} 로 거부하고
     * 아무것도 바꾸지 않는다. 트랜잭션 안에서 던지므로 부분 저장이 남지 않는다.</p>
     */
    @PostMapping("/attempts/{attemptId}/scores")
    @ResponseBody
    public ResponseEntity<?> saveScores(@PathVariable Long attemptId,
                                        @RequestBody ManualScoreRequest request,
                                        @AuthenticationPrincipal LoginUser loginUser) {
        return handle(() -> examGradingService.saveScores(
                attemptId, request, loginUser.getId(), isAdmin(loginUser), BASE));
    }

    /** 성적 확정. 수동 채점이 남아 있으면 400 으로 막는다. */
    @PostMapping("/attempts/{attemptId}/confirm")
    @ResponseBody
    public ResponseEntity<?> confirm(@PathVariable Long attemptId,
                                     @AuthenticationPrincipal LoginUser loginUser) {
        return handle(() -> examGradingService.confirm(
                attemptId, loginUser.getId(), isAdmin(loginUser), BASE));
    }

    /* ===================== 다운로드 ===================== */

    /**
     * 성적 목록 다운로드 — 기본 형식 xlsx.
     *
     * <p>시트 "성적" + "정정이력" 두 장. 권한(강사는 담당 과정만)은 서비스가 본다.</p>
     */
    @GetMapping("/exams/{examId}/grades.xlsx")
    @ResponseBody
    public ResponseEntity<byte[]> gradesExcel(@PathVariable Long examId,
                                              @AuthenticationPrincipal LoginUser loginUser) {
        byte[] body = examGradingService.gradeExcel(examId, loginUser.getId(), isAdmin(loginUser));
        return ExcelDownload.attachment("성적_" + examGradingService.examName(examId), body);
    }

    /**
     * 성적 목록 CSV — 대체 형식.
     *
     * <p>엑셀이 설치되지 않은 환경이나 다른 시스템에 반입할 때를 위해 남겨 둔다.
     * 엑셀에서 바로 열리도록 UTF-8 BOM 을 붙인다.</p>
     */
    @GetMapping("/exams/{examId}/grades.csv")
    @ResponseBody
    public ResponseEntity<byte[]> gradesCsv(@PathVariable Long examId,
                                            @AuthenticationPrincipal LoginUser loginUser) {
        String csv = examGradingService.gradeCsv(examId, loginUser.getId(), isAdmin(loginUser));
        String name = "성적_" + examGradingService.examName(examId) + ".csv";
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"grades.csv\"; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    /* ===================== 내부 ===================== */

    /**
     * 채점 API 공통 예외 변환.
     * 규칙 위반(GradingException)은 400, 권한 위반은 403 — 화면이 둘을 구분해서 안내해야 한다.
     */
    public static ResponseEntity<?> handle(java.util.function.Supplier<AttemptGradingDetail> action) {
        try {
            AttemptGradingDetail detail = action.get();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", true);
            body.put("detail", detail);
            return ResponseEntity.ok(body);
        } catch (GradingException e) {
            log.warn("[채점] 규칙 위반 {} - {}", e.getCode(), e.getMessage());
            return ResponseEntity.badRequest().body(error(e.getCode(), e.getMessage()));
        } catch (AccessDeniedException e) {
            log.warn("[채점] 권한 없음 - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(error("FORBIDDEN", e.getMessage()));
        }
    }

    private static Map<String, Object> error(String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("code", code);
        body.put("message", message);
        return body;
    }

    public static boolean isAdmin(LoginUser loginUser) {
        return loginUser != null && loginUser.getRole() == Role.ADMIN;
    }
}
