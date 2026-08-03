package com.ssa.lms.job.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssa.lms.ai.client.AiAnswer;
import com.ssa.lms.ai.client.AiClient;
import com.ssa.lms.ai.client.AiRequest;
import com.ssa.lms.job.config.JobProperties;
import com.ssa.lms.job.entity.JobPosting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Claude 웹 검색으로 채용공고를 수집한다. [기능 1] 보조 수집처.
 *
 * <p><b>왜 필요한가</b><br>
 * 워크넷은 즉시 발급되지만 <b>기술 키워드 필드를 주지 않는다.</b> 사람인은 주지만
 * 기업회원 승인이 필요해 바로 못 쓴다. 그래서 "Docker 68%" 같은 통계를 낼 재료가
 * 지금은 없다. 이 수집처가 그 빈칸을 메운다.</p>
 *
 * <p><b>검색만으로는 부족하다 — 원문을 열어야 한다</b><br>
 * 기술 스택은 공고의 <b>우대사항</b>에 몰려 있고(자격요건에는 학력·전공 이야기가 많다),
 * 그 본문은 검색 결과 요약문에 안 나온다. 그래서 {@code web_search} 로 공고를 찾고
 * {@code web_fetch} 로 원문을 실제로 열어 읽게 한다. 비용의 대부분이 이 단계다.</p>
 *
 * <p><b>지어내지 못하게 막은 것</b>
 * <ul>
 *   <li><b>url 이 없으면 버린다</b> — 화면은 원문 링크를 근거로 보여준다.
 *       확인할 수 없는 공고는 근거가 아니라 그냥 그럴듯한 문장이다</li>
 *   <li><b>원문을 못 연 공고는 기술을 비워 둔다</b> — 추측으로 채우면 통계가 통째로 거짓이 된다.
 *       대신 {@code bodyFetched=false} 로 그 사실을 남긴다</li>
 *   <li><b>출처를 남긴다</b> — 공개 API 로 받은 공고와 섞이므로,
 *       나중에 링크가 깨졌을 때 어디를 의심할지 알 수 있어야 한다</li>
 * </ul>
 *
 * <p><b>예외를 밖으로 던지지 않는다</b> — 다른 수집처와 같은 규칙이다.
 * 모델이 죽어도 배치가 멈추면 안 되고, 지난 회차 데이터로 화면은 계속 뜬다.</p>
 */
@Component
public class ClaudeSearchSource implements JobPostingSource {

    private static final Logger log = LoggerFactory.getLogger(ClaudeSearchSource.class);

    /** 사용량 기록에 남을 이름. 어느 기능이 비용을 쓰는지 갈라 보려면 필요하다. */
    private static final String PURPOSE = "JOB_COLLECT";

    /** 공고 8건 × 필드 여러 개면 이 정도는 있어야 JSON 이 중간에 잘리지 않는다. */
    private static final int MAX_OUTPUT_TOKENS = 8000;

    /** 한 직무에 검색을 몇 번까지 허용할지. 없으면 모델이 무한정 검색해 요금이 는다. */
    private static final int MAX_SEARCHES = 5;

    /** 페이지 하나에서 읽어올 최대 분량. 공고 본문은 이 정도면 충분하고, 넘기면 토큰만 먹는다. */
    private static final int MAX_PAGE_TOKENS = 8000;

    private final JobProperties props;
    private final AiClient aiClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public ClaudeSearchSource(JobProperties props, AiClient aiClient) {
        this.props = props;
        this.aiClient = aiClient;
    }

    @Override
    public String name() {
        return "AI 웹검색";
    }

    /**
     * 설정이 켜져 있고 <b>AI 키까지 준비됐을 때만</b> 쓴다.
     *
     * <p>설정만 켜고 키가 없으면 여기서 조용히 빠지는데, 그 상태는
     * 관리자 대시보드의 AI 배너가 "API 키가 등록되지 않았습니다"로 알려준다.</p>
     */
    @Override
    public boolean usable() {
        return props.hasClaudeSearch() && aiClient.available();
    }

