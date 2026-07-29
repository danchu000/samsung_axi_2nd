/**
 * 대시보드 차트 공용 렌더러 (관리자·강사).
 *
 * 관리자는 전체 과정, 강사는 담당 과정만 — **보는 범위만 다르고 그림은 같다.**
 * 그래서 그리는 코드를 여기 한 벌만 두고 양쪽이 가져다 쓴다.
 *
 * 외부 라이브러리를 쓰지 않는다(순수 canvas). 폐쇄망 배포가 예정돼 있어
 * CDN 의존을 늘리지 않는 편이 안전하다.
 *
 * 노출: window.DashCharts = { courseProgress, completion, hbar, weekly }
 */
(function (global) {
    'use strict';

    var INK = '#101828', SUB = '#667085', LINE = '#eef0f6';
    /* 기본은 사이드바와 같은 남색. 색은 "구분"이 아니라 **경고**에만 쓴다. */
    var NAVY = '#131D41', NAVY2 = '#2a3866', NAVY3 = '#5a6a9a';
    var AMBER = '#f59e0b', RED = '#ef4444', GREEN = '#10b981';

    function setup(id) {
        var cv = document.getElementById(id);
        if (!cv) return null;
        var dpr = global.devicePixelRatio || 1;
        var w = cv.clientWidth || cv.parentNode.clientWidth || 420;
        var h = Number(cv.getAttribute('height')) || 240;
        cv.width = w * dpr;
        cv.height = h * dpr;
        cv.style.height = h + 'px';
        var ctx = cv.getContext('2d');
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ctx.clearRect(0, 0, w, h);
        ctx.font = '12px Pretendard, sans-serif';
        return { ctx: ctx, w: w, h: h };
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

    function cut(s, n) {
        s = String(s == null ? '' : s);
        return s.length > n ? s.slice(0, n - 1) + '…' : s;
    }

    global.DashCharts = {

        /**
         * 과정별 진행 현황 — 진도 막대 + 기간 경과 기준선.
         * 둘을 겹쳐 그려야 "일정보다 뒤처진 과정"이 보인다. 숫자만으로는 격차가 안 읽힌다.
         * rows: [{ name, progress, elapsed }]
         */
        courseProgress: function (id, rows, emptyMsg) {
            var s = setup(id); if (!s) return;
            rows = rows || [];
            if (!rows.length) { empty(s, emptyMsg || '표시할 과정이 없습니다'); return; }

            var ctx = s.ctx, padL = 108, padR = 46, top = 12;
            var barH = 14, gap = (s.h - top - 10) / rows.length;

            rows.forEach(function (r, i) {
                var y = top + i * gap, full = s.w - padL - padR;

                ctx.fillStyle = INK; ctx.textAlign = 'right';
                ctx.fillText(cut(r.name, 9), padL - 10, y + barH - 1);

                ctx.fillStyle = '#f3f4f6';
                rrect(ctx, padL, y, full, barH, 7);

                var late = r.elapsed != null && r.progress < r.elapsed - 5;
                ctx.fillStyle = late ? AMBER : NAVY;
                rrect(ctx, padL, y, full * Math.min(r.progress, 100) / 100, barH, 7);

                if (r.elapsed != null) {
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
                ctx.fillText(cut(r.label, 7), x + bw / 2, s.h - 12);
            });
        },

        /** 가로 막대. rows: [{ label, value }] (0~100) */
        hbar: function (id, rows, emptyMsg) {
            var s = setup(id); if (!s) return;
            rows = rows || [];
            if (!rows.length) { empty(s, emptyMsg || '데이터가 없습니다'); return; }

            var ctx = s.ctx, padL = 92, padR = 44, top = 8;
            var barH = 12, gap = (s.h - top - 6) / rows.length;

            rows.forEach(function (r, i) {
                var y = top + i * gap, full = s.w - padL - padR;
                ctx.fillStyle = INK; ctx.textAlign = 'right';
                ctx.fillText(cut(r.label, 8), padL - 10, y + barH - 1);

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
