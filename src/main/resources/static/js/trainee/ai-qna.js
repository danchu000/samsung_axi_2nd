/**
 * AI 학습 도우미 (훈련생) — 기존 글등록형 Q&A 를 대체하는 채팅 화면.
 *
 * 답변은 서버(POST /trainee/ai/qna/ask)가 만든다. 화면에는 예시 답변을 두지 않는다 —
 * 더미가 남아 있으면 모델이 죽었을 때도 그럴듯한 답이 나와서 장애를 못 알아챈다.
 *
 * 설계상 지킨 것
 *  - 답변에 딸린 **관련 학습 자료**는 서버가 실제 콘텐츠 링크로 내려준 것만 보여준다.
 *  - 실패(한도 초과·모델 오류)도 말풍선으로 보여준다. 조용히 아무 일도 안 일어나면 안 된다.
 *  - 사람에게 넘기는 경로(강사 전달)를 항상 보이는 자리에 둔다.
 */
(function () {
    'use strict';

    /*
     * 질문 예시. 특정 기술을 적어두면 그 과정에서 다루지도 않는 내용을 물어보게 되고,
     * "학습 자료에 없다"는 답만 돌아온다. 어느 과정에서나 쓰이는 형태로만 둔다.
     */
    var SUGGEST = [
        '이번 주에 배운 내용을 요약해 주세요',
        '방금 강의에서 나온 개념을 쉽게 설명해 주세요',
        '이 과정에서 어떤 자료를 먼저 봐야 하나요?',
        '이번 과제는 무엇을 평가하나요?'
    ];

    /*
     * 강사에게 전달한 질문 — **실제 Q&A 데이터**(QnaListRow).
     * 서버 필드명이 화면과 다르므로 여기서 한 번에 맞춘다. 그대로 꽂으면 예외 없이
     * 빈 칸만 그려진다 — 제일 알아채기 어려운 고장이다.
     */
    var ESCALATED = (window._serverEscalated || []).map(function (r) {
        return {
            id: r.id,
            q: r.title,
            teacher: r.assigneeName || '배정 전',
            status: r.statusLabel,
            date: r.createdAtShort || r.createdAt
        };
    });

    var log, input, busy = false;

    /**
     * 대화 보관.
     *
     * 화면을 나갔다 와도 대화가 남아 있어야 한다 — 물어본 걸 다시 물어보게 만들면
     * 도우미로서 쓸모가 없다. 서버가 붙기 전까지는 브라우저에 보관한다.
     * (서버 연동 시 이 함수들만 API 호출로 바꾸면 된다)
     */
    var STORE_KEY = 'lxp.ai.qna.history.v1';
    var MAX_KEEP = 100;   // 무한정 쌓이면 브라우저 저장 한도를 넘는다

    function loadHistory() {
        try {
            var raw = localStorage.getItem(STORE_KEY);
            return raw ? JSON.parse(raw) : [];
        } catch (e) {
            return [];   // 저장이 막힌 브라우저(시크릿 모드 등)에서도 화면은 동작해야 한다
        }
    }

    function saveHistory(list) {
        try {
            localStorage.setItem(STORE_KEY, JSON.stringify(list.slice(-MAX_KEEP)));
        } catch (e) { /* 저장 실패해도 대화는 계속 가능하다 */ }
    }

    function appendHistory(item) {
        var list = loadHistory();
        list.push(item);
        saveHistory(list);
    }

    document.addEventListener('DOMContentLoaded', function () {
        log = document.getElementById('chatLog');
        input = document.getElementById('chatInput');

        restore();
        renderSuggest();
        renderEscalated();

        document.getElementById('btnSend').addEventListener('click', send);

        input.addEventListener('keydown', function (e) {
            // Shift+Enter 는 줄바꿈, Enter 는 전송
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                send();
            }
        });

        // 입력 길이에 따라 높이 자동 조절
        input.addEventListener('input', function () {
            input.style.height = 'auto';
            input.style.height = Math.min(input.scrollHeight, 120) + 'px';
        });

        document.getElementById('btnClear').addEventListener('click', function () {
            if (!confirm('지금까지의 대화를 모두 지울까요?\n지운 대화는 되돌릴 수 없어요.')) return;
            try { localStorage.removeItem(STORE_KEY); } catch (e) { /* 무시 */ }
            log.innerHTML = '';
            restore();
        });

        document.getElementById('btnEscalate').addEventListener('click', escalate);
    });

    /**
     * AI 로 해결이 안 된 대화를 강사에게 <b>실제로</b> 전달한다.
     *
     * 이전에는 안내 문구만 띄우고 아무 데도 저장하지 않았다. 훈련생은 답변을 기다리는데
     * 강사에게는 아무것도 안 갔다 — AI 도우미에서 가장 나쁜 고장이다.
     * 이제 서버가 기존 Q&A 로 등록하고, 등록된 글로 바로 갈 수 있게 링크를 준다.
     */
    function escalate() {
        var btn = document.getElementById('btnEscalate');
        if (!log.querySelector('.ai-msg.me')) {
            alert('전달할 대화가 없어요. 먼저 질문을 입력해 주세요.');
            return;
        }

        var courseSelect = document.getElementById('courseSelect');
        var courseId = courseSelect ? courseSelect.value : '';
        if (!courseId) {
            alert('질문할 과정을 먼저 선택해 주세요.');
            return;
        }

        // 저장된 대화를 그대로 보낸다 — 화면에서 긁어모으면 말풍선 장식까지 섞인다
        var turns = loadHistory().map(function (h) {
            return { who: h.who, text: h.text };
        });
        if (!turns.length) {
            alert('전달할 대화가 없어요.');
            return;
        }

        // 두 번 눌러 같은 질문이 두 건 등록되는 것을 막는다
        btn.disabled = true;
        var label = btn.textContent;
        btn.textContent = '전달 중…';

        var headers = { 'Content-Type': 'application/json' };
        var t = (document.querySelector('meta[name="_csrf"]') || {}).content;
        var h = (document.querySelector('meta[name="_csrf_header"]') || {}).content;
        if (t && h) headers[h] = t;

        fetch('/trainee/ai/qna/escalate', {
            method: 'POST', headers: headers, credentials: 'same-origin',
            body: JSON.stringify({ courseId: Number(courseId), turns: turns })
        })
            .then(function (res) {
                if (!res.ok) throw new Error('HTTP ' + res.status);
                return res.json();
            })
            .then(function (d) {
                if (!d.ok) { alert(d.message); return; }
                if (confirm(d.message + '\n\n전달한 질문을 지금 확인할까요?')) {
                    window.location.href = '/trainee/qna/' + d.qnaId;
                }
            })
            .catch(function () {
                // 실패를 성공처럼 보이게 하면 오지 않을 답을 기다리게 된다
                alert('전달하지 못했어요. 잠시 후 다시 시도하거나 Q&A 화면에서 직접 남겨 주세요.');
            })
            .finally(function () {
                btn.disabled = false;
                btn.textContent = label;
            });
    }

    /** 저장된 대화를 되살린다. 없으면 인사말부터 시작. */
    function restore() {
        var list = loadHistory();
        if (!list.length) {
            push('ai',
                '안녕하세요! 학습 중 궁금한 점을 물어보세요.\n선택하신 과정의 강의자료와 평가 해설을 바탕으로 답해 드려요.',
                [], null, false);
            return;
        }

        list.forEach(function (m) {
            push(m.who, m.text, m.sources || [], m.time, false);
        });

        // 이어서 대화하는 것임을 알려준다 — 지난 대화가 방금 한 것처럼 보이면 혼란스럽다
        var mark = document.createElement('div');
        mark.style.cssText = 'text-align:center; font-size:12px; color:#9ca3af; padding:4px 0;';
        mark.textContent = '— 이전 대화를 이어서 보고 있어요 —';
        log.appendChild(mark);
        log.scrollTop = log.scrollHeight;
    }

    function renderSuggest() {
        var box = document.getElementById('suggestBox');
        box.innerHTML = SUGGEST.map(function (s) {
            return '<button type="button" class="ai-tag partial js-suggest" style="cursor:pointer;">' + esc(s) + '</button>';
        }).join('');
        box.addEventListener('click', function (e) {
            if (!e.target.classList.contains('js-suggest')) return;
            input.value = e.target.textContent;
            send();
        });
    }

    function renderEscalated() {
        var body = document.getElementById('escalatedBody');
        if (!ESCALATED.length) {
            body.innerHTML = '<tr><td colspan="4" class="ai-empty">전달한 질문이 없어요.</td></tr>';
            return;
        }
        body.innerHTML = ESCALATED.map(function (r) {
            var cls = r.status === '답변완료' ? 'low' : 'mid';
            // 제목을 눌러 실제 Q&A 글로 갈 수 있어야 한다 — 목록만 보여주면 답변을 못 읽는다
            var title = r.id
                ? '<a href="/trainee/qna/' + encodeURIComponent(r.id) + '">' + esc(r.q) + '</a>'
                : esc(r.q);
            return '<tr>' +
                '<td>' + title + '</td>' +
                '<td>' + esc(r.teacher) + '</td>' +
                '<td><span class="ai-level ' + cls + '">' + esc(r.status) + '</span></td>' +
                '<td>' + esc(r.date) + '</td></tr>';
        }).join('');
    }

    function send() {
        if (busy) return;
        var text = input.value.trim();
        if (!text) return;

        var t = now();
        push('me', text, [], t);
        appendHistory({ who: 'me', text: text, sources: [], time: t });

        input.value = '';
        input.style.height = 'auto';

        busy = true;
        var typing = pushTyping();

        sendToServer(text, function (res) {
            typing.remove();
            var at = now();
            push('ai', res.answer, res.sources, at);
            appendHistory({ who: 'ai', text: res.answer, sources: res.sources, time: at });
            busy = false;
        });
    }

    /**
     * 서버에 질문을 보낸다.
     *
     * 서버는 실패해도 200 + {ok:false, answer:안내문구} 로 답한다. 채팅창에서 500 이 나면
     * 사용자에게 보여줄 말이 없기 때문이다. 그래서 여기서는 ok 를 보지 않고 answer 를 그대로
     * 말풍선에 띄운다 — 실패 사유는 이미 사람이 읽을 수 있는 문장으로 와 있다.
     *
     * 네트워크 자체가 끊긴 경우(fetch reject)만 여기서 문구를 만든다.
     */
    function sendToServer(text, done) {
        var courseSelect = document.getElementById('courseSelect');
        var courseId = courseSelect ? courseSelect.value : '';

        if (!courseId) {
            done({ answer: '질문할 과정을 먼저 선택해 주세요. 수강 중인 과정이 없으면 질문할 수 없어요.', sources: [] });
            return;
        }

        var headers = { 'Content-Type': 'application/json' };
        var csrfToken = (document.querySelector('meta[name="_csrf"]') || {}).content;
        var csrfHeader = (document.querySelector('meta[name="_csrf_header"]') || {}).content;
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

        fetch('/trainee/ai/qna/ask', {
            method: 'POST',
            headers: headers,
            credentials: 'same-origin',
            body: JSON.stringify({ courseId: Number(courseId), question: text })
        })
            .then(function (res) {
                if (!res.ok) throw new Error('HTTP ' + res.status);
                return res.json();
            })
            .then(function (data) {
                done({ answer: data.answer, sources: data.sources || [] });
            })
            .catch(function () {
                done({
                    answer: '답변을 가져오지 못했어요. 네트워크 상태를 확인하고 다시 시도해 주세요.\n' +
                            '계속 안 되면 아래 "강사님께 전달" 버튼을 눌러 주세요.',
                    sources: []
                });
            });
    }

    function push(who, text, sources, time, scroll) {
        var wrap = document.createElement('div');
        wrap.className = 'ai-msg' + (who === 'me' ? ' me' : '');

        var avatar = document.createElement('div');
        avatar.className = 'ai-msg-avatar';
        avatar.textContent = who === 'me' ? '나' : 'AI';

        var body = document.createElement('div');
        body.className = 'ai-msg-body';
        body.textContent = text;

        if (sources && sources.length) {
            var src = document.createElement('div');
            src.className = 'ai-source';
            // "근거"가 아니라 "관련 자료" — 서버는 자료의 제목·설명까지만 보고 답한다.
            // 본문을 읽지 않은 답을 근거라고 부르면 훈련생이 답변을 과신한다.
            src.innerHTML = '📎 관련 학습 자료<br>' + sources.map(function (s) {
                return '<a href="' + esc(s.href) + '">' + esc(s.label) + '</a>';
            }).join('<br>');
            body.appendChild(src);
        }

        var stamp = document.createElement('div');
        stamp.className = 'ai-msg-time';
        stamp.textContent = time || now();
        body.appendChild(stamp);

        wrap.appendChild(avatar);
        wrap.appendChild(body);
        log.appendChild(wrap);
        log.scrollTop = log.scrollHeight;
        return wrap;
    }

    function pushTyping() {
        var wrap = document.createElement('div');
        wrap.className = 'ai-msg';
        wrap.innerHTML = '<div class="ai-msg-avatar">AI</div>' +
            '<div class="ai-msg-body"><div class="ai-typing"><span></span><span></span><span></span></div></div>';
        log.appendChild(wrap);
        log.scrollTop = log.scrollHeight;
        return wrap;
    }

    function now() {
        var d = new Date();
        return ('0' + d.getHours()).slice(-2) + ':' + ('0' + d.getMinutes()).slice(-2);
    }

    function esc(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
})();