    @Override
    public List<JobPosting> search(String jobGroup, String keywords, LocalDate collectedAt) {
        if (!usable()) return List.of();

        // 화이트리스트에 없는 직무는 모델을 부르지도 않는다 — 검색 한 번이 곧 비용이다
        if (!props.claudeGroupIds().contains(jobGroup)) {
            return List.of();
        }

        try {
            AiAnswer answer = aiClient.ask(AiRequest.of(PURPOSE)
                    .system(systemPrompt())
                    .user(userPrompt(jobGroup, keywords))
                    .maxOutputTokens(MAX_OUTPUT_TOKENS)
                    .tools(tools())
                    .build());

            if (!answer.ok()) {
                // 사유는 AiUsageLog 에 남아 관리자 배너로 올라간다. 여기서 또 떠들지 않는다
                log.warn("[공고수집] AI 검색 실패 group={} reason={}", jobGroup, answer.reason());
                return List.of();
            }
            return parse(answer.text(), jobGroup, collectedAt);

        } catch (Exception e) {
            // 한 직무의 실패가 나머지 수집을 막으면 안 된다
            log.error("[공고수집] AI 검색 오류 group={} cause={}",
                    jobGroup, e.getClass().getSimpleName());
            return List.of();
        }
    }

    /* ===================== 프롬프트 ===================== */

    private String systemPrompt() {
        return """
               당신은 채용공고 수집기다. 웹에서 실제 공고를 찾아 구조화된 JSON 으로만 답한다.

               [작업 순서]
               1. web_search 로 해당 직무의 최근 채용공고를 찾는다.
               2. web_fetch 로 각 공고 원문을 열어 '자격요건'과 '우대사항'을 직접 읽는다.
                  검색 결과 요약문에는 이 내용이 없다. 반드시 원문을 열어 확인한다.
               3. 실제로 읽은 내용만으로 JSON 을 만든다.

               [반드시 지킬 것]
               - url 은 검색·페치로 실제 확인한 주소만 쓴다. 기억이나 추측으로 주소를 만들지 않는다.
               - 원문을 열지 못한 공고는 bodyFetched 를 false 로 두고 두 skills 배열을 비운다.
                 추측해서 채우지 않는다. 비어 있는 편이 틀린 것보다 낫다.
               - requiredSkills 에는 '자격요건'에 적힌 기술만, preferredSkills 에는 '우대사항'에
                 적힌 기술만 담는다. 같은 기술이 양쪽에 있으면 requiredSkills 에만 넣는다.
               - 학력·전공·경력 연차는 skills 에 넣지 않는다. education / majorRestricted /
                 experience 로 따로 담는다.
               - 기술명은 영문 공식 표기로 통일한다 (Spring Boot, Kubernetes, PostgreSQL).
                 한글 표기(스프링부트)나 임의 축약(k8s)을 쓰지 않는다. 배열당 최대 8개.
               - education 은 다음 중 하나만: 학력무관 / 고졸 이상 / 초대졸 이상 / 대졸 이상.
                 공고에 표기가 없으면 null. 모르는 것을 채우지 않는다.
               - 채용 플랫폼의 목록 페이지가 아니라 개별 공고를 대상으로 한다.

               [출력 형식]
               JSON 객체 하나만 출력한다. 설명·머리말·코드펜스를 붙이지 않는다.
               {
                 "postings": [
                   {
                     "company": "회사명",
                     "title": "공고 제목",
                     "url": "https://...",
                     "location": "서울 강남구",
                     "experience": "신입 | 경력 1~3년 | 경력 3년 이상 | 경력무관",
                     "postedAt": "2026-07-28",
                     "education": "대졸 이상",
                     "majorRestricted": false,
                     "requiredSkills": ["Java", "Spring Boot"],
                     "preferredSkills": ["Kafka", "Kubernetes", "AWS"],
                     "bodyFetched": true
                   }
                 ]
               }
               """;
    }

