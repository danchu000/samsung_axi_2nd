package com.ssa.lms.notice.repository;

import com.ssa.lms.notice.entity.NoticeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeCategoryRepository extends JpaRepository<NoticeCategory, Long> {

    Optional<NoticeCategory> findByCode(String code);

    /** 화면 셀렉트 박스용 — 사용중인 분류만 정렬해서. */
    List<NoticeCategory> findByActiveTrueOrderBySortOrderAsc();
}
