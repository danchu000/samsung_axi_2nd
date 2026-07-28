package com.ssa.lms.web.admin.support;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.support.dto.QnaSearchCond;
import com.ssa.lms.support.dto.ResponseListRow;
import com.ssa.lms.support.service.QnaService;
import com.ssa.lms.support.service.TutoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 관리자·강사 공용 지원 운영 화면. 실제 데이터 권한은 principal ID로 다시 검사한다.
 *
 * <p><b>화면 매핑 (기존 정적 화면 전환 — CLAUDE.md 규칙)</b>
 * <ul>
 *   <li>{@code /admin/support/tutoring}, {@code /admin/support/qna}
 *       → {@code admin-06-support/admin-support-tutoring.html}
 *       (튜터링 / Q&amp;A 탭이 한 화면에 있는 원본 구조라 두 URL 이 같은 뷰를 쓴다)</li>
 *   <li>{@code /admin/support/qna/{id}} → {@code admin-06-support/admin-support-qna.html} (Q&amp;A 상세)</li>
 *   <li>{@code /admin/support/tutoring/{id}} → {@code admin-06-support/tutoring-detail.html}
 *       (채팅 모니터링 — 권한정의서(2) 18행에 따라 관리자는 <b>읽기 전용</b>)</li>
 *   <li>{@code /admin/support/response} → {@code admin-06-support/admin-support-response.html} (응답현황)</li>
 * </ul>
 */
@Controller
@RequestMapping("/admin/support")
@RequiredArgsConstructor
public class AdminSupportController {

    private static final int PAGE_SIZE = 10;

    private static final String VIEW_LIST = "admin/admin-06-support/admin-support-tutoring";
    private static final String VIEW_QNA_DETAIL = "admin/admin-06-support/admin-support-qna";
    private static final String VIEW_TUTORING_DETAIL = "admin/admin-06-support/tutoring-detail";
    private static final String VIEW_RESPONSE = "admin/admin-06-support/admin-support-response";

    private final QnaService qnaService;
    private final TutoringService tutoringService;

    /* ===== 목록 (탭 한 화면) ===== */

    @GetMapping("/qna")
    public String qnaList(@ModelAttribute QnaSearchCond cond,
                          @RequestParam(required = false) String roomKeyword,
                          @RequestParam(required = false) String roomStatus,
                          @RequestParam(name = "page", defaultValue = "1") int page,
                          Model model) {
        fillList(model, cond, roomKeyword, roomStatus, page);
        model.addAttribute("activeTab", "qna");
        return VIEW_LIST;
    }

    @GetMapping("/tutoring")
    public String tutoringList(@ModelAttribute QnaSearchCond cond,
                               @RequestParam(required = false) String roomKeyword,
                               @RequestParam(required = false) String roomStatus,
                               @RequestParam(name = "page", defaultValue = "1") int page,
                               Model model) {
        fillList(model, cond, roomKeyword, roomStatus, page);
        model.addAttribute("activeTab", "tutoring");
        return VIEW_LIST;
    }

    /**
     * 두 탭이 한 화면에 있으므로 어느 URL 로 들어오든 양쪽 데이터를 모두 채운다.
     * Q&amp;A 필터는 {@link QnaSearchCond}(keyword/status), 튜터링 필터는 roomKeyword/roomStatus 로
     * 파라미터 이름을 분리했다 — 원본 화면의 필터 행이 탭마다 따로 있기 때문.
     */
    private void fillList(Model model, QnaSearchCond cond, String roomKeyword, String roomStatus,
                          int page) {
        // 서버 페이징 — 예전에는 전체 Q&A 를 내려주고 JS 가 잘랐다
        org.springframework.data.domain.Page<com.ssa.lms.support.dto.QnaListRow> qnaPage =
                qnaService.search(cond, org.springframework.data.domain.PageRequest.of(
                        Math.max(page - 1, 0), PAGE_SIZE));
        model.addAttribute("rows", qnaPage.getContent());
        model.addAttribute("page", com.ssa.lms.web.PageView.of(qnaPage));
        model.addAttribute("qnaStats", qnaService.stats());
        model.addAttribute("rooms", tutoringService.searchAll(roomKeyword, roomStatus, null, null));
        model.addAttribute("tutoringStats", tutoringService.stats());
        model.addAttribute("roomKeyword", roomKeyword == null ? "" : roomKeyword);
        model.addAttribute("roomStatus", roomStatus == null ? "" : roomStatus);
    }

