package com.ssa.lms.content.service;

/**
 * 업로드 파일 저장 결과 — 서비스 접근 URL 과 원본 메타.
 *
 * @param fileUrl          서비스 경로 (Content.fileUrl 에 저장)
 * @param originalFileName 업로드 당시 원본 파일명
 * @param size             바이트 크기
 * @param mimeType         MIME 타입
 */
public record StoredFile(String fileUrl, String originalFileName, long size, String mimeType) {
}
