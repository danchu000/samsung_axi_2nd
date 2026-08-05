package com.ssa.lms.web.ai;

import com.ssa.lms.job.entity.JobPosting;
import com.ssa.lms.job.repository.JobPostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 직무 로드맵이 <b>수집된 공고가 있을 때</b> 끝까지 렌더되는지 고정한다.
 *
 * <p>기존 {@code AiScreenRenderTest} 는 {@code @WithMockUser} 라 principal 이 null 이고,
 * 로컬 DB 에는 공고가 한 건도 없다. 그래서 로드맵이 <b>서버 값을 실제로 내려보내는 경로</b>
 * (인라인 JSON 직렬화 포함)를 한 번도 타지 않았다 — 운영에서만 터지는 구멍이 된다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoadmapPostingsRenderTest {

    @Autowired MockMvc mvc;
    @Autowired JobPostingRepository jobPostingRepository;

    /** MIN_SAMPLE(5) 이상이어야 그 직무 탭이 만들어진다. */
    @BeforeEach
    void 공고를_심는다() {
        jobPostingRepository.deleteAll();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 6; i++) {
            jobPostingRepository.save(JobPosting.builder()
                    .externalId("test-" + i)
                    .jobGroup("backend")
                    .companyName("(주)테스트" + i)
                    .title("백엔드 개발자 " + i)
                    .url("https://example.com/" + i)
                    .experienceLevel("신입~3년")
                    .keywords("Java, Spring Boot, Docker, AWS")
                    .postingDate(today.minusDays(i))
                    .collectedAt(today)
                    .build());
        }
    }

    @Test
    @WithUserDetails("trainee1")
    @DisplayName("수집된 공고가 있으면 로드맵이 끝까지 렌더된다")
    void 공고기반_로드맵_렌더() throws Exception {
        MvcResult res = mvc.perform(get("/trainee/ai/roadmap")).andReturn();

        assertThat(res.getResponse().getStatus())
                .as("로드맵이 500 으로 떨어졌다")
                .isEqualTo(200);

        String html = res.getResponse().getContentAsString();
        assertThat(html)
                .as("렌더가 도중에 끊겼다 — 200 이어도 HTML 이 잘리면 화면이 깨진다")
                .contains("</html>");
        assertThat(html)
                .as("서버가 수집한 공고를 화면에 내려보내지 않았다")
                .contains("_serverRoadmap");
    }
}
