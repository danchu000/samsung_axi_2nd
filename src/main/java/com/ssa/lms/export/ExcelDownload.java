package com.ssa.lms.export;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * xlsx 다운로드 응답을 만드는 곳 — 4개 화면이 같은 헤더를 쓰게 한 곳에 모아 둔다.
 *
 * <p><b>한글 파일명</b>: {@code filename="..."} 만 쓰면 브라우저마다 인코딩 해석이 달라 깨진다.
 * 기존 성적 CSV 구현이 쓰던 RFC 5987 방식({@code filename*=UTF-8''<percent-encoded>})을 그대로 따르고,
 * 그 확장을 모르는 옛 클라이언트를 위해 ASCII 로 접은 {@code filename=} 을 함께 남긴다.
 * {@code URLEncoder} 는 공백을 {@code +} 로 바꾸는데 RFC 5987 에서는 {@code %20} 이어야 해서
 * 한 번 더 치환한다 (CSV 구현과 동일).</p>
 *
 * <p>CSV 와 달리 xlsx 에는 <b>UTF-8 BOM 을 붙이지 않는다</b>. xlsx 는 텍스트가 아니라 zip
 * 컨테이너(시그니처 {@code PK})이고, 앞에 BOM 3바이트가 붙으면 엑셀이 파일을 아예 못 연다.</p>
 */
public final class ExcelDownload {

    /** xlsx 의 MIME 타입. */
    public static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    /** 윈도우/맥 파일명에 못 쓰는 문자. 설문명·시험명이 그대로 파일명이 되므로 반드시 건다. */
    private static final String ILLEGAL = "\\/:*?\"<>|";

    private static final int MAX_NAME_CHARS = 80;

    private ExcelDownload() {
    }

    /**
     * @param baseName 확장자 없는 파일명. 한글 허용 (예: {@code "성적_1차 중간고사"})
     * @param body     {@link ExcelWriter#toBytes()} 결과
     */
    public static ResponseEntity<byte[]> attachment(String baseName, byte[] body) {
        String fileName = sanitize(baseName) + ".xlsx";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + asciiFallback(fileName) + "\"; "
                                + "filename*=UTF-8''" + encoded)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")   // 성적·설문 결과는 민감하다
                .contentType(XLSX)
                .contentLength(body.length)
                .body(body);
    }

    private static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "download";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length() && sb.length() < MAX_NAME_CHARS; i++) {
            char c = raw.charAt(i);
            if (c < 0x20 || ILLEGAL.indexOf(c) >= 0) {
                sb.append('_');
            } else {
                sb.append(c);
            }
        }
        String name = sb.toString().strip();
        return name.isEmpty() ? "download" : name;
    }

    /**
     * RFC 5987 을 모르는 클라이언트가 읽을 이름.
     *
     * <p>이름이 거의 다 한글이라 ASCII 만 남기면 껍데기가 된다("성적_[채점검증] 자동채점 전용 시험"
     * → {@code _[].xlsx}). 확장자를 뺀 <b>본문에</b> 알파벳/숫자가 없으면 통째로
     * {@code download.xlsx} 로 내린다 — 의미 없는 기호 나열보다 낫다.</p>
     */
    private static String asciiFallback(String fileName) {
        String stem = fileName.endsWith(".xlsx")
                ? fileName.substring(0, fileName.length() - ".xlsx".length())
                : fileName;

        StringBuilder sb = new StringBuilder(stem.length());
        boolean meaningful = false;
        for (int i = 0; i < stem.length(); i++) {
            char c = stem.charAt(i);
            if (c > 0x20 && c < 0x7F && c != '"' && c != '\\') {
                sb.append(c);
                if (Character.isLetterOrDigit(c)) {
                    meaningful = true;
                }
            }
        }
        return meaningful ? sb + ".xlsx" : "download.xlsx";
    }
}
