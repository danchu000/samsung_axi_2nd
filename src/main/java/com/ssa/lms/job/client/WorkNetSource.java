package com.ssa.lms.job.client;

import com.ssa.lms.job.config.JobProperties;
import com.ssa.lms.job.entity.JobPosting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 워크넷(고용노동부) 채용정보 수집. [기능 1] 기본 수집처.
 *
 * <p><b>왜 여기가 기본인가</b>
 * <ul>
 *   <li>공공데이터포털에서 <b>키가 즉시 발급</b>된다 — 기업회원 가입도, 승인 대기도 없다</li>
 *   <li>무료이고 공공 데이터라 <b>정부 제출 사업에서 출처를 밝히기 좋다</b></li>
 *   <li>채용 사이트를 직접 크롤링하지 않아도 된다 — 크롤링은 대부분 이용약관 위반이다</li>
 * </ul>
 *
 * <p><b>응답은 XML 이다.</b> JDK 내장 DOM 파서를 쓴다 — 폐쇄망 배포라 의존성을 늘리지 않는다.
 * XXE(외부 엔티티 주입)를 막는 설정을 반드시 켠다. 외부에서 받은 XML 을 그냥 파싱하면
 * 서버 파일을 읽히거나 내부망으로 요청이 나갈 수 있다.</p>
 *
 * <p><b>필드 이름이 스펙과 다를 수 있다.</b> 그래서 후보 이름을 여러 개 두고 찾는다.
 * 그래도 못 찾으면 <b>"응답은 왔는데 0건 파싱"</b>을 경고로 남긴다 —
 * 조용히 "공고 없음"으로 처리하면 매핑이 틀린 것을 몇 주 동안 모른다.</p>
 */
@Component
public class WorkNetSource implements JobPostingSource {

    private static final Logger log = LoggerFactory.getLogger(WorkNetSource.class);

    /** 목록 조회 경로. callTp=L 이 목록, D 가 상세다. */
    private static final String PATH = "/opi/opi/opia/wantedApi.do";

    /* 워크넷 응답의 필드 이름 후보. 앞에서부터 찾아 처음 있는 것을 쓴다. */
    private static final String[] ITEM_TAGS = {"wanted", "item", "dhsOpenEmpInfo"};
    private static final String[] ID_TAGS = {"wantedAuthNo", "empSeqno", "wantedInfoNo", "id"};
    private static final String[] COMPANY_TAGS = {"company", "empBusiNm", "coNm", "companyName"};
    private static final String[] TITLE_TAGS = {"title", "empWantedTitle", "wantedTitle", "recrutPbancTtl"};
    private static final String[] URL_TAGS = {"wantedInfoUrl", "empWantedHomepgDetail", "url", "infoUrl"};
    private static final String[] CAREER_TAGS = {"career", "empWantedCareerNm", "careerNm"};
    private static final String[] REGION_TAGS = {"region", "empWantedRegionNm", "workRgnNmLst"};
    private static final String[] START_TAGS = {"regDt", "empWantedStdt", "startDt", "pbancBgngYmd"};
    private static final String[] END_TAGS = {"closeDt", "empWantedEndt", "endDt", "pbancEndYmd"};
    /** 요구 역량의 원재료. 워크넷은 별도 키워드 필드가 없어 직종·자격 필드를 함께 쓴다. */
    private static final String[] KEYWORD_TAGS = {"jobsCd", "jobsNm", "empWantedTypeNm", "holidayTpNm", "smodifyDtm"};

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JobProperties props;
    private final RestClient rest;

    public WorkNetSource(JobProperties props, RestClient.Builder builder) {
        this.props = props;
        this.rest = builder.baseUrl(props.getWorknetBaseUrl()).build();
    }

    @Override
    public String name() {
        return "워크넷";
    }

    @Override
    public boolean usable() {
        return props.hasWorknet();
    }

    @Override
    public List<JobPosting> search(String jobGroup, String keywords, LocalDate collectedAt) {
        if (!usable()) return List.of();

        try {
            String xml = rest.get()
                    .uri(uri -> uri.path(PATH)
                            .queryParam("authKey", props.getWorknetKey())
                            .queryParam("callTp", "L")
                            .queryParam("returnType", "XML")
                            .queryParam("startPage", 1)
                            .queryParam("display", props.getCountPerGroup())
                            .queryParam("keyword", keywords)
                            .build())
                    .retrieve()
                    .body(String.class);

            return parse(xml, jobGroup, collectedAt);

        } catch (Exception e) {
            // 예외 메시지에 요청 URL(=키 포함)이 섞일 수 있어 클래스명만 남긴다
            log.error("[공고수집] 워크넷 호출 실패 group={} cause={}",
                    jobGroup, e.getClass().getSimpleName());
            return List.of();
        }
    }

