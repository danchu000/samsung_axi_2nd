/**
 * 관리자 대시보드 차트.
 *
 * 그리기 자체는 dashboard-charts-common.js 가 한다 — 강사 화면과 같은 그림을 써야
 * 보는 범위만 다르고 판단 기준은 같아진다. 이 파일은 데이터만 준비한다.
 *
 * 그리는 것
 *  1. 과정별 진행 현황 — 진도율 vs 기간 경과율. 둘을 나란히 놔야 "지연"이 보인다
 *  2. 주간 학습 활동 추이 — 출석·제출·응시 3개 선
 *  3. 이수 요건 충족 현황 — 요건별 충족 인원
 *  4. 채용공고 요구 역량 TOP — 가로 막대
 *
 * 서버 연동 시 window._serverAdminCharts 를 이 스크립트보다 앞에서 대입한다.
 */
(function () {
    'use strict';

    /* 색은 공용 모듈과 같은 팔레트를 쓴다 (남색 기본, 경고에만 주황/빨강) */
    var C = { navy: '#131D41', navy2: '#2a3866', navy3: '#5a6a9a' };

    var DUMMY = {
        courses: [
            { name: '클라우드 풀스택', progress: 62, elapsed: 58 },
            { name: '데이터 분석 실무', progress: 41, elapsed: 70 },   // 지연
            { name: 'AI 서비스 개발', progress: 88, elapsed: 84 },
            { name: '백엔드 심화', progress: 25, elapsed: 46 },        // 지연
            { name: '프론트엔드 실무', progress: 73, elapsed: 71 }
        ],
        weeks: ['6/2', '6/9', '6/16', '6/23', '6/30', '7/7', '7/14', '7/21'],
        series: [
            { label: '출석률', color: C.navy, values: [92, 94, 91, 95, 93, 96, 94, 95] },
            { label: '과제 제출률', color: C.navy2, values: [78, 82, 75, 88, 84, 79, 86, 90] },
            { label: '평가 응시율', color: C.navy3, values: [65, 71, 88, 62, 74, 91, 70, 77] }
        ],
        completion: [
            { label: '진도 충족', done: 18, total: 24 },
            { label: '출석 충족', done: 21, total: 24 },
            { label: '성적 충족', done: 14, total: 24 },
            { label: '전체 충족', done: 12, total: 24 }
        ],
        demand: [
            { label: 'Docker', value: 74 },
            { label: 'AWS', value: 71 },
            { label: '테스트 코드', value: 68 },
            { label: 'CI/CD', value: 52 },
            { label: 'Redis', value: 39 }
        ]
    };

    document.addEventListener('DOMContentLoaded', function () {
        var d = merge(window._serverAdminCharts || DUMMY, window._serverDashboardMetrics);
        drawAll(d);

        // 폭이 바뀌면 canvas 픽셀 크기가 어긋나 흐려진다 — 다시 그린다
        var t;
        window.addEventListener('resize', function () {
            clearTimeout(t);
            t = setTimeout(function () { drawAll(d); }, 200);
        });
    });

    /**
     * 서버가 실제로 준 지표를 표본 위에 덮어쓴다.
     * 아직 서버가 안 주는 항목(주간 추이·요구 역량)은 표본을 그대로 쓰고,
     * 준 항목은 표본을 버린다 — 실제 값이 있는데 표본을 보여주면 안 된다.
     */
    function merge(base, m) {
        if (!m) return base;
        var out = Object.assign({}, base);
        out.serverCourses = !!(m.courses && m.courses.length);
        out.serverCompletion = !!(m.completion && m.completion.length);
        if (out.serverCourses) out.courses = m.courses;
        if (out.serverCompletion) out.completion = m.completion;
        // 서버가 "판정 이력 없음"을 알려준 경우: 표본으로 채우지 않고 비운다
        else if (m.completion) out.completion = [];
        if (m.courses && !m.courses.length) out.courses = [];
        return out;
    }

    function drawAll(d) {
        // 그리기는 공용 모듈(dashboard-charts-common.js)에 맡긴다.
        // 강사 화면과 같은 그림을 써야 판단 기준이 갈리지 않는다.
        var C = window.DashCharts;
        if (!C) return;

        C.courseProgress('courseProgressChart', d.courses, '진행 중인 과정이 없습니다');
        C.weekly('weeklyActivityChart', d.weeks, d.series);
        C.completion('completionChart', d.completion);
        C.hbar('aiDemandChart', d.demand);

        // 서버 값을 쓴 차트에는 "표본" 배지를 달지 않는다
        if (!d.serverCourses) mark('courseProgressChart');
        if (!d.serverCompletion) mark('completionChart');
        mark('weeklyActivityChart');
        mark('aiDemandChart');
    }

    /**
     * 아직 서버 값이 아닌 차트에는 표시를 남긴다.
     * 표본을 실제 수치로 착각하면 잘못된 판단을 하게 된다.
     */
    function mark(canvasId) {
        var cv = document.getElementById(canvasId);
        if (!cv || cv.parentNode.querySelector('.sample-mark')) return;
        var tag = document.createElement('span');
        tag.className = 'sample-mark';
        tag.textContent = '표본';
        tag.title = '서버 연동 전 표본 값입니다.';
        cv.parentNode.insertBefore(tag, cv);
    }

    function cut(s, n) {
        s = String(s == null ? '' : s);
        return s.length > n ? s.slice(0, n - 1) + '…' : s;
    }
})();
