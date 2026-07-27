 (function () {
    // -----------------------
    // 더미 데이터 (API 연결 전)
    // -----------------------
    const mockCourses = [
      {
        id: 'c01',
        name: '풀스택 웹 개발 (React & Node.js)',
        sessions: [
          { id: 's01', name: '1차시 - 변수와 자료형' },
          { id: 's02', name: '2차시 - 조건문' },
          { id: 's03', name: '3차시 - 반복문 (for/while)' },
          { id: 's04', name: '4차시 - 함수 기초' }
        ]
      },
      {
        id: 'c02',
        name: '데이터 분석 입문 (Python)',
        sessions: [
          { id: 's11', name: '1차시 - 판다스 기초' },
          { id: 's12', name: '2차시 - 데이터 전처리' },
          { id: 's13', name: '3차시 - 시각화 기초' }
        ]
      }
    ];

    const mockHistory = [
      {
        chatId: 'h01',
        title: '반복문에서 break/continue',
        meta: '풀스택 웹 개발 · 3차시',
        messages: [
          { role: 'tutor', text: '안녕하세요! 어떤 부분이 궁금해요?' },
          { role: 'me', text: 'break랑 continue 차이가 뭐야?' },
          { role: 'tutor', text: 'break는 반복문을 “즉시 종료”, continue는 “이번 회차만 건너뛰고 다음 회차로 진행”이에요.\n예제를 같이 볼까요?' }
        ]
      },
      {
        chatId: 'h02',
        title: '조건문 else if 흐름',
        meta: '풀스택 웹 개발 · 2차시',
        messages: [
          { role: 'tutor', text: '어떤 조건 분기가 헷갈리나요?' },
          { role: 'me', text: 'else if가 여러 개면 위에서부터 다 검사해?' },
          { role: 'tutor', text: '네, 위에서부터 순서대로 검사하고, 처음으로 true가 되는 블록 하나만 실행해요.' }
        ]
      }
    ];

    // -----------------------
    // 상태
    // -----------------------
    const state = {
      selectedCourseId: '',
      selectedSessionId: '',
      currentMessages: [],
      currentChatId: 'new'
    };

    // -----------------------
    // 엘리먼트
    // -----------------------
    const courseSelect = document.getElementById('courseSelect');
    const sessionSelect = document.getElementById('sessionSelect');
    const scopeCard = document.getElementById('scopeCard');
    const scopeEmpty = scopeCard.querySelector('.scope-empty');
    const scopeFilled = scopeCard.querySelector('.scope-filled');
    const scopeText = document.getElementById('scopeText');
    const scopeHint = document.getElementById('scopeHint');

    const chatScope = document.getElementById('chatScope');
    const chatWindow = document.getElementById('chatWindow');
    const chatInput = document.getElementById('chatInput');
    const btnSend = document.getElementById('btnSend');
    const btnNewChat = document.getElementById('btnNewChat');
    const chatHelp = document.getElementById('chatHelp');
    const historyList = document.getElementById('historyList');

    // -----------------------
    // 렌더: 과정/차시 옵션
    // -----------------------
    function renderCourseOptions() {
      const options = mockCourses.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');
      courseSelect.innerHTML = `<option value="">과정을 선택하세요</option>${options}`;
    }

    function renderSessionOptions(courseId) {
      const course = mockCourses.find(c => c.id === courseId);
      if (!course) {
        sessionSelect.innerHTML = `<option value="">차시를 선택하세요</option>`;
        sessionSelect.disabled = true;
        return;
      }
      const options = course.sessions.map(s => `<option value="${s.id}">${escapeHtml(s.name)}</option>`).join('');
      sessionSelect.innerHTML = `<option value="">차시를 선택하세요</option>${options}`;
      sessionSelect.disabled = false;
    }

    // -----------------------
    // 렌더: 최근 대화
    // -----------------------
    function renderHistory() {
      if (!mockHistory.length) {
        historyList.innerHTML = `<li style="color:#777;font-size:13px;">최근 대화가 없어요.</li>`;
        return;
      }
      historyList.innerHTML = mockHistory.map(item => {
        return `
          <li class="history-item">
            <button type="button" data-chatid="${item.chatId}">
              <div class="history-title">${escapeHtml(item.title)}</div>
              <div class="history-meta">${escapeHtml(item.meta)}</div>
            </button>
          </li>
        `;
      }).join('');
    }

    // -----------------------
    // 렌더: 채팅
    // -----------------------
    function renderChat(messages, { showDayDivider = true } = {}) {
      const parts = [];
      if (showDayDivider) {
        const today = new Date();
        const label = `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,'0')}-${String(today.getDate()).padStart(2,'0')}`;
        parts.push(`<div class="day-divider"><span>${label}</span></div>`);
      }

      messages.forEach(m => {
        const side = m.role === 'me' ? 'right' : 'left';
        parts.push(`
          <div class="msg ${side}">
            <div class="bubble">
              ${escapeHtml(m.text)}
              ${m.role === 'tutor' && m.source ? `<div class="bubble-meta"><span>참고: ${escapeHtml(m.source)}</span></div>` : ``}
            </div>
          </div>
        `);
      });

      chatWindow.innerHTML = parts.join('');
      scrollChatToBottom();
    }

    function appendMessage(role, text) {
      state.currentMessages.push({ role, text });
      renderChat(state.currentMessages);
    }

    function scrollChatToBottom() {
      chatWindow.scrollTop = chatWindow.scrollHeight;
    }

    // -----------------------
    // UI 활성/비활성
    // -----------------------
    function setChatEnabled(enabled) {
      chatInput.disabled = !enabled;
      btnSend.disabled = !enabled;
      btnNewChat.disabled = !enabled;
      chatHelp.textContent = enabled ? 'Enter 전송 · Shift+Enter 줄바꿈' : '먼저 과정/차시를 선택하세요.';
    }

    function updateScopeUI() {
      const course = mockCourses.find(c => c.id === state.selectedCourseId);
      const session = course?.sessions.find(s => s.id === state.selectedSessionId);

      const enabled = !!(course && session);

      if (!enabled) {
        scopeEmpty.hidden = false;
        scopeFilled.hidden = true;

        const badge = chatScope.querySelector('.badge');
        badge.classList.remove('on');
        badge.textContent = '미선택';
        chatScope.querySelector('.chat-scope-text').textContent = '과정/차시를 선택하면 채팅이 활성화돼요.';

        setChatEnabled(false);
        state.currentMessages = [
          { role: 'tutor', text: '안녕하세요! 먼저 과정/차시를 선택하면 질문을 시작할 수 있어요.' }
        ];
        renderChat(state.currentMessages);
        return;
      }

      scopeEmpty.hidden = true;
      scopeFilled.hidden = false;

      scopeText.textContent = `${course.name} / ${session.name}`;
      scopeHint.textContent = '선택한 범위를 기반으로 질문해보세요.';

      const badge = chatScope.querySelector('.badge');
      badge.classList.add('on');
      badge.textContent = '선택됨';
      chatScope.querySelector('.chat-scope-text').textContent = `${course.name} · ${session.name}`;

      setChatEnabled(true);

      // 선택 완료 시, 새 대화 기본 시작(첫 안내 메시지)
      state.currentChatId = 'new';
      state.currentMessages = [
        { role: 'tutor', text: `좋아요! "${session.name}" 범위에서 어떤 부분이 어려워요?\n원하는 방식(개념/예제/요약)도 같이 말해줘요.` }
      ];
      renderChat(state.currentMessages);
      focusInput();
    }

    function focusInput() {
      if (!chatInput.disabled) chatInput.focus();
    }

    // -----------------------
    // 이벤트
    // -----------------------
    courseSelect.addEventListener('change', (e) => {
      state.selectedCourseId = e.target.value;
      state.selectedSessionId = '';

      renderSessionOptions(state.selectedCourseId);
      sessionSelect.value = '';
      updateScopeUI();
    });

    sessionSelect.addEventListener('change', (e) => {
      state.selectedSessionId = e.target.value;
      updateScopeUI();
    });

    btnNewChat.addEventListener('click', () => {
      // 현재 선택 범위에서 대화 초기화
      updateScopeUI();
    });

    btnSend.addEventListener('click', onSend);

    // Enter 전송 / Shift+Enter 줄바꿈
    chatInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        onSend();
      }
    });

    // textarea 자동 높이 조절
    chatInput.addEventListener('input', () => {
      autoResize(chatInput);
    });

    historyList.addEventListener('click', (e) => {
      const btn = e.target.closest('button[data-chatid]');
      if (!btn) return;
      const chatId = btn.getAttribute('data-chatid');
      const item = mockHistory.find(h => h.chatId === chatId);
      if (!item) return;

      state.currentChatId = chatId;
      state.currentMessages = item.messages.map(m => ({ ...m }));

      // 히스토리 로드는 “읽기”라서 선택범위와 무관하게 볼 수 있게
      renderChat(state.currentMessages);
      // 단, 입력은 선택범위가 있어야 가능
      updateScopeUI();
    });

    function onSend() {
      if (chatInput.disabled) return;

      const text = chatInput.value.trim();
      if (!text) return;

      appendMessage('me', text);
      chatInput.value = '';
      autoResize(chatInput);

      // 더미: 튜터 답변 생성 (나중에 API 연결)
      showTypingThenReply(text);
    }

    function showTypingThenReply(userText) {
      // 타이핑 버블 추가
      const typingId = 'typing-bubble';
      const typingEl = document.createElement('div');
      typingEl.className = 'msg left';
      typingEl.id = typingId;
      typingEl.innerHTML = `
        <div class="bubble">
          <span class="typing" aria-label="답변 중">
            <i></i><i></i><i></i>
          </span>
        </div>
      `;
      chatWindow.appendChild(typingEl);
      scrollChatToBottom();

      setTimeout(() => {
        const t = document.getElementById(typingId);
        if (t) t.remove();

        // 아주 간단한 더미 응답
        const reply = makeDummyTutorReply(userText);
        appendMessage('tutor', reply);
      }, 700);
    }

    function makeDummyTutorReply(text) {
      const lower = text.toLowerCase();

      if (lower.includes('차이') || lower.includes('difference')) {
        return '“차이”를 물어보면, 보통 (1) 정의 (2) 동작 시점 (3) 예제 1~2개로 정리하면 빨라요.\n원하는 언어/상황(예: JS, Python)도 같이 말해줄래요?';
      }
      if (lower.includes('예제') || lower.includes('example')) {
        return '좋아요. 어떤 입력/출력 형태의 예제를 원해요?\n- 아주 쉬운 예제(개념 확인)\n- 실전형 예제(문제 풀이)\n원하는 난이도도 알려줘요.';
      }
      if (lower.includes('요약') || lower.includes('정리')) {
        return '핵심만 3줄 요약으로 정리해볼게요.\n1) 개념/정의\n2) 언제 쓰는지\n3) 주의할 점\n원하는 키워드가 있으면 같이 적어주세요.';
      }
      return '좋은 질문이에요. 지금 질문을 “개념(왜)”과 “동작(어떻게)”로 나눠서 설명해볼게요.\n어떤 부분에서 막혔는지(에러/상황)도 함께 알려주면 더 정확히 도와줄 수 있어요.';
    }

    function autoResize(el) {
      el.style.height = 'auto';
      el.style.height = Math.min(el.scrollHeight, 140) + 'px';
    }

    // XSS 방지용 간단 escape
    function escapeHtml(str) {
      return String(str)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;')
        .replaceAll('\n', '<br/>');
    }

    // -----------------------
    // 초기화
    // -----------------------
    function init() {
      renderCourseOptions();
      renderSessionOptions('');
      renderHistory();

      // 초기 안내 채팅
      state.currentMessages = [
        { role: 'tutor', text: '안녕하세요! 먼저 과정/차시를 선택하면 질문을 시작할 수 있어요.' }
      ];
      renderChat(state.currentMessages);
      setChatEnabled(false);
    }

    init();
  })();