package com.ssa.lms.job.entity;

import com.ssa.lms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 수집한 채용공고 1건. [기능 1] 직무 로드맵의 원천 데이터.
 *
 * <p><b>왜 저장하는가</b><br>
 * 로드맵은 "공고 68%가 테스트 코드를 요구한다" 같은 문장을 근거로 삼는다.
 * 그 숫자를 화면에서 지어내면 안 되고, 원문 공고를 같이 보여줄 수 있어야 한다.
 * 그래서 수집한 공고를 그대로 남긴다.</p>
 *
 * <p><b>외부 id 로 중복을 막는다</b><br>
 * 주 1회 수집이라 같은 공고가 여러 번 잡힌다. 사람인 공고 id 를 유니크로 두고
 * 이미 있으면 건너뛴다. 중복이 쌓이면 "요구 빈도"가 통째로 거짓이 된다.</p>
 *
 * <p><b>개인정보 없음</b> — 기업 공고만 담는다. 지원자 정보는 다루지 않는다.</p>
 */
@Entity
@Table(name = "job_posting",
        indexes = {
                @Index(name = "ux_job_posting_external", columnList = "external_id", unique = true),
                @Index(name = "ix_job_posting_group", columnList = "job_group, posting_date")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobPosting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 사람인 공고 id. 중복 수집을 막는 유일 키. */
    @Column(name = "external_id", nullable = false, length = 40)
    private String externalId;

    /** 우리가 분류한 직무 그룹 (backend/frontend/data …). 로드맵 탭 단위. */
    @Column(name = "job_group", nullable = false, length = 30)
    private String jobGroup;

    @Column(nullable = false, length = 200)
    private String companyName;

    @Column(nullable = false, length = 300)
    private String title;

    /** 공고 원문 링크. 근거를 감추지 않기 위해 반드시 함께 보여준다. */
    @Column(length = 500)
    private String url;

    @Column(length = 100)
    private String experienceLevel;

    @Column(length = 200)
    private String location;

    /**
     * 사람인이 주는 키워드(쉼표 구분). 요구 역량 집계의 원재료다.
     * AI 로 뽑지 않는다 — 제공되는 값이 있는데 모델을 부르면 돈만 나가고 더 부정확하다.
     */
    @Column(length = 1000)
    private String keywords;

    @Column(name = "posting_date")
    private LocalDate postingDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    /** 이 공고가 수집된 회차의 기준일. 화면의 "마지막 수집" 표기에 쓴다. */
    @Column(name = "collected_at", nullable = false)
    private LocalDate collectedAt;

    @Builder
    private JobPosting(String externalId, String jobGroup, String companyName, String title,
                       String url, String experienceLevel, String location, String keywords,
                       LocalDate postingDate, LocalDate expirationDate, LocalDate collectedAt) {
        this.externalId = externalId;
        this.jobGroup = jobGroup;
        this.companyName = companyName;
        this.title = title;
        this.url = url;
        this.experienceLevel = experienceLevel;
        this.location = location;
        this.keywords = keywords;
        this.postingDate = postingDate;
        this.expirationDate = expirationDate;
        this.collectedAt = collectedAt;
    }

    /** 쉼표로 붙어 오는 키워드를 갈라 준다. 빈 조각은 버린다. */
    public List<String> keywordList() {
        if (keywords == null || keywords.isBlank()) return List.of();
        return Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
