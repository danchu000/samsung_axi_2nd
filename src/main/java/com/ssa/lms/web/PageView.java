package com.ssa.lms.web;

import org.springframework.data.domain.Page;

/**
 * 화면에 내려줄 페이지 정보.
 *
 * <p>Spring 의 {@link Page} 를 그대로 템플릿에 넘기면 화면이 0-based 인지 1-based 인지
 * 매번 헷갈리고, 인라인 JS 로 직렬화할 때 필요 없는 필드(sort, pageable 등)까지 실린다.
 * 그래서 화면이 쓰는 값만 <b>1-based</b> 로 담아 넘긴다.</p>
 *
 * <p>화면에서는 {@code window._serverPage} 로 내려주면
 * {@code static/js/server-pagination.js} 가 기존 페이지네이션 DOM 을 그대로 재사용해
 * 링크를 그린다.</p>
 */
public record PageView(
        /** 현재 페이지 (1부터). */
        int page,
        int totalPages,
        long totalCount,
        int size,
        boolean hasPrev,
        boolean hasNext
) {

    public static PageView of(Page<?> page) {
        int total = Math.max(page.getTotalPages(), 1);
        return new PageView(
                page.getNumber() + 1,
                total,
                page.getTotalElements(),
                page.getSize(),
                page.hasPrevious(),
                page.hasNext());
    }
}
