/**
 * ════════════════════════════════════════════════════════════════════════
 *  [기능 5] 대시보드 차트 공용 렌더러 (관리자·강사)     ★ 실데이터 ★
 * ════════════════════════════════════════════════════════════════════════
 *
 * ■ 데이터 출처 — 여기는 시연용이 아니다
 *   DashboardMetricsService(서버) → window._serverDashboardMetrics → 이 파일.
 *   과정별 진도율·기간 경과율·이수 요건 충족 인원 모두 DB 에서 계산된 실제 값이다.
 *   서버 값이 없으면 **아무것도 그리지 않는다** — 가짜로 채우지 않는다.
 *
 * ■ 관리자와 강사가 같은 그림을 본다
 *   관리자는 전체 과정, 강사는 담당 과정만 — **보는 범위만 다르고 계산은 같다.**
 *   그래서 그리는 코드를 여기 한 벌만 두고 양쪽이 가져다 쓴다.
 *   예전엔 강사 쪽에만 표본 폴백이 남아 있어 같은 과정을 두고 관리자와 강사가
 *   다른 숫자를 봤다. 판단 기준이 갈리면 안 된다.
 *
 * ■ 없는 값을 지어내지 않는다
 *   · 기간 경과율 elapsed < 0 = "기준선을 계산할 수 없음"(시작일==종료일 등).
 *     0 이나 100 으로 그리면 거짓 정보가 된다 → 기준선을 아예 안 그린다.
 *   · 이수 판정 이력이 없으면 빈 목록. 판정 전 인원을 미충족으로 세면
 *     실제보다 나쁘게 보인다.
 *
 * ■ 외부 라이브러리 없음
 *   순수 canvas 로 그린다. 폐쇄망 배포가 예정돼 있어 CDN 의존을 늘리지 않는다.
 *
 * 노출: window.DashCharts = { courseProgress, completion, hbar, weekly, colors }
 */
