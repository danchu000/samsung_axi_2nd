package com.ssa.lms.web.admin.proctor;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.proctor.dto.EventLogRow;
import com.ssa.lms.proctor.dto.LiveMonitorView;
import com.ssa.lms.proctor.dto.ProctorWarningRow;
import com.ssa.lms.proctor.service.ExamRecordingService;
import com.ssa.lms.proctor.service.ProctorMonitorService;
import com.ssa.lms.proctor.service.ProctorViewer;
import com.ssa.lms.proctor.service.ProctorWarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 관리자 시험 모니터링(감독).
 *
 * <p>접근 권한: SecurityConfig 의 {@code /admin/evaluation/**} → ADMIN, INSTRUCTOR.
 * <b>강사도 이 URL 로 들어온다</b>는 게 중요하다 — 그래서 조회는 담당 과정으로 좁히고,
 * 응시 무효 처리는 서비스가 역할을 다시 본다 (권한정의서(1) 16행).</p>
 *
 * <p>화면은 기존 정적 화면을 전환해서 쓴다 (새로 만들지 않는다).</p>
 * <ul>
 *   <li>{@code admin-04-evaluation/admin-evaluation-monitoring.html} — 모니터링 목록</li>
 *   <li>{@code admin-04-evaluation/admin-evaluation-monitoring-live.html} — 실시간 감독</li>
 *   <li>{@code admin-04-evaluation/recordings.html} — 녹화 목록</li>
 * </ul>
 */
@Controller
@RequestMapping("/admin/evaluation/monitoring")
@RequiredArgsConstructor
public class AdminProctorController {

    private static final String LIST_VIEW = "admin/admin-04-evaluation/admin-evaluation-monitoring";
    private static final String LIVE_VIEW = "admin/admin-04-evaluation/admin-evaluation-monitoring-live";
    private static final String RECORDING_VIEW = "admin/admin-04-evaluation/recordings";

    private static final String DETAIL_PREFIX = "/admin/evaluation/monitoring/";
    private static final String STREAM_PREFIX = "/admin/evaluation/monitoring/recordings/";

    private final ProctorMonitorService proctorMonitorService;
    private final ProctorWarningService proctorWarningService;
    private final ExamRecordingService examRecordingService;

    /* ===================== 화면 ===================== */

    /** 모니터링 목록 — 시험별 응시중/완료/미응시. */
    @GetMapping
    public String list(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("rows",
                proctorMonitorService.monitoringList(viewer(loginUser), DETAIL_PREFIX));
        return LIST_VIEW;
    }

    /** 실시간 감독 — 응시자 목록·상태·남은 시간·이상행위 카운트. 카메라 영상은 아직 샘플 더미다. */
    @GetMapping("/{examId}")
    public String live(@PathVariable Long examId,
                       @AuthenticationPrincipal LoginUser loginUser,
                       Model model) {
        LiveMonitorView view = proctorMonitorService.live(examId, viewer(loginUser), urls());
        model.addAttribute("monitor", view);
        return LIVE_VIEW;
    }

    /** 녹화 목록. */
    @GetMapping("/recordings")
    public String recordings(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("rows", examRecordingService.list(viewer(loginUser), STREAM_PREFIX));
        return RECORDING_VIEW;
    }

    /* ===================== 조회 API ===================== */

    /**
     * 입·퇴장 및 이상행위 로그 (시각 · 유형 · 심각도 · IP).
     * 감독 화면이 모달로 띄우므로 JSON 으로 내린다.
     */
    @GetMapping("/attempt/{attemptId}/events")
    @ResponseBody
    public List<EventLogRow> events(@PathVariable Long attemptId,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        return proctorMonitorService.events(attemptId, viewer(loginUser));
    }

    /** 발송한 경고 이력. */
    @GetMapping("/attempt/{attemptId}/warnings")
    @ResponseBody
    public List<ProctorWarningRow> warnings(@PathVariable Long attemptId,
                                            @AuthenticationPrincipal LoginUser loginUser) {
        return proctorWarningService.findByAttempt(attemptId, viewer(loginUser));
    }

    /* ===================== 처리 ===================== */

    /** 이상행위 경고 발송 — 관리자·강사 모두 가능. */
    @PostMapping("/attempt/{attemptId}/warning")
    public String sendWarning(@PathVariable Long attemptId,
                              @RequestParam(value = "message", required = false) String message,
                              @RequestParam(value = "redirect", required = false) String redirect,
                              @AuthenticationPrincipal LoginUser loginUser,
                              RedirectAttributes redirectAttributes) {
        ProctorWarningRow row = proctorWarningService.send(attemptId, viewer(loginUser), message);
        redirectAttributes.addFlashAttribute("message", "경고를 발송했습니다: " + row.message());
        return "redirect:" + backTo(redirect, attemptId);
    }

    /**
     * 응시 무효 처리 — <b>관리자만</b>. 강사가 호출하면 서비스가 403 을 던진다.
     * (SecurityConfig 는 이 URL 을 강사에게도 열어두므로 URL 만으로는 막히지 않는다)
     */
    @PostMapping("/attempt/{attemptId}/void")
    public String voidAttempt(@PathVariable Long attemptId,
                              @RequestParam(value = "reason", required = false) String reason,
                              @RequestParam(value = "redirect", required = false) String redirect,
                              @AuthenticationPrincipal LoginUser loginUser,
                              RedirectAttributes redirectAttributes) {
        proctorMonitorService.voidAttempt(attemptId, viewer(loginUser), reason);
        redirectAttributes.addFlashAttribute("message", "응시를 무효 처리했습니다.");
        return "redirect:" + backTo(redirect, attemptId);
    }

    /* ===================== 녹화 스트리밍 ===================== */

    /**
     * 녹화 재생. 파일은 웹 루트 밖에 있고 이 엔드포인트만이 유일한 통로다.
     * 인증(SecurityConfig) + 담당 과정 판정(서비스) 을 모두 통과해야 스트림이 열린다.
     */
    @GetMapping("/recordings/{recordingId}/stream")
    @ResponseBody
    public ResponseEntity<Resource> stream(@PathVariable Long recordingId,
                                           @AuthenticationPrincipal LoginUser loginUser) {
        Resource resource = examRecordingService.stream(recordingId, viewer(loginUser));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                // 녹화물은 개인영상정보다 — 프록시/브라우저 캐시에 남기지 않는다
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(resource);
    }

    /* ===================== 내부 ===================== */

    private ProctorViewer viewer(LoginUser loginUser) {
        return new ProctorViewer(loginUser.getId(), loginUser.getRole());
    }

    private ProctorMonitorService.ProctorUrls urls() {
        return new ProctorMonitorService.ProctorUrls(
                "/admin/evaluation/monitoring/attempt/", DETAIL_PREFIX);
    }

    /** 되돌아갈 화면. 외부 URL 주입을 막으려고 내부 경로만 허용한다. */
    private String backTo(String redirect, Long attemptId) {
        if (redirect != null && redirect.startsWith("/admin/evaluation/monitoring")) {
            return redirect;
        }
        return DETAIL_PREFIX;
    }
}
