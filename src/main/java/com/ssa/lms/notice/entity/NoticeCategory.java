package com.ssa.lms.notice.entity;

import com.ssa.lms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공지 분류 코드.
 *
 * static/js/notices.js 는 'kdt' / '고교위탁' / '심화' 를 문자열로 박아뒀지만,
 * 기수·훈련유형이 늘어날 때마다 코드 수정이 필요해지므로 코드 테이블로 뺐다.
 * (설계안 §9-3 결정)
 */
@Entity
@Table(name = "notice_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 코드 값. 예: KDT, HIGHSCHOOL, ADVANCED */
    @Column(name = "code", length = 30, nullable = false, unique = true)
    private String code;

    /** 화면 표시명. 예: kdt, 고교위탁, 심화 */
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Builder
    public NoticeCategory(String code, String name, Integer sortOrder, boolean active) {
        this.code = code;
        this.name = name;
        this.sortOrder = sortOrder;
        this.active = active;
    }
}
