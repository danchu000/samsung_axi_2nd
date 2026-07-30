package com.ssa.lms.user.service;

/**
 * 기본 프로필 아바타 SVG 생성 유틸.
 *
 * <p>프로필 사진을 올리지 않은 계정에 이름 이니셜 + 사용자별 색상 조합의 SVG 를 보여준다.
 * 색상은 userId 로 결정되므로 같은 사용자는 항상 같은 아바타가 나온다
 * (local 시드 정적 아바타 {@code /static/img/avatars/*.svg} 와 동일한 스타일).</p>
 */
public final class DefaultAvatars {

    /** 그라데이션 팔레트 (위 색 → 아래 색). userId % 12 로 선택. */
    private static final String[][] GRADIENTS = {
            {"#3B82F6", "#1D4ED8"},   // 파랑
            {"#10B981", "#047857"},   // 초록
            {"#F59E0B", "#B45309"},   // 호박
            {"#EF4444", "#B91C1C"},   // 빨강
            {"#8B5CF6", "#6D28D9"},   // 보라
            {"#EC4899", "#BE185D"},   // 분홍
            {"#14B8A6", "#0F766E"},   // 청록
            {"#F97316", "#C2410C"},   // 주황
            {"#6366F1", "#4338CA"},   // 남색
            {"#84CC16", "#4D7C0F"},   // 연두
            {"#06B6D4", "#0E7490"},   // 하늘
            {"#A855F7", "#7E22CE"},   // 자주
    };

    private DefaultAvatars() {
    }

    /** DB(profile_image_url)에 저장할 아바타 URL. */
    public static String urlFor(Long userId) {
        return "/avatar/" + userId + ".svg";
    }

    /** 이름 이니셜 + userId 색상으로 아바타 SVG 문서를 만든다. */
    public static String render(Long userId, String name) {
        long id = userId != null ? userId : 0L;
        String[] g = GRADIENTS[(int) Math.floorMod(id, GRADIENTS.length)];
        String initial = escapeXml(initialOf(name));
        return """
                <svg xmlns="http://www.w3.org/2000/svg" width="240" height="240" viewBox="0 0 240 240" role="img" aria-label="profile">
                  <defs>
                    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0" stop-color="%s"/>
                      <stop offset="1" stop-color="%s"/>
                    </linearGradient>
                  </defs>
                  <rect width="240" height="240" fill="url(#bg)"/>
                  <circle cx="120" cy="120" r="82" fill="#ffffff" fill-opacity="0.14"/>
                  <text x="120" y="120" text-anchor="middle" dominant-baseline="central"
                        font-family="'Malgun Gothic','Apple SD Gothic Neo','Noto Sans KR',sans-serif"
                        font-size="112" font-weight="700" fill="#ffffff">%s</text>
                </svg>
                """.formatted(g[0], g[1], initial);
    }

    /** 이름 첫 글자(코드포인트 단위 — 한글/영문 모두 안전). 이름이 없으면 "?". */
    private static String initialOf(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String trimmed = name.strip();
        return new String(Character.toChars(trimmed.codePointAt(0)));
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
