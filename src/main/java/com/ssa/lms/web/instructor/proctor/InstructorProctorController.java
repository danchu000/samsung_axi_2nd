package com.ssa.lms.web.instructor.proctor;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.proctor.dto.EventLogRow;
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
 * 강사 시험 모니터링.
 *
 * <p>접근 권한: SecurityConfig 의 {@code /instructor/proctor/**} → ADMIN, INSTRUCTOR.
 * 그 위에 서비스가 <b>담당 과정인지</b>를 다시 본다 (권한정의서 △).</p>
 *
 * <p><b>무효 처리 엔드포인트는 여기에 없다.</b> 강사에게 허용되지 않는 조치라
 * 라우트 자체를 만들지 않았다 (관리자 컨트롤러에서 역할 검사로 한 번 더 막힌다).</p>
 *
 * <p>화면은 기존 정적 화면 전환 — {@code instructor/proctor/exams.html}(목록),
 * {@code instructor/proctor/recordings.html}(녹화·제재).</p>
 */
@Controller
@RequestMapping("/instructor/proctor")
@RequiredArgsConstructor
public class InstructorProctorController {

    private static final String LIST_VIEW = "instructor/proctor/exams";
    private static final String RECORDING_VIEW = "instructor/proctor/recordings";

    private static final String DETAIL_PREFIX = "/instructor/proctor/exams/";
    private static final String STREAM_PREFIX = "/instructor/proctor/recordings/";

    private final ProctorMonitorService proctorMonitorService;
    private final ProctorWarningService proctorWarningService;
    private final ExamRecordingService examRecordingService;

    /** 담당 과정 시험의 응시 현황 목록. */
    @GetMapping("/exams")
    public String list(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("rows",
                proctorMonitorService.monitoringList(viewer(loginUser), DETAIL_PREFIX));
        return LIST_VIEW;
    }

    /**
     * 시험 한 건의 감독 상세.
     *
     * <p>강사용 실시간 감독 전용 템플릿이 없어서 녹화 화면(우측 패널에 제재 UI 가 이미 있다)을
     * 상세 화면으로 쓴다. 새 화면을 만들지 않는다는 규칙(CLAUDE.md)을 지키기 위한 선택이다.</p>
     */
    @GetMapping("/exams/{examId}")
    public String detail(@PathVariable Long examId,
                         @AuthenticationPrincipal LoginUser loginUser,
                         Model model) {
        model.addAttribute("monitor",
                proctorMonitorService.live(examId, viewer(loginUser), urls()));
        model.addAttribute("rows", examRecordingService.list(viewer(loginUser), STREAM_PREFIX));
        return RECORDING_VIEW;
    }

    /** 녹화 목록. */
    @GetMapping("/recordings")
    public String recordings(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("rows", examRecordingService.list(viewer(loginUser), STREAM_PREFIX));
        return RECORDING_VIEW;
    }

    @GetMapping("/attempt/{attemptId}/events")
    @ResponseBody
    public List<EventLogRow> events(@PathVariable Long attemptId,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        return proctorMonitorService.events(attemptId, viewer(loginUser));
    }

    @GetMapping("/attempt/{attemptId}/warnings")
    @ResponseBody
    public List<ProctorWarningRow> warnings(@PathVariable Long attemptId,
                                            @AuthenticationPrincipal LoginUser loginUser) {
        return proctorWarningService.findByAttempt(attemptId, viewer(loginUser));
    }

    /** 이상행위 경고 발송 — 강사도 가능 (권한정의서(1) 16행 △). 담당 과정이 아니면 403. */
    @PostMapping("/attempt/{attemptId}/warning")
    public String sendWarning(@PathVariable Long attemptId,
                              @RequestParam(value = "message", required = false) String message,
                              @RequestParam(value = "redirect", required = false) String redirect,
                              @AuthenticationPrincipal LoginUser loginUser,
                              RedirectAttributes redirectAttributes) {
        ProctorWarningRow row = proctorWarningService.send(attemptId, viewer(loginUser), message);
        redirectAttributes.addFlashAttribute("message", "경고를 발송했습니다: " + row.message());
        return "redirect:" + backTo(redirect);
    }

    @GetMapping("/recordings/{recordingId}/stream")
    @ResponseBody
    public ResponseEntity<Resource> stream(@PathVariable Long recordingId,
                                           @AuthenticationPrincipal LoginUser loginUser) {
        Resource resource = examRecordingService.stream(recordingId, viewer(loginUser));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(resource);
    }

    private ProctorViewer viewer(LoginUser loginUser) {
        return new ProctorViewer(loginUser.getId(), loginUser.getRole());
    }

    private ProctorMonitorService.ProctorUrls urls() {
        return new ProctorMonitorService.ProctorUrls(
                "/instructor/proctor/attempt/", "/instructor/proctor/exams");
    }

    private String backTo(String redirect) {
        if (redirect != null && redirect.startsWith("/instructor/proctor")) {
            return redirect;
        }
        return "/instructor/proctor/exams";
    }
}
