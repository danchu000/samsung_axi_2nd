package com.ssa.lms.ai.service;

import com.ssa.lms.content.config.ContentStorageConfig;
import com.ssa.lms.content.entity.Content;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 학습 자료 본문 추출 — [기능 3] AI 학습 도우미가 <b>제목이 아니라 내용</b>으로 답하게 한다.
 *
 * <p>지금까지는 자료의 제목·설명만 모델에 넘겼다. "3주차 트랜잭션"이라는 제목만 보고
 * 답하니 "영상 12분쯤 나온 그거"는 답할 수 없었다. 이제 PDF 본문을 읽어 넘긴다.</p>
 *
 * <p><b>추출 결과를 캐시한다.</b> 같은 PDF 를 질문마다 다시 여는 것은 낭비다.
 * 16MB PDF 를 매 질문마다 파싱하면 응답이 눈에 띄게 느려진다.
 * 캐시 키는 파일 URL 이라 파일이 교체되면 URL 도 바뀌어 자연히 갱신된다.</p>
 *
 * <p><b>못 읽어도 예외를 던지지 않는다.</b> 자료 하나가 깨졌다고 질문 전체가 실패하면 안 된다.
 * 못 읽은 자료는 제목·설명만으로 넘어간다.</p>
 *
 * <p>영상 자막은 다루지 않는다 — STT 가 필요해 별도 작업이다.</p>
 */
@Service
public class ContentTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(ContentTextExtractor.class);

    /**
     * 자료 하나에서 뽑을 글자 수 상한.
     * 한 자료가 프롬프트를 다 먹으면 다른 자료가 밀려난다. 앞부분이 대개 핵심이다.
     */
    private static final int MAX_CHARS_PER_FILE = 12_000;

    /** 이보다 큰 파일은 열지 않는다. 파싱에 시간과 메모리가 지나치게 든다. */
    private static final long MAX_FILE_BYTES = 60L * 1024 * 1024;

    /** 캐시 상한. 자료가 아주 많은 과정에서 메모리를 무한정 쓰지 않게 한다. */
    private static final int MAX_CACHE_ENTRIES = 300;

    private final Path uploadDir;

    /** 파일 URL → 추출된 본문. 빈 문자열은 "읽어봤지만 없음"이라 다시 시도하지 않는다. */
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public ContentTextExtractor(@Value("${lms.content.upload-dir:./uploads/content}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * 자료의 본문. 읽을 수 없으면 빈 문자열.
     *
     * <p>PDF 만 읽는다. 영상·이미지는 본문이 없고, 그 사실을 호출부가 알 필요는 없다 —
     * 빈 문자열을 받으면 제목·설명만 쓰면 된다.</p>
     */
    public String textOf(Content content) {
        String url = content.getFileUrl();
        if (url == null || url.isBlank()) return "";
        if (!isPdf(content)) return "";

        String cached = cache.get(url);
        if (cached != null) return cached;

        String text = extract(url);

        // 캐시가 너무 커지면 비운다. LRU 까지 갈 규모가 아니라 단순하게 둔다
        if (cache.size() >= MAX_CACHE_ENTRIES) cache.clear();
        cache.put(url, text);
        return text;
    }

    private boolean isPdf(Content content) {
        String name = content.getOriginalFileName();
        String url = content.getFileUrl();
        return (name != null && name.toLowerCase().endsWith(".pdf"))
                || (url != null && url.toLowerCase().endsWith(".pdf"));
    }

    private String extract(String fileUrl) {
        Path file = resolve(fileUrl);
        if (file == null) return "";

        try {
            if (!Files.isReadable(file)) {
                log.debug("[AI] 본문 추출 건너뜀 — 읽을 수 없는 파일 {}", fileUrl);
                return "";
            }
            long size = Files.size(file);
            if (size > MAX_FILE_BYTES) {
                log.info("[AI] 본문 추출 건너뜀 — 파일이 너무 큼 ({}MB) {}", size / 1024 / 1024, fileUrl);
                return "";
            }

            try (PDDocument doc = PDDocument.load(file.toFile())) {
                if (doc.isEncrypted()) {
                    log.info("[AI] 본문 추출 건너뜀 — 암호화된 PDF {}", fileUrl);
                    return "";
                }
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String raw = stripper.getText(doc);
                return normalize(raw);
            }

        } catch (Exception e) {
            // 자료 하나가 깨졌다고 질문 전체가 실패하면 안 된다
            log.warn("[AI] 본문 추출 실패 — 제목·설명만 사용한다 file={} cause={}",
                    fileUrl, e.getClass().getSimpleName());
            return "";
        }
    }

    /**
     * 서비스 URL → 실제 파일 경로.
     *
     * <p><b>업로드 폴더 밖으로 못 나가게 막는다.</b> URL 에 {@code ../} 가 섞여 오면
     * 서버의 아무 파일이나 읽혀 모델 답변으로 새어 나갈 수 있다.</p>
     */
    private Path resolve(String fileUrl) {
        if (!fileUrl.startsWith(ContentStorageConfig.URL_PREFIX)) return null;
        String relative = fileUrl.substring(ContentStorageConfig.URL_PREFIX.length());
        Path target = uploadDir.resolve(relative).normalize();
        if (!target.startsWith(uploadDir)) {
            log.warn("[AI] 업로드 폴더 밖 경로 요청을 막았다: {}", fileUrl);
            return null;
        }
        return target;
    }

    /**
     * PDF 에서 뽑은 글자는 줄바꿈·공백이 지저분하다.
     * 그대로 넘기면 의미 없는 공백이 토큰을 잡아먹는다.
     */
    private String normalize(String raw) {
        if (raw == null) return "";
        String t = raw.replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                      .replaceAll("\\n{3,}", "\n\n")
                      .trim();
        return t.length() > MAX_CHARS_PER_FILE ? t.substring(0, MAX_CHARS_PER_FILE) : t;
    }

    /** 파일이 바뀌었을 때 캐시를 비운다. 관리자 화면에서 호출할 수 있게 열어 둔다. */
    public void evictAll() {
        cache.clear();
    }
}