    private String userPrompt(String jobGroup, String keywords) {
        return """
               직무: %s
               검색 키워드: %s
               찾을 공고 수: 최대 %d건

               조건: 한국 채용공고, 최근 3개월 이내에 올라온 것.
               위에서 정한 JSON 형식으로만 답하라.
               """.formatted(props.nameOf(jobGroup), keywords, props.getClaudeCount());
    }

    /**
     * 서버 도구 정의 — 검색과 페치를 함께 준다.
     *
     * <p>{@code max_uses} 를 반드시 건다. 상한이 없으면 모델이 만족할 때까지 검색해
     * 주 1회 배치 한 번의 비용이 예측 불가능해진다.</p>
     */
    private List<Map<String, Object>> tools() {
        Map<String, Object> search = new HashMap<>();
        search.put("type", "web_search_20260209");
        search.put("name", "web_search");
        search.put("max_uses", MAX_SEARCHES);

        Map<String, Object> fetch = new HashMap<>();
        fetch.put("type", "web_fetch_20260209");
        fetch.put("name", "web_fetch");
        // 목표 건수보다 넉넉히 — 열어 봤더니 마감된 공고인 경우가 있다
        fetch.put("max_uses", props.getClaudeCount() * 2);
        fetch.put("max_content_tokens", MAX_PAGE_TOKENS);

        List<String> allowed = props.claudeAllowedDomainList();
        if (!allowed.isEmpty()) {
            // 설정으로 좁혔으면 검색·페치 양쪽에 똑같이 건다. 한쪽만 걸면 새어 나간다
            search.put("allowed_domains", allowed);
            fetch.put("allowed_domains", allowed);
        }
        return List.of(search, fetch);
    }

    /* ===================== 파싱 ===================== */

    /**
     * 모델 답변에서 JSON 을 꺼내 공고로 만든다.
     *
     * <p>형식을 아무리 못박아도 모델은 가끔 코드펜스나 한 줄 설명을 붙인다.
     * 그것 때문에 한 주치 수집을 통째로 버리는 건 아깝다 — 바깥쪽 중괄호를 찾아 잘라 쓴다.</p>
     */
    private List<JobPosting> parse(String text, String jobGroup, LocalDate collectedAt) {
        String json = extractJsonObject(text);
        if (json == null) {
            log.warn("[공고수집] AI 응답에서 JSON 을 찾지 못했다 group={}", jobGroup);
            return List.of();
        }

        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (Exception e) {
            log.warn("[공고수집] AI 응답 JSON 파싱 실패 group={}", jobGroup);
            return List.of();
        }

        JsonNode postings = root.path("postings");
        if (!postings.isArray()) return List.of();

        List<JobPosting> out = new ArrayList<>();
        int droppedNoUrl = 0;
        int notFetched = 0;

        for (JsonNode p : postings) {
            String url = text(p.path("url"));
            if (url.isBlank() || !url.startsWith("http")) {
                // 링크가 없으면 확인할 방법이 없다. 확인 못 하는 공고는 근거가 아니다
                droppedNoUrl++;
                continue;
            }

            boolean fetched = p.path("bodyFetched").asBoolean(false);
            if (!fetched) notFetched++;

            List<String> required = fetched ? skills(p.path("requiredSkills")) : List.of();
            List<String> preferred = fetched ? skills(p.path("preferredSkills")) : List.of();

            out.add(JobPosting.builder()
                    .externalId(externalIdOf(url))
                    .jobGroup(jobGroup)
                    .companyName(cut(orDash(text(p.path("company"))), 200))
                    .title(cut(orDash(text(p.path("title"))), 300))
                    .url(cut(url, 500))
                    .experienceLevel(cut(nullIfBlank(text(p.path("experience"))), 100))
                    .location(cut(nullIfBlank(text(p.path("location"))), 200))
                    // 집계는 keywords 한 칸을 본다. 화면을 안 건드리려고 둘을 합쳐 넣는다
                    .keywords(cut(nullIfBlank(merge(required, preferred)), 1000))
                    .requiredSkills(cut(nullIfBlank(String.join(", ", required)), 1000))
                    .preferredSkills(cut(nullIfBlank(String.join(", ", preferred)), 1000))
                    .education(cut(nullIfBlank(text(p.path("education"))), 30))
                    .majorRestricted(bool(p.path("majorRestricted")))
                    .collectedBy(name())
                    .bodyFetched(fetched)
                    .postingDate(date(p.path("postedAt")))
                    .collectedAt(collectedAt)
                    .build());
        }

        // 표본이 왜 적은지는 이 로그로만 알 수 있다. 조용히 줄어들면 원인을 못 찾는다
        log.info("[공고수집] AI 검색 {} — 받음 {}건 / 링크없어 버림 {}건 / 원문 못 읽음 {}건",
                jobGroup, out.size(), droppedNoUrl, notFetched);
        return out;
    }

