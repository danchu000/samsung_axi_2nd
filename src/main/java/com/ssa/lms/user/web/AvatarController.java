package com.ssa.lms.user.web;

import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import com.ssa.lms.user.service.DefaultAvatars;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 기본 프로필 아바타 — 사진을 올리지 않은 계정용 동적 SVG.
 *
 * <p>{@code profile_image_url} 에 {@code /avatar/{id}.svg} 를 저장해 두면
 * 이 엔드포인트가 이름 이니셜 + 사용자별 색상 SVG 를 렌더한다
 * ({@link DefaultAvatars} 참고). 탈퇴(soft delete) 계정은 조회에서 걸러지므로
 * 회색 "?" 아바타로 대체된다.</p>
 */
@RestController
@RequiredArgsConstructor
public class AvatarController {

    private final UserRepository userRepository;

    @GetMapping(value = "/avatar/{userId}.svg", produces = "image/svg+xml")
    public ResponseEntity<byte[]> avatar(@PathVariable Long userId) {
        String name = userRepository.findById(userId).map(User::getName).orElse("?");
        // 한글 이니셜이 깨지지 않도록 UTF-8 바이트 + charset 명시로 응답한다
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/svg+xml;charset=UTF-8"))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
                .body(DefaultAvatars.render(userId, name).getBytes(StandardCharsets.UTF_8));
    }
}
