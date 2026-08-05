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
    /*
     * 보관 키에 **사용자 id 를 붙인다.** 예전에는 고정 키 하나였는데, 그러면 같은 PC 에서
     * 계정을 바꿔 로그인해도 앞 사람의 대화가 그대로 복원된다 — 실습실 공용 PC 에서
     * 남의 질문 내용이 보이는 실제 유출 경로였다.
     *
     * id 는 서버가 화면에 심어준다(window._meId). 없으면 'anon' 으로 두되, 그 칸에는
     * 사실상 아무것도 쌓이지 않는다 — 로그인 없이는 이 화면에 못 들어온다.
     */
    var STORE_KEY = 'lxp.ai.qna.history.v1:' + (window._meId || 'anon');

    /*
     * 고정 키 시절에 쌓인 대화. 이미 브라우저에 남아 있으므로 **지운다.**
     * 그냥 두면 지금 이 수정을 배포해도 기존 PC 에서는 계속 남의 대화가 보인다.
     * (옮겨 담지 않는다 — 누구 것인지 알 수 없는 대화라 옮기면 유출을 그대로 이어받는다)
     */
    try { localStorage.removeItem('lxp.ai.qna.history.v1'); } catch (e) { /* 무시 */ }

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
            .then(checkResponse)
            .then(function (d) {
                if (!d.ok) { alert(d.message); return; }
                if (confirm(d.message + '\n\n전달한 질문을 지금 확인할까요?')) {
                    window.location.href = '/trainee/qna/' + d.qnaId;
                }
            })
            .catch(function (e) {
                // 실패를 성공처럼 보이게 하면 오지 않을 답을 기다리게 된다
                logFailure('강사 전달', e);
                alert(e && e.loginRequired
                    ? '로그인이 풀려서 전달하지 못했어요.\n페이지를 새로고침해 다시 로그인한 뒤 시도해 주세요.'
                    : '전달하지 못했어요. 잠시 후 다시 시도하거나 Q&A 화면에서 직접 남겨 주세요.');
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
            push(m.who, m.text, m.sources || [], m.time, false, m.general);
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
            push('ai', res.answer, res.sources, at, true, res.general);
            appendHistory({ who: 'ai', text: res.answer, sources: res.sources,
                            time: at, general: res.general });
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
            .then(checkResponse)
            .then(function (data) {
                done({ answer: data.answer, sources: data.sources || [], general: data.general });
            })
            .catch(function (e) {
                logFailure('질문 전송', e);
                done({ answer: failureMessage(e), sources: [] });
            });
    }

    /**
     * 응답을 JSON 으로 넘기기 전에 <b>실패를 구분해 둔다.</b>
     *
     * 서버는 모델 오류·한도 초과까지 전부 200 + 안내문구로 내려준다. 그래서 여기까지
     * 온 실패는 전부 "요청이 서버에 제대로 닿지 못한" 것이고, 원인마다 사용자가 할 일이
     * 완전히 다르다 — 로그인이 풀렸으면 새로고침, 오래 걸려 끊겼으면 재시도다.
     *
     * 특히 <b>세션 만료는 res.ok 로 못 잡는다.</b> Spring Security 가 로그인 화면으로
     * 리다이렉트하면 fetch 는 그걸 따라가 200 + HTML 을 받는다. 그대로 res.json() 을
     * 부르면 파싱 오류로 떨어져 "네트워크 상태를 확인하세요" 가 뜬다 — 네트워크는
     * 멀쩡한데 엉뚱한 곳을 보게 만드는 문구다.
     */
    function checkResponse(res) {
        var contentType = res.headers.get('content-type') || '';
        if (res.redirected || contentType.indexOf('json') < 0) {
            var expired = new Error('LOGIN_REQUIRED');
            expired.loginRequired = true;
            throw expired;
        }
        if (!res.ok) {
            var err = new Error('HTTP ' + res.status);
            err.status = res.status;   // 버리면 원인 구분이 영영 불가능하다
            throw err;
        }
        return res.json();
    }

    /** 원인별 안내. 사용자가 <b>지금 할 수 있는 행동</b>을 알려주는 것이 목적이다. */
    function failureMessage(e) {
        if (e && e.loginRequired) {
            return '로그인이 풀렸어요. 페이지를 새로고침해 다시 로그인한 뒤 물어봐 주세요.\n' +
                   '지금까지의 대화는 남아 있어요.';
        }
        var status = e && e.status;
        if (status === 401 || status === 403) {
            return '로그인 정보가 만료됐어요. 페이지를 새로고침한 뒤 다시 시도해 주세요.';
        }
        if (status === 502 || status === 504 || status === 524) {
            return '답변이 오래 걸려 연결이 끊겼어요. 질문을 조금 짧게 줄여 다시 시도해 주세요.\n' +
                   '계속 안 되면 아래 "강사님께 전달" 버튼을 눌러 주세요.';
        }
        if (status >= 500) {
            return '서버에서 오류가 났어요(' + status + '). 잠시 후 다시 시도하거나\n' +
                   '아래 "강사님께 전달" 버튼을 눌러 주세요.';
        }
        return '답변을 가져오지 못했어요. 네트워크 상태를 확인하고 다시 시도해 주세요.\n' +
               '계속 안 되면 아래 "강사님께 전달" 버튼을 눌러 주세요.';
    }

    /**
     * 콘솔에 상태 코드를 남긴다.
     *
     * 화면 문구는 훈련생용이라 원인을 다 담을 수 없다. 문의가 들어왔을 때
     * "F12 콘솔에 뭐라고 찍혀 있나요" 한 줄로 302/403/500/524 를 가를 수 있어야 한다.
     */
    function logFailure(what, e) {
        console.error('[AI] ' + what + ' 실패 — ' +
            (e && e.loginRequired ? '로그인 필요(세션 만료 추정)'
                                  : 'status=' + ((e && e.status) || '(응답 없음)')), e);
    }

    function push(who, text, sources, time, scroll, general) {
        var wrap = document.createElement('div');
        wrap.className = 'ai-msg' + (who === 'me' ? ' me' : '');

        var avatar = document.createElement('div');
        avatar.className = 'ai-msg-avatar';
        avatar.textContent = who === 'me' ? '나' : 'AI';

        var body = document.createElement('div');
        body.className = 'ai-msg-body';

        /*
         * 과정 자료가 아니라 모델의 일반 지식으로 답한 경우.
         * 표시 없이 섞이면 훈련생이 "수업에서 배운 내용"으로 오해한다 —
         * 시험 답으로 쓰거나 강사에게 "자료에 있다더라"고 말하게 된다.
         */
        if (general) {
            var badge = document.createElement('div');
            badge.className = 'ai-general-badge';
            badge.textContent = '💡 과정 자료 밖의 일반 지식이에요';
            body.appendChild(badge);
        }

        var textNode = document.createElement('div');
        textNode.textContent = text;
        body.appendChild(textNode);

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