    /** 코드펜스·머리말이 붙어도 바깥쪽 객체만 잘라 쓴다. */
    private String extractJsonObject(String text) {
        if (text == null) return null;
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return (start < 0 || end <= start) ? null : text.substring(start, end + 1);
    }

    /**
     * 중복을 막을 유일 키.
     *
     * <p>AI 수집에는 사람인 같은 공고 id 가 없다. url 이 그 역할을 한다 —
     * 매주 같은 공고가 다시 잡히는데 그대로 쌓으면 "68%가 요구" 가 통째로 거짓이 된다.</p>
     */
    private String externalIdOf(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(url.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("ai-");
            for (int i = 0; i < 16; i++) {         // 32자 — 컬럼(40)에 들어간다
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 쓸 수 없다", e);   // JDK 표준이라 사실상 안 난다
        }
    }

    /** 필수 우선, 그다음 우대. 중복은 한 번만. 집계가 같은 기술을 두 번 세면 비율이 부푼다. */
    private String merge(List<String> required, List<String> preferred) {
        List<String> all = new ArrayList<>(required);
        for (String s : preferred) {
            if (!all.contains(s)) all.add(s);
        }
        return String.join(", ", all);
    }

    /** 기술명 배열. 빈 값·중복을 걷어내고 최대 8개까지만 — 나열이 긴 공고 하나가 순위를 흔든다. */
    private List<String> skills(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<String> out = new ArrayList<>();
        for (JsonNode n : node) {
            String s = text(n);
            if (!s.isBlank() && !out.contains(s)) out.add(s);
            if (out.size() >= 8) break;
        }
        return out;
    }

    private String text(JsonNode n) {
        return n == null || n.isMissingNode() || n.isNull() ? "" : n.asText("").trim();
    }

    /** 모르면 null 로 둔다 — false 로 채우면 "전공 무관" 비율이 통째로 부풀려진다. */
    private Boolean bool(JsonNode n) {
        return (n == null || n.isMissingNode() || n.isNull() || !n.isBoolean()) ? null : n.asBoolean();
    }

    /** 회사명·제목은 not null 이다. 비어 있으면 저장이 깨지므로 표시만 대체한다. */
    private String orDash(String s) {
        return s.isBlank() ? "-" : s;
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private String cut(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    /** 못 읽으면 null — 날짜 하나 때문에 공고를 버리지 않는다. */
    private LocalDate date(JsonNode n) {
        String v = text(n);
        if (v.isBlank()) return null;
        try {
            return LocalDate.parse(v.substring(0, Math.min(10, v.length())));
        } catch (DateTimeParseException | IndexOutOfBoundsException ignored) {
            return null;
        }
    }
}
