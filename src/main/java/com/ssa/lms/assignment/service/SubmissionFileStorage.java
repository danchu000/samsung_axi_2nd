package com.ssa.lms.assignment.service;

import com.ssa.lms.assignment.entity.SubmissionFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 과제 첨부파일 저장소.
 *
 * <p><b>원본 파일명을 경로로 쓰지 않는다.</b> 훈련생이 올리는 파일명에는 이름·생년월일이
 * 섞여 들어오는 일이 잦고, 경로에 그대로 쓰면 웹에서 유추 가능한 주소가 된다.
 * 저장은 무의미한 UUID 경로(storedPath)로 하고, 원본명(originalName)은 DB 에만 두었다가
 * 다운로드 응답의 Content-Disposition 에서만 되살린다.</p>
 *
 * <p>저장 위치는 <b>웹 루트(static/) 밖</b>이다. 정적 리소스 매핑으로 노출되면
 * 인증 없이 남의 제출물을 받아갈 수 있다.</p>
 *
 * <p>multipart 500MB 설정은 application.yml 에 이미 있다 (공동 소유 파일이라 건드리지 않았다).
 * 저장 루트는 {@code lms.upload.assignment-dir} 로 덮어쓸 수 있고, 기본값은 홈 디렉터리 밑이다.</p>
 */
@Slf4j
@Component
public class SubmissionFileStorage {

    private static final DateTimeFormatter DIR_FMT = DateTimeFormatter.ofPattern("yyyy/MM");

    private final Path root;

    public SubmissionFileStorage(
            @Value("${lms.upload.assignment-dir:#{systemProperties['user.home']}/.samsung-lxp-uploads/assignment}")
            String dir) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
    }

    /**
     * 저장 후 파일 메타를 만들어 돌려준다 (아직 Submission 에 붙지 않은 상태).
     * 빈 파일은 무시한다 — 화면이 빈 input 을 함께 보내는 경우가 있다.
     */
    public SubmissionFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String originalName = sanitize(file.getOriginalFilename());
        String stored = UUID.randomUUID().toString().replace("-", "") + extensionOf(originalName);
        Path dir = root.resolve(LocalDate.now().format(DIR_FMT));
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(stored);
            file.transferTo(target);
            return SubmissionFile.builder()
                    .originalName(originalName)
                    .storedPath(root.relativize(target).toString())
                    .sizeBytes(file.getSize())
                    .contentType(file.getContentType())
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException("과제 첨부파일 저장에 실패했습니다: " + originalName, e);
        }
    }

    /** 다운로드용 실제 경로. storedPath 는 루트 상대 경로다. */
    public Path resolve(SubmissionFile file) {
        Path resolved = root.resolve(file.getStoredPath()).normalize();
        if (!resolved.startsWith(root)) {
            // storedPath 가 조작됐을 때 루트 밖으로 나가지 못하게 막는다.
            throw new IllegalArgumentException("잘못된 첨부파일 경로입니다.");
        }
        return resolved;
    }

    /* ===== 내부 ===== */

    /** 경로 구분자와 상위 디렉터리 표기를 제거한다 (파일명은 표시용으로만 쓰인다). */
    private static String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "unnamed";
        }
        String cleaned = name.replace("\\", "/");
        cleaned = cleaned.substring(cleaned.lastIndexOf('/') + 1).replace("..", "_");
        return cleaned.length() > 200 ? cleaned.substring(cleaned.length() - 200) : cleaned;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return (dot < 0 || dot == name.length() - 1) ? "" : name.substring(dot).toLowerCase();
    }
}
