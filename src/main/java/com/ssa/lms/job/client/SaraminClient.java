package com.ssa.lms.job.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssa.lms.job.config.SaraminProperties;
import com.ssa.lms.job.entity.JobPosting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 사람인 채용정보 오픈 API 호출. [기능 1] 직무 로드맵 수집기의 바깥쪽.
 *
 * <p>GET {@code /job-search?access-key=…&keywords=…&count=…}
 * 응답은 {@code {"jobs":{"job":[…]}}} 모양이다.</p>
 *
 * <p><b>이 클래스가 지키는 것</b>
 * <ul>
 *   <li><b>예외를 밖으로 던지지 않는다</b> — 외부 API 가 죽어도 배치가 멈추면 안 된다.
 *       실패하면 빈 목록을 주고, 남아 있는 지난 회차 데이터로 화면은 계속 뜬다</li>
 *   <li><b>액세스 키를 로그에 남기지 않는다</b></li>
 *   <li><b>응답 필드가 없어도 죽지 않는다</b> — 외부 스펙은 예고 없이 바뀐다.
 *       {@code JsonNode.path()} 는 없는 필드에 대해 missing 노드를 준다</li>
 * </ul>
 */
@Component
public class SaraminClient {

    private static final Logger log = LoggerFactory.getLogger(SaraminClient.class);

    private final SaraminProperties props;
    private final RestClient rest;

    public SaraminClient(SaraminProperties props, RestClient.Builder builder) {
        this.props = props;
        this.rest = builder.baseUrl(props.getBaseUrl()).build();
    }

    /**
     * 한 직무 그룹의 공고를 가져온다.
     *
     * @return 파싱된 공고. 실패하거나 결과가 없으면 빈 목록 (예외를 던지지 않는다)
     */
    public List<JobPosting> search(String jobGroup, String keywords, LocalDate collectedAt) {
        if (!props.isUsable()) return List.of();

        try {
            JsonNode res = rest.get()
                    .uri(uri -> uri.path("/job-search")
                            .queryParam("access-key", props.getAccessKey())
                            .queryParam("keywords", keywords)
                            .queryParam("count", props.getCountPerGroup())
                            .queryParam("sr", "directhire")   // 헤드헌팅·파견 제외
                            .queryParam("fields", "posting-date,expiration-date")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            return parse(res, jobGroup, collectedAt);

        } catch (Exception e) {
            // 예외 메시지에 요청 URL(=키 포함)이 섞여 나올 수 있어 클래스명만 남긴다
            log.error("[공고수집] 사람인 호출 실패 group={} cause={}",
                    jobGroup, e.getClass().getSimpleName());
            return List.of();
        }
    }

    private List<JobPosting> parse(JsonNode res, String jobGroup, LocalDate collectedAt) {
        List<JobPosting> out = new ArrayList<>();
        if (res == null) return out;

        JsonNode jobs = res.path("jobs").path("job");
        if (!jobs.isArray()) return out;

        for (JsonNode j : jobs) {
            String externalId = text(j.path("id"));
            if (externalId.isBlank()) continue;   // id 가 없으면 중복을 막을 수 없다

            JsonNode position = j.path("position");
            out.add(JobPosting.builder()
                    .externalId(externalId)
                    .jobGroup(jobGroup)
                    .companyName(cut(orDash(text(j.path("company").path("detail").path("name"))), 200))
                    .title(cut(orDash(text(position.path("title"))), 300))
                    .url(cut(text(j.path("url")), 500))
                    .experienceLevel(cut(text(position.path("experience-level").path("name")), 100))
                    .location(cut(text(position.path("location").path("name")), 200))
                    .keywords(cut(text(j.path("keyword")), 1000))
                    .postingDate(date(j.path("posting-date")))
                    .expirationDate(date(j.path("expiration-date")))
                    .collectedAt(collectedAt)
                    .build());
        }
        return out;
    }

    private String text(JsonNode n) {
        return n == null || n.isMissingNode() || n.isNull() ? "" : n.asText("").trim();
    }

    /** 회사명·제목은 not null 이다. 비어 있으면 저장이 통째로 깨지므로 표시만 대체한다. */
    private String orDash(String s) {
        return s.isBlank() ? "-" : s;
    }

    private String cut(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    /** 사람인은 RFC 1123 계열 문자열을 준다. 못 읽으면 null — 날짜 하나 때문에 공고를 버리지 않는다. */
    private LocalDate date(JsonNode n) {
        String v = text(n);
        if (v.isBlank()) return null;
        try {
            return OffsetDateTime.parse(v, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toLocalDate();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(v.substring(0, Math.min(10, v.length())));
            } catch (Exception ignored2) {
                return null;
            }
        }
    }
}
