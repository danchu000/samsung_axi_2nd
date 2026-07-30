package com.ssa.lms.user;

import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import com.ssa.lms.user.service.DefaultAvatars;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 프로필 이미지가 없는 기존 계정에 기본 아바타(/avatar/{id}.svg)를 백필한다.
 *
 * <p>가입 화면에 사진 업로드가 없어 {@code profile_image_url} 이 null 인 계정은
 * 전 화면에서 동일한 샘플 사진으로 폴백되던 문제의 보정. 신규 가입은
 * {@code UserService.signup} 이 지정하므로, 이 러너는 그 이전에 만들어진 계정만 채운다.</p>
 *
 * <p>모든 프로필에서 실행 — 시드 러너들(@Order 0~100)이 만든 계정까지 포함하도록
 * 마지막(@Order(500))에 돈다. null 만 채우므로 멱등이고, 이미 지정된 아바타/업로드
 * 사진은 건드리지 않는다.</p>
 */
@Slf4j
@Component
@Order(500)
@RequiredArgsConstructor
public class ProfileImageBackfillRunner implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<User> missing = userRepository.findByProfileImageUrlIsNull();
        if (missing.isEmpty()) {
            return;
        }
        missing.forEach(u -> u.assignProfileImage(DefaultAvatars.urlFor(u.getId())));
        log.info("[avatar] 프로필 이미지 없는 계정 {}건에 기본 아바타 백필", missing.size());
    }
}
