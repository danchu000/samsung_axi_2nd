/* ============================================================
   brand.js — 브랜드 문자열 단일 출처

   브랜드명이 아직 확정이 아니다("내일의AI"도 바뀔 수 있음).
   그래서 화면 파일에 브랜드 문자열을 절대 직접 쓰지 않는다.
   전부 여기서 읽는다. 확정되면 이 파일만 고치면 전 화면이 따라온다.

   검증법: 아래 name 을 아무 문자열로 바꾸고 전 화면을 돌아본다.
           옛 이름이 남아 있으면 그 화면은 하드코딩된 것이다.
   ============================================================ */

window.BRAND = {
  /* --- 정체 --- */
  name:     "내일의AI",              // 국문 브랜드명 (미확정)
  nameEn:   "Tomorrow's AI",          // 영문
  short:    "내일의AI",               // 좁은 자리용
  tagline:  "공부하는 오늘이 취업하는 내일이 되도록",
  legalName: "(주)내일의AI",          // 사업자명 — 확정 필요
  domain:   "example.com",            // 최종 도메인 미정

  /* --- 연혁 --- */
  foundedYear: 1982,
  heritage: "1982 세종교육",

  /* --- 연락처 (확정 전 자리표시) --- */
  tel:   "1600-0000",
  email: "help@example.com",
  addr:  "주소 확정 예정",

  /* --- 서비스 6종 (공통-001 통합 내비게이션) ---
     current 는 각 화면에서 Shell.gnb({ service: "campus" }) 로 지정한다. */
  services: [
    { key: "home",    label: "통합 홈",       href: "/v2/index.html" },
    { key: "campus",  label: "취업캠퍼스",     href: "/v2/site/campus/index.html" },
    { key: "class",   label: "몰입클라쓰",     href: "/v2/site/class/index.html" },
    { key: "biz",     label: "비즈워크넥트",   href: "/v2/site/biz/index.html" },
    { key: "lxp",     label: "LXP 학습",       href: "/v2/login/index.html" },
    { key: "dorm",    label: "기숙사",         href: "/v2/site/campus/facility.html#dorm" },
    { key: "pool",    label: "수영센터",       href: "/v2/site/campus/facility.html#pool" }
  ],

  /* --- 관리자 권한 3단계 --- */
  adminLevels: [
    { key: "L1", label: "최고관리자", desc: "전체 + 설정·권한·계정·감사로그" },
    { key: "L2", label: "운영관리자", desc: "과정·훈련생·출결·이수·공지·설문·승인" },
    { key: "L3", label: "강사",       desc: "담당 과정의 콘텐츠·채점·감독·튜터링" }
  ]
};

/* 화면에서 문자열을 심는 헬퍼.
   <span data-brand="name"></span> 형태로 쓰면 자동 치환된다. */
window.applyBrand = function applyBrand(root) {
  var scope = root || document;
  scope.querySelectorAll("[data-brand]").forEach(function (el) {
    var key = el.getAttribute("data-brand");
    var val = window.BRAND[key];
    if (typeof val === "string" || typeof val === "number") el.textContent = val;
  });
  // <title> 안의 {brand} 치환
  if (document.title.indexOf("{brand}") !== -1) {
    document.title = document.title.replace(/\{brand\}/g, window.BRAND.name);
  }
};
