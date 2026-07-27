package com.ssa.lms.web.trainee.notice;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.notice.service.NoticeService;
import com.ssa.lms.notice.service.NoticeVisibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 훈련생 공지 조회 (trainee/notices.html, notices-detail.html).
 *
 * 게시된 공지만, 그리고 전체 공지 + 본인이 수강 중인 과정 공지만 보인다.
 * 접근 권한: SecurityConfig 의 /trainee/** → TRAINEE, ADMIN.
 */
@Controller
@RequestMapping("/trainee/notice")
@RequiredArgsConstructor
public class TraineeNoticeController {

    private static final int PAGE_SIZE = 10;

    private final NoticeService noticeService;
    private final NoticeVisibilityService visibilityService;

    @GetMapping
    public String list(@RequestParam(name = "keyword", required = false) String keyword,
                       @RequestParam(name = "sort", defaultValue = "recent") String sort,
                       @RequestParam(name = "page", defaultValue = "1") int page,
                       @AuthenticationPrincipal LoginUser loginUser,
                       Model model) {
        Sort order = switch (sort) {
            case "old" -> Sort.by(Sort.Order.desc("pinned"), Sort.Order.asc("id"));
            case "title" -> Sort.by(Sort.Order.desc("pinned"), Sort.Order.asc("title"));
            default -> Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("id"));
        };
        Page<?> rows = noticeService.searchPublished(
                visibilityService.traineeCourseIds(loginUser.getId()), keyword,
                PageRequest.of(Math.max(page, 1) - 1, PAGE_SIZE, order));

        model.addAttribute("rows", rows.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("currentPage", Math.min(Math.max(page, 1), Math.max(rows.getTotalPages(), 1)));
        model.addAttribute("totalPages", Math.max(rows.getTotalPages(), 1));
        return "trainee/notices";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("notice", noticeService.getDetail(id, true));
        return "trainee/notices-detail";
    }
}