(function (global) {
    'use strict';

    /*
     * canvas 는 CSS 변수를 직접 못 쓰므로 :root 의 디자인 토큰을 읽어 해석해 둔다.
     * 폴백 값은 css/common-style.css 의 토큰과 같은 값이다 — 여기서 색을 새로 정하지 않는다.
     * (예전엔 이 파일이 자체 팔레트(#10b981 등)를 써서 같은 "정상/주의/위험"이
     *  화면마다 다른 색으로 보였다.)
     */
    function token(name, fallback) {
        try {
            var v = getComputedStyle(document.documentElement).getPropertyValue(name);
            return (v && v.trim()) ? v.trim() : fallback;
        } catch (e) {
            return fallback;
        }
    }

    var INK = '#101828', SUB = '#667085', LINE = '#eef0f6';
    /* 기본은 사이드바와 같은 남색. 색은 "구분"이 아니라 **경고**에만 쓴다. */
    var NAVY = '#131D41', NAVY2 = '#2a3866', NAVY3 = '#5a6a9a';
    var AMBER = token('--color-warning', '#B45309'),
        RED = token('--color-danger', '#C62828'),
        GREEN = token('--color-success', '#2E7D32');

    /**
     * @param id            캔버스 id
     * @param desiredHeight 내용에 맞춘 높이(px). 넘기지 않으면 마크업의 height 속성을 쓴다.
     *
     * 높이를 내용에 맞추는 이유 — 막대가 2개인데 캔버스가 240px 로 고정돼 있으면
     * 행 간격이 100px 넘게 벌어져 "빈 화면"으로 보인다. 행 수에 맞춰 줄인다.
     */
    function setup(id, desiredHeight) {
        var cv = document.getElementById(id);
        if (!cv) return null;
        var dpr = global.devicePixelRatio || 1;
        var w = cv.clientWidth || cv.parentNode.clientWidth || 420;
        // 아래 cv.height 대입이 height 속성을 dpr 배 값으로 덮어쓴다 — 매번 다시 읽으면
        // HiDPI(배율>100%) 화면에서 리사이즈할 때마다 높이가 dpr 배씩 자란다. 원본을 한 번만 저장한다.
        var base = Number(cv.dataset.baseHeight || cv.getAttribute('height')) || 240;
        cv.dataset.baseHeight = base;
        var h = desiredHeight ? Math.round(desiredHeight) : base;
        cv.width = w * dpr;
        cv.height = h * dpr;
        cv.style.height = h + 'px';
        var ctx = cv.getContext('2d');
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ctx.clearRect(0, 0, w, h);
        ctx.font = '12px Pretendard, sans-serif';
        return { ctx: ctx, w: w, h: h };
    }

    /** 캔버스만 먼저 확인한다 — 행 수를 알아야 높이를 정할 수 있어서 setup 전에 쓴다. */
    function exists(id) {
        return !!document.getElementById(id);
    }

    /** 값이 없을 때. 빈 그래프를 그리면 "0"인지 "모름"인지 구분이 안 된다. */
    function empty(s, msg) {
        s.ctx.fillStyle = '#9ca3af';
        s.ctx.textAlign = 'center';
        s.ctx.fillText(msg, s.w / 2, s.h / 2);
    }

    function rrect(ctx, x, y, w, h, r) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, h / 2, w / 2);
        ctx.beginPath();
        ctx.moveTo(x + r, y);
        ctx.arcTo(x + w, y, x + w, y + h, r);
        ctx.arcTo(x + w, y + h, x, y + h, r);
        ctx.arcTo(x, y + h, x, y, r);
        ctx.arcTo(x, y, x + w, y, r);
        ctx.closePath();
        ctx.fill();
    }

    /**
     * 글자 수가 아니라 <b>실제 폭</b>으로 자른다.
     * 글자 수로 자르면 "데이터 분석 실무"(8자)는 살아남고 "데이터 분석 실무 과정"은
     * "데이터 분석 실…" 이 되는 식으로, 정작 자리가 남는데도 이름이 잘렸다.
     */
    function fitText(ctx, s, maxW) {
        s = String(s == null ? '' : s);
        if (ctx.measureText(s).width <= maxW) return s;
        var lo = 0, hi = s.length;
        while (lo < hi) {
            var mid = (lo + hi + 1) >> 1;
            if (ctx.measureText(s.slice(0, mid) + '…').width <= maxW) lo = mid; else hi = mid - 1;
        }
        return lo > 0 ? s.slice(0, lo) + '…' : '…';
    }

    /**
     * 라벨 칸(좌측 여백)의 폭을 실제 라벨에 맞춰 정한다.
     * 고정값(108px)이면 짧은 이름일 땐 여백이 남고 긴 이름은 잘렸다.
     */
    function labelGutter(ctx, labels, minW, maxW) {
        var widest = 0;
        for (var i = 0; i < labels.length; i++) {
            var t = ctx.measureText(String(labels[i] == null ? '' : labels[i])).width;
            if (t > widest) widest = t;
        }
        return Math.round(Math.max(minW, Math.min(maxW, widest + 16)));
    }

    global.DashCharts = {

        /**
         * 과정별 진행 현황 — 진도 막대 + 기간 경과 기준선.
         * 둘을 겹쳐 그려야 "일정보다 뒤처진 과정"이 보인다. 숫자만으로는 격차가 안 읽힌다.
         * rows: [{ name, progress, elapsed }]
         */
        courseProgress: function (id, rows, emptyMsg) {
            if (!exists(id)) return;
            rows = rows || [];
            if (!rows.length) {
                var se = setup(id); if (!se) return;
                empty(se, emptyMsg || '표시할 과정이 없습니다'); return;
            }

            // 행 수에 맞춘 높이 — 막대 2개에 240px 를 쓰면 가운데가 텅 빈다
            var ROW = 34, TOP = 12, BOT = 10;
            var s = setup(id, Math.min(TOP + rows.length * ROW + BOT, 420)); if (!s) return;

            var ctx = s.ctx, padR = 46, top = TOP;
            var padL = labelGutter(ctx, rows.map(function (r) { return r.name; }), 74, s.w * 0.38);
            var barH = 14, gap = (s.h - top - BOT) / rows.length;

            rows.forEach(function (r, i) {
                var y = top + i * gap, full = s.w - padL - padR;

                ctx.fillStyle = INK; ctx.textAlign = 'right';
                ctx.fillText(fitText(ctx, r.name, padL - 12), padL - 10, y + barH - 1);

                ctx.fillStyle = '#f3f4f6';
                rrect(ctx, padL, y, full, barH, 7);

                // 서버는 날짜가 없는 과정에 elapsed = -1 을 준다 — 없는 기준선을 그리면 거짓말이 된다
                var hasElapsed = r.elapsed != null && r.elapsed >= 0;
                var late = hasElapsed && r.progress < r.elapsed - 5;
                ctx.fillStyle = late ? AMBER : NAVY;
                rrect(ctx, padL, y, full * Math.min(r.progress, 100) / 100, barH, 7);

                if (hasElapsed) {
                    var ex = padL + full * Math.min(r.elapsed, 100) / 100;
                    ctx.strokeStyle = SUB; ctx.lineWidth = 2;
                    ctx.beginPath(); ctx.moveTo(ex, y - 3); ctx.lineTo(ex, y + barH + 3); ctx.stroke();
                }

                ctx.textAlign = 'left';
                ctx.fillStyle = late ? '#b45309' : INK;
                ctx.fillText(r.progress + '%', s.w - padR + 6, y + barH - 1);
            });
        },

        /**
         * 이수 요건 충족 현황 — 요건별 충족 인원.
         * rows: [{ label, done, total }]
         */
        completion: function (id, rows, emptyMsg) {
            var s = setup(id); if (!s) return;
            rows = rows || [];
            if (!rows.length) { empty(s, emptyMsg || '이수 판정 이력이 없습니다'); return; }

            var ctx = s.ctx, padL = 34, padR = 12, padT = 16, padB = 34;
            var gw = s.w - padL - padR, gh = s.h - padT - padB;
            var max = Math.max(1, Math.max.apply(null, rows.map(function (r) { return r.total; })));
            var slot = gw / rows.length, bw = Math.min(52, slot * 0.5);

            for (var i = 0; i <= 4; i++) {
                var y = padT + gh * i / 4;
                ctx.strokeStyle = LINE; ctx.lineWidth = 1;
                ctx.beginPath(); ctx.moveTo(padL, y); ctx.lineTo(padL + gw, y); ctx.stroke();
                ctx.fillStyle = SUB; ctx.textAlign = 'right';
                ctx.fillText(Math.round(max * (1 - i / 4)) + '', padL - 6, y + 4);
            }

            rows.forEach(function (r, i) {
                var x = padL + slot * i + (slot - bw) / 2;
                ctx.fillStyle = '#f3f4f6';
                rrect(ctx, x, padT, bw, gh, 6);

                var hh = gh * r.done / max;
                var rate = r.total ? r.done / r.total : 0;
                ctx.fillStyle = rate >= 0.75 ? GREEN : (rate >= 0.5 ? AMBER : RED);
                rrect(ctx, x, padT + gh - hh, bw, Math.max(hh, 3), 6);

                ctx.textAlign = 'center'; ctx.fillStyle = INK;
                ctx.fillText(r.done + '/' + r.total, x + bw / 2, padT + gh - hh - 6);
                ctx.fillStyle = SUB;
                // 라벨 칸은 막대 슬롯 폭 — 글자 수가 아니라 실제 폭 기준으로 자른다
                ctx.fillText(fitText(ctx, r.label, slot - 6), x + bw / 2, s.h - 12);
            });
        },

        /** 가로 막대. rows: [{ label, value }] (0~100) */
        hbar: function (id, rows, emptyMsg) {
            if (!exists(id)) return;
            rows = rows || [];
            if (!rows.length) {
                var se = setup(id); if (!se) return;
                empty(se, emptyMsg || '데이터가 없습니다'); return;
            }

            var ROW = 30, TOP = 8, BOT = 6;
            var s = setup(id, Math.min(TOP + rows.length * ROW + BOT, 420)); if (!s) return;

            var ctx = s.ctx, padR = 44, top = TOP;
            var padL = labelGutter(ctx, rows.map(function (r) { return r.label; }), 64, s.w * 0.38);
            var barH = 12, gap = (s.h - top - BOT) / rows.length;

            rows.forEach(function (r, i) {
                var y = top + i * gap, full = s.w - padL - padR;
                ctx.fillStyle = INK; ctx.textAlign = 'right';
                ctx.fillText(fitText(ctx, r.label, padL - 12), padL - 10, y + barH - 1);

                ctx.fillStyle = '#f3f4f6';
                rrect(ctx, padL, y, full, barH, 6);
                ctx.fillStyle = NAVY;
                rrect(ctx, padL, y, full * Math.min(r.value, 100) / 100, barH, 6);

                ctx.textAlign = 'left'; ctx.fillStyle = SUB;
                ctx.fillText(r.value + '%', s.w - padR + 6, y + barH - 1);
            });
        },

        /** 추이 — 선 여러 개. series: [{ label, color, values }] */
        weekly: function (id, labels, series, emptyMsg) {
            var s = setup(id); if (!s) return;
            if (!labels || labels.length < 2) { empty(s, emptyMsg || '추이 데이터가 부족합니다'); return; }

            var ctx = s.ctx, padL = 34, padR = 12, padT = 26, padB = 26;
            var gw = s.w - padL - padR, gh = s.h - padT - padB;

            for (var i = 0; i <= 4; i++) {
                var y = padT + gh * i / 4;
                ctx.strokeStyle = LINE; ctx.lineWidth = 1;
                ctx.beginPath(); ctx.moveTo(padL, y); ctx.lineTo(padL + gw, y); ctx.stroke();
                ctx.fillStyle = SUB; ctx.textAlign = 'right';
                ctx.fillText((100 - i * 25) + '', padL - 6, y + 4);
            }

            ctx.textAlign = 'center';
            labels.forEach(function (l, i) {
                ctx.fillStyle = SUB;
                ctx.fillText(l, padL + gw * i / (labels.length - 1), s.h - 8);
            });

            series.forEach(function (se) {
                ctx.strokeStyle = se.color; ctx.lineWidth = 2.5;
                ctx.beginPath();
                se.values.forEach(function (v, i) {
                    var x = padL + gw * i / (se.values.length - 1);
                    var y = padT + gh * (1 - v / 100);
                    i ? ctx.lineTo(x, y) : ctx.moveTo(x, y);
                });
                ctx.stroke();

                ctx.fillStyle = se.color;
                se.values.forEach(function (v, i) {
                    var x = padL + gw * i / (se.values.length - 1);
                    var y = padT + gh * (1 - v / 100);
                    ctx.beginPath(); ctx.arc(x, y, 3, 0, Math.PI * 2); ctx.fill();
                });
            });

            var lx = padL;
            ctx.textAlign = 'left';
            series.forEach(function (se) {
                ctx.fillStyle = se.color;
                ctx.fillRect(lx, 8, 10, 4);
                ctx.fillStyle = SUB;
                ctx.fillText(se.label, lx + 15, 13);
                lx += ctx.measureText(se.label).width + 42;
            });
        },

        colors: { navy: NAVY, navy2: NAVY2, navy3: NAVY3, amber: AMBER, red: RED, green: GREEN }
    };
})(window);
