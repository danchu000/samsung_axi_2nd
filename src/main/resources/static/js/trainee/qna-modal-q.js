
    // 제목 글자수 카운트
    const aTitle = document.getElementById('aTitle');
    const titleCount = document.getElementById('titleCount');
    if (aTitle && titleCount) {
      aTitle.addEventListener('input', function() {
        titleCount.textContent = `${aTitle.value.length} / 80`;
      });
    }
    // 모달 닫기
    document.querySelectorAll('[data-close="askModal"]').forEach(btn => {
      btn.addEventListener('click', () => {
        document.getElementById('askModal').style.display = 'none';
      });
    });

/* =========================
   qna-ask-modal.js
   - 질문하기 모달(열기/닫기/검증/등록)
   - 등록 완료 시 qna:created 이벤트 발행
========================= */
(function () {
  const askModal = document.getElementById('askModal');
  const btnOpenAsk = document.getElementById('btnOpenAsk');

  const aCourse = document.getElementById('aCourse');
  const aSession = document.getElementById('aSession');
  const aCategory = document.getElementById('aCategory');
  const aVisibility = document.getElementById('aVisibility');
  const aTitle = document.getElementById('aTitle');
  const aBody = document.getElementById('aBody');
  const titleCount = document.getElementById('titleCount');
  const askError = document.getElementById('askError');
  const btnSubmitAsk = document.getElementById('btnSubmitAsk');

  function escapeText(s) {
    return String(s)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  }

  function openModal(el) {
    el.classList.add('open');
    el.setAttribute('aria-hidden', 'false');
    document.body.style.overflow = 'hidden';
  }
  function closeModal(el) {
    el.classList.remove('open');
    el.setAttribute('aria-hidden', 'true');
    document.body.style.overflow = '';
  }

  function renderCourseSelect() {
    const opts = window.QNA_STORE.courses
      .map(c => `<option value="${c.id}">${escapeText(c.name)}</option>`)
      .join('');
    aCourse.innerHTML = `<option value="">선택</option>` + opts;
  }

  function renderSessionSelect(courseId) {
    const course = window.QNA_STORE.courses.find(c => c.id === courseId);
    if (!course) {
      aSession.innerHTML = `<option value="">선택(선택)</option>`;
      aSession.disabled = true;
      return;
    }
    const opts = course.sessions.map(s => `<option value="${s.id}">${escapeText(s.name)}</option>`).join('');
    aSession.innerHTML = `<option value="">선택(선택)</option>` + opts;
    aSession.disabled = false;
  }

  function resetAskForm() {
    aCourse.value = '';
    renderSessionSelect('');
    aSession.value = '';
    aCategory.value = '학습내용';
    aVisibility.value = 'private';
    aTitle.value = '';
    aBody.value = '';
    titleCount.textContent = '0 / 80';
    askError.hidden = true;
    askError.textContent = '';
    btnSubmitAsk.disabled = false;
  }

  function showAskError(msg) {
    askError.hidden = false;
    askError.textContent = msg;
  }

  function makeNewItem() {
    const courseId = aCourse.value;
    const sessionId = aSession.value;
    const category = aCategory.value;
    const visibility = aVisibility.value;
    const title = aTitle.value.trim();
    const body = aBody.value.trim();

    if (!courseId) return { error: '과정을 선택해주세요.' };
    if (!title) return { error: '제목을 입력해주세요.' };
    if (!body) return { error: '내용을 입력해주세요.' };

    const course = window.QNA_STORE.courses.find(c => c.id === courseId);
    const session = course?.sessions.find(s => s.id === sessionId);

    const nextNo = (window.QNA_STORE.items.reduce((max, x) => Math.max(max, x.no), 0) || 0) + 1;

    const today = new Date();
    const createdAt = `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,'0')}-${String(today.getDate()).padStart(2,'0')}`;

    return {
      item: {
        id: 'q' + nextNo,
        no: nextNo,
        status: 'pending',
        category,
        title,
        body,
        courseId,
        courseName: course ? course.name : '-',
        sessionId: sessionId || '',
        sessionName: session ? session.name : '-',
        createdAt,
        views: 0,
        visibility,
        answer: null
      }
    };
  }

  function submitAsk() {
    askError.hidden = true;

    const { item, error } = makeNewItem();
    if (error) return showAskError(error);

    closeModal(askModal);

    // 리스트 모듈에 “새 글 생성” 알림
    window.dispatchEvent(new CustomEvent('qna:created', { detail: { item } }));
  }

  function bindEvents() {
    btnOpenAsk.addEventListener('click', () => {
      resetAskForm();
      openModal(askModal);
      aCourse.focus();
    });

    aCourse.addEventListener('change', () => {
      renderSessionSelect(aCourse.value);
      aSession.value = '';
    });

    aTitle.addEventListener('input', () => {
      titleCount.textContent = `${aTitle.value.length} / 80`;
    });

    btnSubmitAsk.addEventListener('click', submitAsk);

    // backdrop click to close
    askModal.addEventListener('click', (e) => {
      if (e.target === askModal) closeModal(askModal);
    });

    // close buttons
    askModal.querySelectorAll('[data-close="askModal"]').forEach(btn => {
      btn.addEventListener('click', () => closeModal(askModal));
    });

    // ESC to close
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && askModal.classList.contains('open')) closeModal(askModal);
    });
  }

  function init() {
    renderCourseSelect();
    renderSessionSelect('');
    bindEvents();
  }

  init();
})();

