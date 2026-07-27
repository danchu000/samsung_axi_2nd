package com.ssa.lms.content.service;

/** 업로드 파일 저장/삭제 실패. */
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileStorageException(String message) {
        super(message);
    }
}
