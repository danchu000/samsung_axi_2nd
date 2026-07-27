package com.ssa.lms.content.service;

import com.ssa.lms.content.config.ContentStorageConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 업로드 파일을 로컬 저장소(디스크)에 보관하고 서비스 URL 을 돌려준다.
 * 저장 경로: {@code <uploadDir>/<yyyy>/<uuid>.<ext>}, 서비스 URL: {@code /content/files/<yyyy>/<uuid>.<ext>}.
 *
 * <p>파일명은 UUID 로 무작위화해 원본명 노출·경로 조작(path traversal)을 막는다. 원본명은 메타로 보관한다.</p>
 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path contentUploadDir;

    /**
     * 업로드 파일 저장.
     *
     * @param file 비어있지 않은 MultipartFile
     * @return 저장 결과(서비스 URL/원본명/크기/MIME)
     */
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("업로드할 파일이 비어 있습니다.");
        }
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String ext = StringUtils.getFilenameExtension(original);
        String stored = UUID.randomUUID().toString().replace("-", "")
                + (ext != null && !ext.isBlank() ? "." + ext.toLowerCase() : "");

        String yearDir = String.valueOf(LocalDate.now().getYear());
        try {
            Path dir = contentUploadDir.resolve(yearDir).normalize();
            if (!dir.startsWith(contentUploadDir)) {
                throw new FileStorageException("잘못된 저장 경로입니다.");
            }
            Files.createDirectories(dir);
            Path target = dir.resolve(stored);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String url = ContentStorageConfig.URL_PREFIX + yearDir + "/" + stored;
            log.debug("콘텐츠 파일 저장: {} ({} bytes)", url, file.getSize());
            return new StoredFile(url, original, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new FileStorageException("파일 저장에 실패했습니다: " + original, e);
        }
    }

    /**
     * 서비스 URL 로 저장된 실제 파일을 삭제한다(파일 교체/콘텐츠 삭제 시). 실패해도 예외를 던지지 않는다.
     */
    public void deleteByUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(ContentStorageConfig.URL_PREFIX)) {
            return;
        }
        String relative = fileUrl.substring(ContentStorageConfig.URL_PREFIX.length());
        try {
            Path target = contentUploadDir.resolve(relative).normalize();
            if (target.startsWith(contentUploadDir)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            log.warn("콘텐츠 파일 삭제 실패: {} ({})", fileUrl, e.getMessage());
        }
    }
}
