package com.ssa.lms.content.service;

public class ContentNotFoundException extends RuntimeException {
    public ContentNotFoundException(Long id) {
        super("콘텐츠를 찾을 수 없습니다: id=" + id);
    }
}
