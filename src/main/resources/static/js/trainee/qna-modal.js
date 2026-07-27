
/* =========================
   qna-detail-modal.js
   - 상세 모달 열기/렌더링
   - 조회수 +1 반영 후 qna:dataUpdated 발행
========================= */
(function () {
  const detailModal = document.getElementById('detailModal');

  const dTitle = document.getElementById('dTitle');
  const dStatus = document.getElementById('dStatus');
  const dCategory = document.getElementById('dCategory');
  const dVisibility = document.getElementById('dVisibility');
  const dMeta = document.getElementById('dMeta');
  const dBody = document.getElementById('dBody');

  const answerCard = document.getElementById('answerCard');
  const pendingCard = document.getElementById('pendingCard');
  const dAnswerMeta = document.getElementById('dAnswerMeta');
  const dAnswerBody = document.getElementById('dAnswerBody');

  function statusLabel(status) {
    return status === 'answered' ? '답변완료' : '답변대기';
  }
  function visibilityLabel(v) {
    return v === 'course' ? '과정공유' : '나만보기';
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

  function paintStatusPill(status) {
    dStatus.textContent = statusLabel(status);
    if (status === 'answered') {
      dStatus.style.background = '#eaf2ff';
      dStatus.style.borderColor = '#cfe0ff';
      dStatus.style.color = '#1f4aa8';
    } else {
      dStatus.style.background = '#fff2e6';
      dStatus.style.borderColor = '#ffd9b3';
      dStatus.style.color = '#9a4b00';
    }
  }

  function openDetailById(id) {
    const item = window.QNA_STORE.items.find(x => x.id === id);
    if (!item) return;

    // 조회수 +1
    item.views += 1;

    dTitle.textContent = item.title;
    paintStatusPill(item.status);

    dCategory.textContent = item.category;
    dVisibility.textContent = visibilityLabel(item.visibility);
    dMeta.textContent = `${item.courseName} · ${item.sessionName || '-'} · 작성일 ${item.createdAt} · 조회 ${item.views}`;
    dBody.textContent = item.body;

    if (item.answer) {
      pendingCard.hidden = true;
      answerCard.hidden = false;
      dAnswerMeta.textContent = `${item.answer.author} · ${item.answer.createdAt}`;
      dAnswerBody.textContent = item.answer.body;
    } else {
      answerCard.hidden = true;
      pendingCard.hidden = false;
      dAnswerMeta.textContent = '';
      dAnswerBody.textContent = '';
    }

    // 리스트에 조회수 반영 요청
    window.dispatchEvent(new CustomEvent('qna:dataUpdated'));

    openModal(detailModal);
  }

  function bindEvents() {
    // 리스트에서 상세 오픈 요청 받기
    window.addEventListener('qna:openDetail', (e) => {
      const id = e.detail?.id;
      if (!id) return;
      openDetailById(id);
    });

    // backdrop click close
    detailModal.addEventListener('click', (e) => {
      if (e.target === detailModal) closeModal(detailModal);
    });

    // close buttons
    detailModal.querySelectorAll('[data-close="detailModal"]').forEach(btn => {
      btn.addEventListener('click', () => closeModal(detailModal));
    });

    // ESC to close
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && detailModal.classList.contains('open')) closeModal(detailModal);
    });
  }

  bindEvents();
})();
