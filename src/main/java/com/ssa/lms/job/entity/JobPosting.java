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

    /**
     * <b>자격요건</b>에 적힌 기술만 (쉼표 구분). 없으면 null.
     *
     * <p>자격요건에는 학력·전공 이야기가 많고 기술은 적다. 그래도 여기 적힌 기술은
     * <b>"없으면 지원조차 안 되는 것"</b>이라 우대사항과 무게가 다르다. 그래서 따로 담는다.</p>
     *
     * <p>학력·전공은 여기 넣지 않는다 — {@link #education}, {@link #majorRestricted} 가 받는다.</p>
     */
    @Column(name = "required_skills", length = 1000)
    private String requiredSkills;

    /**
     * <b>우대사항</b>에 적힌 기술만 (쉼표 구분). 없으면 null.
     *
     * <p>실무 기술 스택은 대개 여기에 몰려 있다. 로드맵이 "남들과 갈리는 지점"을
     * 보여주려면 이쪽이 핵심이다.</p>
     */
    @Column(name = "preferred_skills", length = 1000)
    private String preferredSkills;

    /** 학력 요건 (학력무관 / 고졸 이상 / 초대졸 이상 / 대졸 이상). 표기가 없으면 null. */
    @Column(name = "education", length = 30)
    private String education;

    /**
     * 전공 제한이 있는지. 모르면 null — <b>false 로 채우지 않는다.</b>
     *
     * <p>비전공·전직 훈련생에게는 "전공 무관 68%" 같은 숫자가 실제로 필요한 정보다.
     * 모르는 것을 false 로 채우면 그 비율이 통째로 부풀려진다.</p>
     */
    @Column(name = "major_restricted")
    private Boolean majorRestricted;

    /**
     * 어느 수집처에서 왔는지 (워크넷 / 사람인 / AI 웹검색).
     *
     * <p>출처마다 신뢰도가 다르다. 공개 API 로 받은 것과 AI 가 웹에서 읽어온 것을
     * 섞어 두고 구분할 수 없으면, 나중에 링크 하나가 깨졌을 때 어디를 의심할지 알 수 없다.</p>
     */
    @Column(name = "collected_by", length = 20)
    private String collectedBy;

    /**
     * 공고 <b>원문</b>을 실제로 열어 읽었는지.
     *
     * <p>검색 요약문에는 자격요건·우대사항 본문이 없다. 원문을 못 연 공고는 기술을
     * 추측으로 채우면 안 되므로 비워 두는데, 그러면 나중에 <b>"왜 이 공고만 키워드가
     * 없지"</b>를 알 수 없다. 그래서 못 읽었다는 사실 자체를 남긴다.</p>
     */
    @Column(name = "body_fetched")
    private Boolean bodyFetched;

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
                       String requiredSkills, String preferredSkills, String education,
                       Boolean majorRestricted, String collectedBy, Boolean bodyFetched,
                       LocalDate postingDate, LocalDate expirationDate, LocalDate collectedAt) {
        this.externalId = externalId;
        this.jobGroup = jobGroup;
        this.companyName = companyName;
        this.title = title;
        this.url = url;
        this.experienceLevel = experienceLevel;
        this.location = location;
        this.keywords = keywords;
        this.requiredSkills = requiredSkills;
        this.preferredSkills = preferredSkills;
        this.education = education;
        this.majorRestricted = majorRestricted;
        this.collectedBy = collectedBy;
        this.bodyFetched = bodyFetched;
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
