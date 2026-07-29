package com.ssa.lms.job;

import com.ssa.lms.job.dto.JobCollectionSummary;
import com.ssa.lms.job.entity.JobPosting;
import com.ssa.lms.job.repository.JobPostingRepository;
import com.ssa.lms.job.service.RoadmapService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [기능 1-관리자] 수집 현황 요약을 고정한다.
 *
 * <p>관리자가 이 화면으로 "수집이 멈췄는지"를 판단한다. 멈춘 것을 정상으로 보이게 하면
 * 훈련생이 몇 주째 지난 시장을 보고 있어도 아무도 모른다.</p>
 */
@SpringBootTest
@Transactional
class JobCollectionSummaryTest {

    @Autowired RoadmapService roadmapService;
    @Autowired JobPostingRepository jobPostingRepository;

    private void posting(String group, String keywords, LocalDate collectedAt) {
        jobPostingRepository.save(JobPosting.builder()
                .externalId("s-" + System.nanoTime())
                .jobGroup(group).companyName("회사").title("공고")
                .keywords(keywords).experienceLevel("신입")
                .postingDate(LocalDate.now().minusDays(1))
                .collectedAt(collectedAt)
                .build());
    }

    @Test
    @DisplayName("수집 전에는 날짜를 지어내지 않는다")
    void 수집_전() {
        JobCollectionSummary s = roadmapService.collectionSummary();
        assertThat(s.collectedAt()).isNull();
        assertThat(s.staleDays()).isNull();
        assertThat(s.isStale()).isFalse();
    }

    @Test
    @DisplayName("2주 넘게 갱신이 없으면 '멈춤'으로 표시한다 — 주 1회 수집인데 2주면 이상하다")
    void 오래되면_경고() {
        posting("backend", "Java", LocalDate.now().minusDays(20));

        JobCollectionSummary s = roadmapService.collectionSummary();

        assertThat(s.staleDays()).isGreaterThanOrEqualTo(20);
        assertThat(s.isStale()).isTrue();
    }

    @Test
    @DisplayName("표본이 모자란 직무는 '표본 부족'으로 구분한다 — 수집됐는데 안 보이는 이유를 알아야 한다")
    void 표본_부족을_구분한다() {
        for (int i = 0; i < 3; i++) posting("backend", "Java,Spring", LocalDate.now());

        JobCollectionSummary.GroupSummary backend = roadmapService.collectionSummary().groups()
                .stream().filter(g -> g.id().equals("backend")).findFirst().orElseThrow();

        assertThat(backend.postingCount()).isEqualTo(3);
        assertThat(backend.analyzable())
                .as("3건으로 비율을 말하면 거짓이다 — 훈련생 화면에서 빠진다")
                .isFalse();
        assertThat(backend.name()).isEqualTo("백엔드 개발자");
    }

    @Test
    @DisplayName("요구 역량 요약은 공고 대비 비율로 준다 — 건수만 있으면 많은지 알 수 없다")
    void 요구역량_비율() {
        for (int i = 0; i < 6; i++) posting("backend", "Java,Spring", LocalDate.now());
        for (int i = 0; i < 2; i++) posting("backend", "Java", LocalDate.now());

        JobCollectionSummary s = roadmapService.collectionSummary();

        assertThat(s.topSkills()).isNotEmpty();
        JobCollectionSummary.SkillCount java = s.topSkills().stream()
                .filter(k -> k.label().equals("Java")).findFirst().orElseThrow();
        assertThat(java.count()).isEqualTo(8);
        assertThat(java.percent()).isEqualTo(100);
    }
}
