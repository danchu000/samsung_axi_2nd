/**
 * 과제 배정 화면에서 "AI 추천으로 왔을 때" 값을 미리 채운다.
 *
 * 강사 AI 학습진단에서 "과제 배부"를 누르면 아래 쿼리를 달고 이 화면으로 온다.
 *   ?aiTask=과제명&aiTrainees=김훈련,이수강&aiCourseId=1
 *
 * 폼 바인딩(th:field)은 건드리지 않는다 — 값만 넣고 나머지는 강사가 확인해 채운다.
 * AI 가 배정을 확정하는 게 아니라, 입력을 줄여주는 역할이다.
 */
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        var q = new URLSearchParams(window.location.search);
        var task = q.get('aiTask');
        if (!task) return;   // 평소 배정 화면에서는 아무 일도 하지 않는다

        var trainees = (q.get('aiTrainees') || '').split(',').filter(Boolean);
        var courseId = q.get('aiCourseId');

        banner(task, trainees);
        prefill(task, courseId);
    });

    /** 어디서 왜 왔는지 화면에 남긴다 — 값이 미리 채워진 이유를 알 수 있어야 한다. */
    function banner(task, trainees) {
        var anchor = document.querySelector('.main-content-inner')
                  || document.querySelector('main')
                  || document.body;

        var box = document.createElement('div');
        box.className = 'ai-disclaimer';
        box.style.margin = '0 0 16px';
        box.innerHTML =
            '<span>🤖</span>' +
            '<span><b>AI 학습진단에서 넘어왔어요.</b><br>' +
            '추천 과제: <b>' + esc(task) + '</b>' +
            (trainees.length
                ? '<br>대상 훈련생 ' + trainees.length + '명: ' + esc(trainees.join(', '))
                : '') +
            '<br>아래 내용을 확인하고 배정해 주세요. <b>확인 없이 배정되지 않아요.</b></span>';

        anchor.insertBefore(box, anchor.firstChild);
    }

    function prefill(task, courseId) {
        // 과정 — 진단 대상 훈련생이 듣는 과정
        var course = document.getElementById('courseSelect');
        if (course && courseId) {
            setIfExists(course, courseId);
        }

        /*
         * 과제 제목. 이 화면은 "기존 과제 선택(라디오)" 과 "새 과제 만들기" 두 갈래인데,
         * AI 추천 과제는 아직 없는 과제이므로 새로 만드는 쪽에 넣는다.
         */
        var title = document.getElementById('newAssignmentTitle');
        if (title && !title.value) {
            title.value = task;
            title.focus();
        }
    }

    /** 없는 값을 강제로 넣으면 select 가 빈 상태가 되므로, 있는 경우에만 고른다. */
    function setIfExists(select, value) {
        for (var i = 0; i < select.options.length; i++) {
            if (select.options[i].value === String(value)) {
                select.value = String(value);
                select.dispatchEvent(new Event('change', { bubbles: true }));
                return;
            }
        }
    }

    function esc(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
})();
