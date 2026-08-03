package com.ssa.lms.job;

import com.ssa.lms.ai.client.AiAnswer;
import com.ssa.lms.ai.client.AiClient;
import com.ssa.lms.ai.client.AiRequest;
import com.ssa.lms.job.client.ClaudeSearchSource;
import com.ssa.lms.job.config.JobProperties;
import com.ssa.lms.job.entity.JobPosting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * [기능 1] AI 웹 검색 수집기.
 *
 * <p><b>모델을 실제로 부르지 않는다.</b> 테스트가 API 를 때리면 돈이 나가고 결과도 매번
 * 달라져 실패를 믿을 수 없게 된다 ({@code AiQnaServiceTest} 와 같은 원칙).</p>
 *
 * <p>여기서 막으려는 것은 <b>지어낸 공고가 통계로 올라가는 것</b>이다. 화면은 이 데이터를
 * 근거로 "공고 68%에 등장"이라고 말하고 원문 링크를 함께 보여준다. 링크가 죽어 있거나
 * 읽지도 않은 기술이 섞이면, 화면은 멀쩡한데 내용이 거짓이 된다.</p>
 */
class ClaudeSearchSourceTest {

    private JobProperties props;
    private AiClient aiClient;
    private ClaudeSearchSource source;

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 3);

    @BeforeEach
    void setUp() {
        props = new JobProperties();
        props.setEnabled(true);
        props.setClaudeSearch(true);
        props.setClaudeGroups("backend");
        props.setClaudeCount(8);

        JobProperties.Group backend = new JobProperties.Group();
        backend.setName("백엔드 개발자");
        backend.setKeywords("백엔드 자바 스프링");
        props.setGroups(Map.of("backend", backend));

        aiClient = mock(AiClient.class);
        when(aiClient.available()).thenReturn(true);

        source = new ClaudeSearchSource(props, aiClient);
    }

    private void modelReturns(String text) {
        when(aiClient.ask(any(AiRequest.class))).thenReturn(AiAnswer.success(text, 100, 200));
    }

    private List<JobPosting> collect() {
        return source.search("backend", "백엔드 자바 스프링", TODAY);
    }

    /* ===================== 사용 가능 여부 ===================== */

    @Test
    @DisplayName("설정을 켜도 AI 키가 없으면 쓰지 않는다")
    void notUsableWithoutAiKey() {
        when(aiClient.available()).thenReturn(false);
        assertThat(source.usable()).isFalse();
    }

    @Test
    @DisplayName("직무를 지정하지 않으면 켜도 아무것도 하지 않는다 — 비용이 직무 수에 비례한다")
    void noGroupsMeansOff() {
        props.setClaudeGroups("");
        assertThat(source.usable()).isFalse();
    }

    @Test
    @DisplayName("화이트리스트에 없는 직무는 모델을 부르지도 않는다")
    void skipsGroupsOutsideWhitelist() {
        modelReturns("{\"postings\":[]}");

        List<JobPosting> got = source.search("security", "정보보안", TODAY);

        assertThat(got).isEmpty();
        // 부르고 나서 버리면 돈은 이미 나간 뒤다
        verify(aiClient, never()).ask(any());
    }

    /* ===================== 파싱 ===================== */

    @Test
    @DisplayName("자격요건과 우대사항을 나눠 담고, 집계용 keywords 에는 합쳐 넣는다")
    void splitsRequiredAndPreferred() {
        modelReturns("""
                {"postings":[{
                  "company":"테스트컴퍼니","title":"백엔드 개발자",
                  "url":"https://example.com/jobs/1",
                  "location":"서울 강남구","experience":"경력 3년 이상","postedAt":"2026-07-28",
                  "education":"대졸 이상","majorRestricted":false,
                  "requiredSkills":["Java","Spring Boot"],
                  "preferredSkills":["Kafka","Kubernetes"],
                  "bodyFetched":true
                }]}
                """);

        JobPosting p = collect().get(0);

        assertThat(p.getRequiredSkills()).isEqualTo("Java, Spring Boot");
        assertThat(p.getPreferredSkills()).isEqualTo("Kafka, Kubernetes");
        // 화면(RoadmapService)은 keywords 한 칸만 본다. 둘을 합쳐야 집계가 된다
        assertThat(p.keywordList())
                .containsExactly("Java", "Spring Boot", "Kafka", "Kubernetes");
        assertThat(p.getEducation()).isEqualTo("대졸 이상");
        assertThat(p.getMajorRestricted()).isFalse();
        assertThat(p.getCollectedBy()).isEqualTo("AI 웹검색");
        assertThat(p.getPostingDate()).isEqualTo(LocalDate.of(2026, 7, 28));
        assertThat(p.getCollectedAt()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("링크 없는 공고는 버린다 — 확인할 수 없으면 근거가 아니다")
    void dropsPostingsWithoutUrl() {
        modelReturns("""
                {"postings":[
                  {"company":"실체없음","title":"백엔드","url":"","bodyFetched":true,
                   "requiredSkills":["Java"],"preferredSkills":[]},
                  {"company":"진짜회사","title":"백엔드","url":"https://example.com/jobs/2",
                   "bodyFetched":true,"requiredSkills":["Java"],"preferredSkills":[]}
                ]}
                """);

        List<JobPosting> got = collect();

        assertThat(got).hasSize(1);
        assertThat(got.get(0).getCompanyName()).isEqualTo("진짜회사");
    }

    @Test
    @DisplayName("원문을 못 읽은 공고는 기술을 비운다 — 추측으로 채우면 통계가 거짓이 된다")
    void unreadPostingKeepsNoSkills() {
        modelReturns("""
                {"postings":[{
                  "company":"열지못함","title":"백엔드","url":"https://example.com/jobs/3",
                  "bodyFetched":false,
                  "requiredSkills":["Java","Spring"],"preferredSkills":["Redis"]
                }]}
                """);

        JobPosting p = collect().get(0);

        // 모델이 채워 보내도 믿지 않는다. 본문을 안 읽었으면 근거가 없다
        assertThat(p.getRequiredSkills()).isNull();
        assertThat(p.getPreferredSkills()).isNull();
        assertThat(p.keywordList()).isEmpty();
        // 버리지는 않는다 — 왜 표본이 적은지 나중에 알 수 있어야 한다
        assertThat(p.getBodyFetched()).isFalse();
    }

    @Test
    @DisplayName("전공 제한을 모르면 null 로 둔다 — false 로 채우면 '전공 무관' 비율이 부푼다")
    void unknownMajorStaysNull() {
        modelReturns("""
                {"postings":[{
                  "company":"회사","title":"백엔드","url":"https://example.com/jobs/4",
                  "bodyFetched":true,"requiredSkills":["Java"],"preferredSkills":[]
                }]}
                """);

        assertThat(collect().get(0).getMajorRestricted()).isNull();
    }

    @Test
    @DisplayName("같은 공고는 매주 같은 id 를 갖는다 — 중복이 쌓이면 비율이 통째로 거짓이 된다")
    void sameUrlYieldsSameExternalId() {
        String json = """
                {"postings":[{
                  "company":"회사","title":"백엔드","url":"https://example.com/jobs/5",
                  "bodyFetched":true,"requiredSkills":["Java"],"preferredSkills":[]
                }]}
                """;
        modelReturns(json);

        String first = collect().get(0).getExternalId();
        String second = source.search("backend", "백엔드", TODAY.plusWeeks(1)).get(0).getExternalId();

        assertThat(first).isEqualTo(second);
        assertThat(first).startsWith("ai-").hasSizeLessThanOrEqualTo(40);   // 컬럼 길이 40
    }

    @Test
    @DisplayName("모델이 코드펜스나 머리말을 붙여도 한 주치를 통째로 버리지 않는다")
    void toleratesCodeFence() {
        modelReturns("""
                찾은 결과입니다.
                ```json
                {"postings":[{
                  "company":"회사","title":"백엔드","url":"https://example.com/jobs/6",
                  "bodyFetched":true,"requiredSkills":["Java"],"preferredSkills":[]
                }]}
                ```
                """);

        assertThat(collect()).hasSize(1);
    }

    /* ===================== 실패해도 배치는 계속된다 ===================== */

    @Test
    @DisplayName("모델 호출이 실패해도 예외를 던지지 않는다 — 다른 직무 수집이 멈추면 안 된다")
    void modelFailureReturnsEmpty() {
        when(aiClient.ask(any(AiRequest.class)))
                .thenReturn(AiAnswer.failure("CREDIT_EXHAUSTED", "중단됐어요"));

        assertThat(collect()).isEmpty();
    }

    @Test
    @DisplayName("JSON 이 아닌 답이 와도 죽지 않는다")
    void garbageResponseReturnsEmpty() {
        modelReturns("죄송합니다. 공고를 찾지 못했습니다.");

        assertThat(collect()).isEmpty();
    }

    /* ===================== 비용 안전장치 ===================== */

    @Test
    @DisplayName("검색·페치 횟수에 상한을 걸어 보낸다 — 없으면 배치 한 번의 비용을 예측할 수 없다")
    void sendsToolUsageCaps() {
        modelReturns("{\"postings\":[]}");

        collect();

        ArgumentCaptor<AiRequest> captor = ArgumentCaptor.forClass(AiRequest.class);
        verify(aiClient).ask(captor.capture());
        AiRequest sent = captor.getValue();

        assertThat(sent.hasTools()).isTrue();
        assertThat(sent.tools()).allSatisfy(t ->
                assertThat(t).containsKey("max_uses"));
        // 검색만으로는 우대사항 본문을 못 읽는다. 페치가 반드시 함께 가야 한다
        assertThat(sent.tools()).extracting(t -> t.get("name"))
                .containsExactlyInAnyOrder("web_search", "web_fetch");
        // 사용량 기록에서 다른 기능과 갈라 보려면 purpose 가 달라야 한다
        assertThat(sent.purpose()).isEqualTo("JOB_COLLECT");
    }

    @Test
    @DisplayName("도메인을 좁히면 검색과 페치 양쪽에 건다 — 한쪽만 걸면 새어 나간다")
    void domainRestrictionAppliesToBothTools() {
        props.setClaudeAllowedDomains("work.go.kr, example.com");
        modelReturns("{\"postings\":[]}");

        collect();

        ArgumentCaptor<AiRequest> captor = ArgumentCaptor.forClass(AiRequest.class);
        verify(aiClient).ask(captor.capture());

        assertThat(captor.getValue().tools()).allSatisfy(t ->
                assertThat(t.get("allowed_domains"))
                        .isEqualTo(List.of("work.go.kr", "example.com")));
    }
}
