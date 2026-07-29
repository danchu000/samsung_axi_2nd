/**
 * AI 학습 도우미 (훈련생) — 기존 글등록형 Q&A 를 대체하는 채팅 화면.
 *
 * 지금은 화면만 있는 단계라 답변을 **정해진 더미에서 골라** 보여준다.
 * 실제 모델 연동 시 sendToServer() 안쪽만 fetch 로 바꾸면 되고,
 * 화면 코드는 건드릴 필요가 없다.
 *
 * 설계상 지킨 것
 *  - 답변에는 항상 **근거 자료**를 붙인다. 출처 없는 답은 학습에 해롭다.
 *  - 사람에게 넘기는 경로(강사 전달)를 항상 보이는 자리에 둔다.
 */
(function () {
    'use strict';

    var CANNED = [
        {
            match: ['트랜잭션', 'acid', '격리'],
            answer: '트랜잭션의 ACID 중 격리성(Isolation)은 "동시에 실행되는 여러 트랜잭션이 서로의 중간 상태를 보지 못하는 성질"이에요.\n\n' +
                    '예를 들어 A가 계좌 이체를 하는 도중에 B가 잔액을 조회하면, B는 이체가 끝나기 전의 값이나 끝난 후의 값 중 하나만 보게 되고 "출금은 됐는데 입금은 안 된" 중간 상태는 볼 수 없어요.',
            sources: [{ label: '3주차 강의자료 — 트랜잭션과 동시성 (p.12)', href: '#' },
                      { label: '단원평가 3회차 4번 문항 해설', href: '#' }]
        },
        {
            match: ['도커', 'docker', '컨테이너'],
            answer: 'Docker 는 애플리케이션과 실행에 필요한 환경을 하나로 묶어 "어디서 실행해도 같게" 만들어 주는 도구예요.\n\n' +
                    '내 컴퓨터에서는 되는데 서버에서는 안 되는 문제를 줄이려고 씁니다. 이미지(설계도)를 만들고, 그걸 실행한 것이 컨테이너예요.',
            sources: [{ label: '5주차 실습자료 — 컨테이너 기초 (p.3)', href: '#' }]
        },
        {
            match: ['jpa', '영속성', '엔티티'],
            answer: 'JPA 의 영속성 컨텍스트는 엔티티를 보관하는 "1차 캐시"라고 생각하면 이해가 쉬워요.\n\n' +
                    '같은 트랜잭션 안에서 같은 엔티티를 두 번 조회하면 DB에 두 번 가지 않고 캐시에서 꺼내 줍니다. 그래서 두 객체가 == 비교에서도 같아요.',
            sources: [{ label: '4주차 강의자료 — JPA 영속성 컨텍스트 (p.8)', href: '#' }]
        }
    ];

    var FALLBACK = {
        answer: '죄송해요, 학습 자료에서 관련 내용을 찾지 못했어요.\n\n' +
                '질문을 조금 더 구체적으로 적어주시거나, 아래 "강사님께 전달" 버튼을 눌러 주세요. 담당 강사님이 직접 답변해 드려요.',
        sources: []
    };

    var SUGGEST = [
        '트랜잭션 격리성이 뭔가요?',
        'JPA 영속성 컨텍스트를 쉽게 설명해 주세요',
        'Docker 는 왜 쓰나요?',
        '이번 주 과제에서 어떤 걸 평가하나요?'
    ];

    var ESCALATED = [
        { q: '3주차 실습에서 커넥션 풀 설정값을 어떻게 정해야 하나요?', teacher: '김강사', status: '답변완료', date: '2026-07-26' },
        { q: '과제 제출 형식이 zip 이어도 되나요?', teacher: '김강사', status: '대기중', date: '2026-07-28' }
    ];

    var log, input, busy = false;

    document.addEventListener('DOMContentLoaded', function () {
        log = document.getElementById('chatLog');
        input = document.getElementById('chatInput');

        greet();
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

        document.getElementById('btnEscalate').addEventListener('click', function () {
            if (!log.querySelector('.ai-msg.me')) {
                alert('전달할 대화가 없어요. 먼저 질문을 입력해 주세요.');
                return;
            }
            alert('담당 강사님께 전달되었어요.\n답변이 등록되면 알림으로 알려드려요.\n\n(서버 연동 준비 중)');
        });
    });

    function greet() {
        push('ai',
            '안녕하세요! 학습 중 궁금한 점을 물어보세요.\n선택하신 과정의 강의자료와 평가 해설을 바탕으로 답해 드려요.',
            []);
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
            return '<tr>' +
                '<td>' + esc(r.q) + '</td>' +
                '<td>' + esc(r.teacher) + '</td>' +
                '<td><span class="ai-level ' + cls + '">' + esc(r.status) + '</span></td>' +
                '<td>' + esc(r.date) + '</td></tr>';
        }).join('');
    }

    function send() {
        if (busy) return;
        var text = input.value.trim();
        if (!text) return;

        push('me', text, []);
        input.value = '';
        input.style.height = 'auto';

        busy = true;
        var typing = pushTyping();

        sendToServer(text, function (res) {
            typing.remove();
            push('ai', res.answer, res.sources);
            busy = false;
        });
    }

    /**
     * 서버 연동 지점. 지금은 더미에서 고르고 살짝 지연을 준다.
     * 실제 연동 시 이 함수 안만 fetch('/trainee/ai-qna/ask', ...) 로 바꾼다.
     */
    function sendToServer(text, done) {
        var lower = text.toLowerCase();
        var hit = CANNED.filter(function (c) {
            return c.match.some(function (k) { return lower.indexOf(k) >= 0; });
        })[0];
        setTimeout(function () { done(hit || FALLBACK); }, 700);
    }

    function push(who, text, sources) {
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
            src.innerHTML = '📎 근거 자료<br>' + sources.map(function (s) {
                return '<a href="' + esc(s.href) + '">' + esc(s.label) + '</a>';
            }).join('<br>');
            body.appendChild(src);
        }

        var time = document.createElement('div');
        time.className = 'ai-msg-time';
        time.textContent = now();
        body.appendChild(time);

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
