package com.ssa.lms.web.instructor.notice;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.notice.service.NoticeService;
import com.ssa.lms.notice.service.NoticeVisibilityService;
import lombok.RequiredArgsConstructor;
import com.ssa.lms.demo.SampleScreenData;
import com.ssa.lms.notice.dto.NoticeListRow;
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
 * 강사 공지 조회 (instructor/notices.html, notice-detail.html).
 *
 * 전체 공지 + 담당 과정 공지만 보인다 (권한정의서 △ — 담당 과정 한정).
 * 등록/수정은 /admin/notice/** 가 이미 INSTRUCTOR 에게 열려 있어 그쪽 화면을 재사용한다.
 */
@Controller
@RequestMapping("/instructor/notice")
@RequiredArgsConstructor
public class InstructorNoticeController {

    private static final int PAGE_SIZE = 10;

    private final NoticeService noticeService;
    private final com.ssa.lms.demo.SampleInstructorData sampleData;
    private final NoticeVisibilityService visibilityService;

    @GetMapping
    public String list(@RequestParam(name = "keyword", required = false) String keyword,
                       @RequestParam(name = "page", defaultValue = "1") int page,
                       @AuthenticationPrincipal LoginUser loginUser,
                       Model model) {
        Page<NoticeListRow> rows = noticeService.searchPublished(
                visibilityService.instructorCourseIds(loginUser.getId()), keyword,
                PageRequest.of(Math.max(page, 1) - 1, PAGE_SIZE,
                        Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("id"))));

        // 검색 결과 0건과 공지 자체가 없는 것은 다르다 — 검색어 없이 비었을 때만 예시로 채운다.
        var filled = (keyword == null || keyword.isBlank())
                ? sampleData.fill(rows.getContent(), sampleData::notices)
                : new SampleScreenData.Filled<>(rows.getContent(), false);
        model.addAttribute("rows", filled.rows());
        model.addAttribute("sampleData", filled.sample());
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", Math.min(Math.max(page, 1), Math.max(rows.getTotalPages(), 1)));
        model.addAttribute("totalPages", Math.max(rows.getTotalPages(), 1));
        return "instructor/notices";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("notice", noticeService.getPublishedDetail(id,
                visibilityService.instructorCourseIds(loginUser.getId()), true));
        return "instructor/notice-detail";
    }
}
