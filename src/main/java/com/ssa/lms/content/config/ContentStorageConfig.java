package com.ssa.lms.content.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 콘텐츠(content) 도메인 전용 업로드 저장소 설정 — <b>content 패키지 내부</b>라 공통 {@code WebConfig} 를
 * 건드리지 않는다(공유 파일 충돌 방지, PARALLEL-P2.md). Spring 은 복수의 {@link WebMvcConfigurer} 를 병합한다.
 *
 * <ul>
 *   <li>업로드 파일은 {@code lms.content.upload-dir}(기본 {@code ./uploads/content}) 아래에 저장.
 *       OneDrive/Git 추적 밖 경로라 커밋에 포함되지 않는다.</li>
 *   <li>서비스 경로 {@code /content/files/**} 를 저장 디렉터리로 매핑한다. {@code ResourceHttpRequestHandler}
 *       가 HTTP Range 요청을 지원하므로 VOD 탐색(seek) 재생이 가능하다.</li>
 * </ul>
 */
@Configuration
public class ContentStorageConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(ContentStorageConfig.class);

    /** 서비스 URL 접두어 — Content.fileUrl 이 이 접두어로 시작한다. */
    public static final String URL_PREFIX = "/content/files/";

    private final Path uploadDir;

    public ContentStorageConfig(@Value("${lms.content.upload-dir:./uploads/content}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** 콘텐츠 업로드 루트 디렉터리. 없으면 생성한다. */
    @Bean
    public Path contentUploadDir() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            log.warn("콘텐츠 업로드 디렉터리 생성 실패: {} ({})", uploadDir, e.getMessage());
        }
        return uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(URL_PREFIX + "**")
                .addResourceLocations(uploadDir.toUri().toString());
    }
}