    private List<JobPosting> parse(String xml, String jobGroup, LocalDate collectedAt) {
        List<JobPosting> out = new ArrayList<>();
        if (xml == null || xml.isBlank()) return out;

        Document doc;
        try {
            doc = builder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            log.error("[공고수집] 워크넷 XML 파싱 실패 group={} cause={}",
                    jobGroup, e.getClass().getSimpleName());
            return out;
        }

        NodeList items = firstNodeList(doc);
        for (int i = 0; i < items.getLength(); i++) {
            if (!(items.item(i) instanceof Element el)) continue;

            String id = text(el, ID_TAGS);
            if (id.isBlank()) continue;   // id 가 없으면 중복을 막을 수 없다

            out.add(JobPosting.builder()
                    .externalId("wn-" + cut(id, 36))
                    .jobGroup(jobGroup)
                    .companyName(cut(orDash(text(el, COMPANY_TAGS)), 200))
                    .title(cut(orDash(text(el, TITLE_TAGS)), 300))
                    .url(cut(text(el, URL_TAGS), 500))
                    .experienceLevel(cut(text(el, CAREER_TAGS), 100))
                    .location(cut(text(el, REGION_TAGS), 200))
                    .keywords(cut(keywordsOf(el), 1000))
                    .postingDate(date(text(el, START_TAGS)))
                    .expirationDate(date(text(el, END_TAGS)))
                    .collectedAt(collectedAt)
                    .build());
        }

        /*
         * 응답은 왔는데 한 건도 못 만들었다면 필드 이름이 스펙과 다른 것이다.
         * "공고 없음"으로 넘기면 몇 주 동안 원인을 못 찾는다. 응답 앞부분을 같이 남겨
         * 어떤 태그를 쓰는지 바로 확인할 수 있게 한다(키는 응답 본문에 없다).
         */
        if (out.isEmpty() && xml.length() > 200) {
            log.warn("[공고수집] 워크넷 응답은 받았으나 0건 파싱 — 필드 매핑 확인 필요. 응답 앞부분: {}",
                    xml.substring(0, Math.min(400, xml.length())).replaceAll("\\s+", " "));
        }
        return out;
    }

    /** 항목을 감싼 태그 이름이 스펙 버전마다 달라 후보를 차례로 찾는다. */
    private NodeList firstNodeList(Document doc) {
        for (String tag : ITEM_TAGS) {
            NodeList list = doc.getElementsByTagName(tag);
            if (list.getLength() > 0) return list;
        }
        return doc.getElementsByTagName("__none__");
    }

    /**
     * 요구 역량 집계의 원재료.
     *
     * <p>워크넷은 사람인과 달리 <b>기술 키워드 필드가 없다.</b> 직종명 등으로 대신하는데,
     * 그래서 이 출처만 쓰면 "Docker 68%" 같은 세부 기술 통계는 나오지 않는다.
     * 세부 기술까지 보려면 사람인 키를 함께 넣어야 한다 — 화면에서 감추지 말고 그대로 둔다.</p>
     */
    private String keywordsOf(Element el) {
        List<String> parts = new ArrayList<>();
        for (String tag : new String[]{"jobsNm", "empWantedTypeNm"}) {
            String v = text(el, new String[]{tag});
            if (!v.isBlank()) parts.add(v);
        }
        return String.join(",", parts);
    }

    /** XXE 차단 — 외부에서 받은 XML 을 그냥 파싱하면 서버 파일이 읽히거나 내부망 요청이 나간다. */
    private DocumentBuilder builder() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        return f.newDocumentBuilder();
    }

    private String text(Element parent, String[] tags) {
        for (String tag : tags) {
            NodeList list = parent.getElementsByTagName(tag);
            if (list.getLength() > 0) {
                Node n = list.item(0);
                String v = n.getTextContent();
                if (v != null && !v.isBlank()) return v.trim();
            }
        }
        return "";
    }

    /** 회사명·제목은 not null 이다. 비면 저장이 통째로 깨지므로 표시만 대체한다. */
    private String orDash(String s) {
        return s.isBlank() ? "-" : s;
    }

    private String cut(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    /** yyyyMMdd 또는 yyyy-MM-dd. 못 읽으면 null — 날짜 하나 때문에 공고를 버리지 않는다. */
    private LocalDate date(String v) {
        if (v == null || v.isBlank()) return null;
        String t = v.trim().replace("-", "").replace(".", "");
        try {
            return t.length() >= 8 ? LocalDate.parse(t.substring(0, 8), YMD) : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
