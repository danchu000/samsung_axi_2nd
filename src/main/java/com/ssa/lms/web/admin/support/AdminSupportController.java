package com.ssa.lms.web.admin.support;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.support.dto.QnaSearchCond;
import com.ssa.lms.support.dto.ResponseListRow;
import com.ssa.lms.support.service.QnaService;
import com.ssa.lms.support.service.TutoringService;
import com.ssa.lms.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
 *
 * <p><b>역할별 차이 (SecurityConfig 는 /admin/support/** 를 ADMIN·INSTRUCTOR 에게 열어 둔다)</b>
 * <ul>
 *   <li><b>목록 범위</b> — 관리자는 전체(모니터링 권한), 강사는 <b>본인에게 배정된 건만</b>.
 *       1:1 튜터링 방은 훈련생의 사적 대화라 남의 방이 목록에 뜨면 개인정보 노출이다.</li>
 *   <li><b>배정·종료</b> — 운영 액션이므로 관리자 전용. URL 규칙만으로는 강사도 POST 할 수 있어
 *       {@link #assertAdmin} 으로 한 번 더 막는다.</li>
 *   <li><b>레이아웃</b> — 강사에게 관리자 사이드바를 그리면 메뉴가 모두 {@code /admin/**} 이라
 *       누르는 순간 403 이다. {@code layout} 모델 속성으로 역할에 맞는 fragment 를 고른다.</li>
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
                          @AuthenticationPrincipal LoginUser user,
                          Model model) {
        fillList(model, cond, roomKeyword, roomStatus, page, user);
        model.addAttribute("activeTab", "qna");
        return VIEW_LIST;
    }

    @GetMapping("/tutoring")
    public String tutoringList(@ModelAttribute QnaSearchCond cond,
                               @RequestParam(required = false) String roomKeyword,
                               @RequestParam(required = false) String roomStatus,
                               @RequestParam(name = "page", defaultValue = "1") int page,
                               @AuthenticationPrincipal LoginUser user,
                               Model model) {
        fillList(model, cond, roomKeyword, roomStatus, page, user);
        model.addAttribute("activeTab", "tutoring");
        return VIEW_LIST;
    }

    /**
     * 두 탭이 한 화면에 있으므로 어느 URL 로 들어오든 양쪽 데이터를 모두 채운다.
     * Q&amp;A 필터는 {@link QnaSearchCond}(keyword/status), 튜터링 필터는 roomKeyword/roomStatus 로
     * 파라미터 이름을 분리했다 — 원본 화면의 필터 행이 탭마다 따로 있기 때문.
     *
     * <p><b>범위:</b> 강사가 열면 튜터링은 본인이 배정된 방, Q&amp;A 는 본인이 담당자인 질문만
     * 내려간다({@link #scopeIdOf}). 예전에는 두 목록 모두 전체를 내려줘 강사가 남의 1:1 대화 방
     * 목록까지 볼 수 있었다. 관리자는 모니터링 권한이 있으므로 그대로 전체를 본다.</p>
     */
    private void fillList(Model model, QnaSearchCond cond, String roomKeyword, String roomStatus,
                          int page, LoginUser user) {
        Long scopeId = scopeIdOf(user);   // null = 제한 없음(관리자)

        // 서버 페이징 — 예전에는 전체 Q&A 를 내려주고 JS 가 잘랐다
        org.springframework.data.domain.Page<com.ssa.lms.support.dto.QnaListRow> qnaPage =
                qnaService.search(cond.scopedTo(scopeId), org.springframework.data.domain.PageRequest.of(
                        Math.max(page - 1, 0), PAGE_SIZE));
        model.addAttribute("rows", qnaPage.getContent());
        model.addAttribute("page", com.ssa.lms.web.PageView.of(qnaPage));
        model.addAttribute("qnaStats", qnaService.stats());
        model.addAttribute("rooms", tutoringService.searchAll(roomKeyword, roomStatus, null, scopeId));
        model.addAttribute("tutoringStats", tutoringService.stats());
        model.addAttribute("roomKeyword", roomKeyword == null ? "" : roomKeyword);
        model.addAttribute("roomStatus", roomStatus == null ? "" : roomStatus);
        fillLayout(model, user);
    }

    /* ===== Q&A 상세 ===== */

    @GetMapping("/qna/{id}")
    public String qnaDetail(@PathVariable Long id, @AuthenticationPrincipal LoginUser user, Model model) {
        model.addAttribute("detail", qnaService.readDetail(id, user.getId(), true));
        model.addAttribute("staffOptions", qnaService.staffOptions());
        fillLayout(model, user);
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
    public String assign(@PathVariable Long id, @RequestParam Long assigneeId,
                         @AuthenticationPrincipal LoginUser user, RedirectAttributes ra) {
        assertAdmin(user);
        qnaService.assign(id, assigneeId);
        ra.addFlashAttribute("message", "담당자를 배정했습니다.");
        return "redirect:/admin/support/qna/" + id;
    }

    @PostMapping("/qna/{id}/close")
    public String closeQna(@PathVariable Long id,
                           @AuthenticationPrincipal LoginUser user, RedirectAttributes ra) {
        assertAdmin(user);
        qnaService.close(id);
        ra.addFlashAttribute("message", "질문을 종료 처리했습니다.");
        return "redirect:/admin/support/qna/" + id;
    }

    /* ===== 튜터링 상세 (채팅 모니터링) ===== */

    @GetMapping("/tutoring/{id}")
    public String tutoringDetail(@PathVariable Long id, @AuthenticationPrincipal LoginUser user, Model model) {
        boolean participant = tutoringService.isParticipant(tutoringService.getOrThrow(id), user.getId());

        // 강사는 본인이 배정된 방만 연다. 목록만 스코프하면 /tutoring/2 처럼 URL 을 직접 쳐서
        // 남의 1:1 대화 전문이 그대로 열린다 — 목록 스코프와 같은 개인정보 경계다.
        // 관리자는 권한정의서(2) 18행의 모니터링 권한으로 모든 방을 읽을 수 있다.
        if (!isAdmin(user) && !participant) {
            throw new AccessDeniedException("배정된 튜터링 방만 열람할 수 있습니다.");
        }

        // 담당 강사가 열면 훈련생이 보낸 메시지를 읽음 처리한다(openDetail).
        // 관리자가 열 때는 getDetail 로 조회만 한다 — 모니터링 때문에 당사자 읽음 상태가 바뀌면 안 된다.
        model.addAttribute("detail", participant
                ? tutoringService.openDetail(id, user.getId())
                : tutoringService.getDetail(id, user.getId()));
        model.addAttribute("staffOptions", tutoringService.instructorOptions());
        // 입력창은 대화 당사자(담당 강사)에게만. 관리자는 권한정의서(2) 18행에 따라 R 만.
        model.addAttribute("canSend", participant);
        fillLayout(model, user);
        return VIEW_TUTORING_DETAIL;
    }

    @PostMapping("/tutoring/{id}/assign")
    public String assignTutor(@PathVariable Long id, @RequestParam Long instructorId,
                              @AuthenticationPrincipal LoginUser user, RedirectAttributes ra) {
        assertAdmin(user);
        tutoringService.assignInstructor(id, instructorId);
        ra.addFlashAttribute("message", "튜터를 배정했습니다.");
        return "redirect:/admin/support/tutoring/" + id;
    }

    @PostMapping("/tutoring/{id}/close")
    public String closeTutor(@PathVariable Long id,
                             @AuthenticationPrincipal LoginUser user, RedirectAttributes ra) {
        assertAdmin(user);
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

    /* ===== 역할 판정 (SecurityConfig 가 이 URL 을 ADMIN·INSTRUCTOR 양쪽에 열어 둔다) ===== */

    private boolean isAdmin(LoginUser user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    /**
     * 목록에 적용할 본인 범위. 관리자는 {@code null}(제한 없음 — 모니터링 권한),
     * 강사는 본인 id 로 배정 필터를 건다.
     */
    private Long scopeIdOf(LoginUser user) {
        if (user == null) {
            // 인증 필수 경로라 정상 흐름에서는 오지 않는다. 오더라도 NPE(500) 대신 닫는 쪽으로 떨어뜨린다.
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        return isAdmin(user) ? null : user.getId();
    }

    /**
     * 배정·종료처럼 <b>운영 액션</b>은 관리자 전용이다.
     * URL 규칙(/admin/support/** → ADMIN·INSTRUCTOR)만으로는 강사도 POST 할 수 있어
     * 여기서 데이터 수준으로 한 번 더 막는다 —
     * {@link com.ssa.lms.web.AccessDeniedAdvice} 가 403 안내 화면으로 받는다.
     */
    private void assertAdmin(LoginUser user) {
        if (!isAdmin(user)) {
            throw new AccessDeniedException("배정·종료 처리는 관리자만 할 수 있습니다.");
        }
    }

    /**
     * 역할에 맞는 공통 레이아웃 fragment 를 모델에 넣는다.
     *
     * <p>이 화면들은 관리자·강사가 같은 뷰를 쓰는데 {@code fragments/admin} 을 고정으로 그리면
     * 강사에게 관리자 메뉴(/admin/users/** 등)가 뜨고 누르는 순간 403 이 된다(커밋 c459838 과 같은 부류).
     * 화면 마크업은 그대로 두고 fragment 이름만 바꿔 끼운다.</p>
     */
    private void fillLayout(Model model, LoginUser user) {
        boolean admin = isAdmin(user);
        model.addAttribute("isAdmin", admin);
        model.addAttribute("layout", admin ? "fragments/admin" : "fragments/instructor");
        // 사이드바 active 키가 레이아웃마다 다르다 (admin: support-tutoring / instructor: tutoring)
        model.addAttribute("sidebarActive", admin ? "support-tutoring" : "tutoring");
        // common-funtion.js 가 body 의 이 값으로 [data-user-role] 요소를 숨긴다
        model.addAttribute("bodyRole", admin ? "admin" : "instructor");
    }
}
