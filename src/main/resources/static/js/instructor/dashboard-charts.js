/**
 * 강사 대시보드 차트.
 *
 * **서버가 실제로 내려준 값만 그린다** (window._serverInstructorDashboard).
 * 예전에 여기 있던 차트들은 고정 시드 난수로 만든 가짜였고, 심지어
 * "코딩 시간"·"이탈률"처럼 이 시스템이 수집하지도 않는 지표였다.
 * 없는 값을 그리면 화면은 그럴듯해지지만 판단은 틀어진다.
 *
 * 그리는 것 (전부 InstructorDashboardView 에 있는 값)
 *  1. 과정별 수강 인원   ← courses[].progress      "수강생 5명"
 *  2. 과정별 평균 출결률 ← courses[].todaySession  "평균 출결률 100%" (없으면 "-")
 *  3. 내 처리 대기 업무  ← kpi.*                    "0" "1" ...
 *
 * <b>필드 이름과 내용이 다르다.</b> progress 는 진도율이 아니라 수강 인원 문자열이고,
 * todaySession 에 출결률이 들어 있다. 이름만 보고 % 로 그리면 "수강생 5명"이 5% 로
 * 표시된다 — 실제 응답을 확인하고 맞춘 매핑이다.
 */
(function () {
    'use strict';

    var INK = '#101828', SUB = '#667085', LINE = '#eef0f6';
    var NAVY = '#131D41', NAVY2 = '#2a3866', AMBER = '#f59e0b', RED = '#ef4444';

    document.addEventListener('DOMContentLoaded', function () {
        var d = window._serverInstructorDashboard;
        if (!d) return;   // 서버 값이 없으면 아무것도 그리지 않는다 (가짜로 채우지 않는다)

        draw(d);

        var t;
        window.addEventListener('resize', function () {
            clearTimeout(t);
            t = setTimeout(function () { draw(d); }, 200);
        });
    });

    function draw(d) {
        var courses = d.courses || [];
        headcountChart('courseProgressChart', courses);
        attendanceChart('attendanceChart', courses);
        workloadChart('myWorkloadChart', d.kpi);
    }

    /** 문자열로 내려오는 값("62%", "3명", "-")에서 숫자만 뽑는다. 없으면 null. */
    function num(v) {
        if (v === null || v === undefined) return null;
        var m = String(v).match(/-?\d+(\.\d+)?/);
        return m ? parseFloat(m[0]) : null;
    }

    function setup(id) {
        var cv = document.getElementById(id);
        if (!cv) return null;
        var dpr = window.devicePixelRatio || 1;
        var w = cv.clientWidth || cv.parentNode.clientWidth || 320;
        var h = Number(cv.getAttribute('height')) || 220;
        cv.width = w * dpr;
        cv.height = h * dpr;
        cv.style.height = h + 'px';
        var ctx = cv.getContext('2d');
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ctx.clearRect(0, 0, w, h);
        ctx.font = '12px Pretendard, sans-serif';
        return { ctx: ctx, w: w, h: h };
    }

    /** 값이 하나도 없을 때. 빈 그래프를 그리면 "0" 인지 "모름" 인지 구분이 안 된다. */
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

    /** 과정별 수강 인원 — 가로 막대. courses[].progress 는 "수강생 N명" 문자열이다. */
    function headcountChart(id, courses) {
        var s = setup(id); if (!s) return;
        var rows = courses.map(function (c) { return { name: c.name, v: num(c.progress) }; })
                          .filter(function (r) { return r.v !== null; });
        if (!rows.length) { empty(s, '수강 인원 정보가 없습니다'); return; }

        var ctx = s.ctx, padL = 84, padR = 42, top = 10;
        var barH = 13, gap = (s.h - top - 8) / rows.length;
        var max = Math.max(1, Math.max.apply(null, rows.map(function (r) { return r.v; })));

        rows.forEach(function (r, i) {
            var y = top + i * gap, full = s.w - padL - padR;
            ctx.fillStyle = INK; ctx.textAlign = 'right';
            ctx.fillText(cut(r.name, 7), padL - 8, y + barH - 1);

            ctx.fillStyle = '#f3f4f6';
            rrect(ctx, padL, y, full, barH, 6);
            ctx.fillStyle = NAVY;
            rrect(ctx, padL, y, full * r.v / max, barH, 6);

            ctx.textAlign = 'left'; ctx.fillStyle = SUB;
            ctx.fillText(r.v + '명', s.w - padR + 6, y + barH - 1);
        });
    }

    /**
     * 과정별 평균 출결률 — 세로 막대. courses[].todaySession 이 "평균 출결률 100%" 이다.
     * 값이 없는 과정("평균 출결률 -")은 0% 로 그리지 않고 뺀다 —
     * "출결 0%" 와 "아직 집계 없음"은 완전히 다른 뜻이다.
     */
    function attendanceChart(id, courses) {
        var s = setup(id); if (!s) return;
        var rows = courses.map(function (c) { return { name: c.name, v: num(c.todaySession) }; })
                          .filter(function (r) { return r.v !== null; });
        if (!rows.length) { empty(s, '출결 집계가 아직 없습니다'); return; }

        var ctx = s.ctx, padL = 32, padR = 10, padT = 14, padB = 30;
        var gw = s.w - padL - padR, gh = s.h - padT - padB;
        var slot = gw / rows.length, bw = Math.min(46, slot * 0.52);

        for (var i = 0; i <= 4; i++) {
            var y = padT + gh * i / 4;
            ctx.strokeStyle = LINE; ctx.lineWidth = 1;
            ctx.beginPath(); ctx.moveTo(padL, y); ctx.lineTo(padL + gw, y); ctx.stroke();
            ctx.fillStyle = SUB; ctx.textAlign = 'right';
            ctx.fillText((100 - i * 25) + '', padL - 5, y + 4);
        }

        rows.forEach(function (r, i) {
            var x = padL + slot * i + (slot - bw) / 2;
            var hh = gh * Math.min(r.v, 100) / 100;
            // 출결 80% 미만은 이수 위험 — 색은 경고에만 쓴다
            ctx.fillStyle = r.v < 70 ? RED : (r.v < 80 ? AMBER : NAVY);
            rrect(ctx, x, padT + gh - hh, bw, Math.max(hh, 3), 5);

            ctx.textAlign = 'center'; ctx.fillStyle = INK;
            ctx.fillText(r.v + '%', x + bw / 2, padT + gh - hh - 5);
            ctx.fillStyle = SUB;
            ctx.fillText(cut(r.name, 6), x + bw / 2, s.h - 10);
        });
    }

    /** 내 처리 대기 업무 — 지금 손봐야 할 것이 무엇인지. */
    function workloadChart(id, kpi) {
        var s = setup(id); if (!s) return;
        if (!kpi) { empty(s, '데이터가 없습니다'); return; }

        var rows = [
            { name: '과제 채점', v: num(kpi.pendingAssignments) || 0 },
            { name: '시험 채점', v: num(kpi.pendingExams) || 0 },
            { name: '문의 응답', v: num(kpi.pendingSupport) || 0 },
            { name: '시험 감독', v: num(kpi.proctoringExams) || 0 }
        ];
        var total = rows.reduce(function (a, r) { return a + r.v; }, 0);
        if (total === 0) { empty(s, '처리할 업무가 없습니다 👍'); return; }

        var ctx = s.ctx, padL = 66, padR = 34, top = 12;
        var barH = 14, gap = (s.h - top - 10) / rows.length;
        var max = Math.max.apply(null, rows.map(function (r) { return r.v; }));

        rows.forEach(function (r, i) {
            var y = top + i * gap, full = s.w - padL - padR;
            ctx.fillStyle = INK; ctx.textAlign = 'right';
            ctx.fillText(r.name, padL - 8, y + barH - 1);

            ctx.fillStyle = '#f3f4f6';
            rrect(ctx, padL, y, full, barH, 6);
            ctx.fillStyle = r.v === 0 ? '#e5e7eb' : (r.v >= 5 ? AMBER : NAVY2);
            rrect(ctx, padL, y, full * r.v / max, barH, 6);

            ctx.textAlign = 'left'; ctx.fillStyle = r.v === 0 ? '#9ca3af' : INK;
            ctx.fillText(r.v + '건', s.w - padR + 6, y + barH - 1);
        });
    }

    function cut(s, n) {
        s = String(s == null ? '' : s);
        return s.length > n ? s.slice(0, n - 1) + '…' : s;
    }
})();