    /* ===== Q&A 상세 ===== */

    @GetMapping("/qna/{id}")
    public String qnaDetail(@PathVariable Long id, @AuthenticationPrincipal LoginUser user, Model model) {
        model.addAttribute("detail", qnaService.readDetail(id, user.getId(), true));
        model.addAttribute("staffOptions", qnaService.staffOptions());
        return VIEW_QNA_DETAIL;
    }

    @PostMapping("/qna/{id}/answer")
    public String answer(@PathVariable Long id, @AuthenticationPrincipal LoginUser user,
                         @RequestParam String content, RedirectAttributes ra) {
        qnaService.answer(id, user.getId(), content);
        ra.addFlashAttribute("message", "답변을 등록했습니다.");
        return "redirect:/admin/support/qna/" + id;
    }

    @PostMapping("/qna/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam Long assigneeId, RedirectAttributes ra) {
        qnaService.assign(id, assigneeId);
        ra.addFlashAttribute("message", "담당자를 배정했습니다.");
        return "redirect:/admin/support/qna/" + id;
    }

    @PostMapping("/qna/{id}/close")
    public String closeQna(@PathVariable Long id, RedirectAttributes ra) {
        qnaService.close(id);
        ra.addFlashAttribute("message", "질문을 종료 처리했습니다.");
        return "redirect:/admin/support/qna/" + id;
    }

    /* ===== 튜터링 상세 (채팅 모니터링) ===== */

    @GetMapping("/tutoring/{id}")
    public String tutoringDetail(@PathVariable Long id, @AuthenticationPrincipal LoginUser user, Model model) {
        boolean participant = tutoringService.isParticipant(tutoringService.getOrThrow(id), user.getId());

        // 담당 강사가 열면 훈련생이 보낸 메시지를 읽음 처리한다(openDetail).
        // 관리자가 열 때는 getDetail 로 조회만 한다 — 모니터링 때문에 당사자 읽음 상태가 바뀌면 안 된다.
        model.addAttribute("detail", participant
                ? tutoringService.openDetail(id, user.getId())
                : tutoringService.getDetail(id, user.getId()));
        model.addAttribute("staffOptions", tutoringService.instructorOptions());
        // 입력창은 대화 당사자(담당 강사)에게만. 관리자는 권한정의서(2) 18행에 따라 R 만.
        model.addAttribute("canSend", participant);
        return VIEW_TUTORING_DETAIL;
    }

    @PostMapping("/tutoring/{id}/assign")
    public String assignTutor(@PathVariable Long id, @RequestParam Long instructorId, RedirectAttributes ra) {
        tutoringService.assignInstructor(id, instructorId);
        ra.addFlashAttribute("message", "튜터를 배정했습니다.");
        return "redirect:/admin/support/tutoring/" + id;
    }

    @PostMapping("/tutoring/{id}/close")
    public String closeTutor(@PathVariable Long id, RedirectAttributes ra) {
        tutoringService.closeRoom(id);
        ra.addFlashAttribute("message", "튜터링을 종료했습니다.");
        return "redirect:/admin/support/tutoring/" + id;
    }

    /**
     * 담당 강사의 답장. 관리자가 호출하면 {@code TutoringService.sendMessage} 의
     * 당사자 검사에 걸려 거부된다 (권한정의서(2) 18행: 채팅 메시지는 관리자 R).
     */
    @PostMapping("/tutoring/{id}/messages")
    public String sendMessage(@PathVariable Long id, @AuthenticationPrincipal LoginUser user,
                              @RequestParam String content, RedirectAttributes ra) {
        // 관리자는 당사자가 아니므로 서비스가 AccessDeniedException(→403) 을 던진다 — 여기서 잡지 않는다.
        // 종료된 방 전송(업무규칙 위반)만 flash 로 되돌린다. raw IllegalStateException 을 두면 500 이 됐다.
        try {
            tutoringService.sendMessage(id, user.getId(), content);
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/support/tutoring/" + id;
    }

    /* ===== 응답현황 ===== */

    @GetMapping("/response")
    public String response(Model model) {
        List<ResponseListRow> rows = new ArrayList<>(qnaService.responseRows());
        rows.addAll(tutoringService.responseRows());
        rows.sort(Comparator.comparingLong(ResponseListRow::elapsedMinutes).reversed());
        model.addAttribute("rows", rows);
        return VIEW_RESPONSE;
    }
}
