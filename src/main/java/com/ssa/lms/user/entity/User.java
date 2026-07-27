package com.ssa.lms.user.entity;

import com.ssa.lms.common.converter.CryptoConverter;
import com.ssa.lms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 공통 사용자 엔티티 (관리자/강사/훈련생 통합).
 *
 * <ul>
 *   <li>비밀번호: bcrypt 해시로만 저장 (SecurityConfig의 PasswordEncoder)</li>
 *   <li>개인정보 컬럼(email, phone, birthDate)은 AES-256 암호화 — 내역서 증빙 요건.
 *       암호문이 저장되므로 해당 컬럼으로는 DB 검색 불가. 검색은 loginId/name 사용.</li>
 *   <li>soft delete: 삭제 시 is_deleted 마킹만 (훈련 정보 3년 보존 요건)</li>
 * </ul>
 *
 * 스키마 변경은 공통 엔티티 규칙에 따라 A·B 합의 후 진행 (CLAUDE.md).
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_login_id", columnNames = "login_id")
})
@SQLDelete(sql = "UPDATE users SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;

    /** bcrypt 해시 */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    /* ===== 개인정보 (AES-256 암호화 저장) ===== */

    @Convert(converter = CryptoConverter.class)
    @Column(length = 512)
    private String email;

    @Convert(converter = CryptoConverter.class)
    @Column(length = 512)
    private String phone;

    /** yyyy-MM-dd 문자열로 암호화 저장 */
    @Convert(converter = CryptoConverter.class)
    @Column(name = "birth_date", length = 512)
    private String birthDate;

    /* ===== 부가 정보 ===== */

    @Column(length = 10)
    private String gender;

    @Column(length = 100)
    private String education;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    /* ===== 동의/이력 (내역서: 개인정보 수집·제3자 제공 동의) ===== */

    @Column(name = "privacy_consent_at")
    private LocalDateTime privacyConsentAt;

    @Column(name = "third_party_consent_at")
    private LocalDateTime thirdPartyConsentAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder
    private User(String loginId, String password, String name, Role role, UserStatus status,
                 String email, String phone, String birthDate, String gender, String education,
                 String profileImageUrl, LocalDateTime privacyConsentAt, LocalDateTime thirdPartyConsentAt) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.role = role;
        this.status = status != null ? status : UserStatus.PENDING;
        this.email = email;
        this.phone = phone;
        this.birthDate = birthDate;
        this.gender = gender;
        this.education = education;
        this.profileImageUrl = profileImageUrl;
        this.privacyConsentAt = privacyConsentAt;
        this.thirdPartyConsentAt = thirdPartyConsentAt;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeStatus(UserStatus status) {
        this.status = status;
    }

    public void updateProfile(String name, String email, String phone, String birthDate,
                              String gender, String education) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.birthDate = birthDate;
        this.gender = gender;
        this.education = education;
    }

    public void recordLogin(LocalDateTime at) {
        this.lastLoginAt = at;
    }
}
