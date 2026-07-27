(function () {
  const tbody = document.getElementById("notice-list-tbody");
  const paginationEl = document.getElementById("noticePagination");
  
  if (!tbody) return;
  
  // ===== 더미 데이터 (API 연결 시 여기만 교체) =====
  const dummyNotices = [
    { id: "n-001", category: "학습", title: "[필독] 수강생 학습 유의사항 안내", createdAt: "2026-01-22", author: "운영팀", views: 184, pinned: true },
    { id: "n-002", category: "학습", title: "1월 과제 제출 마감 일정 안내", createdAt: "2026-01-20", author: "운영팀", views: 121, pinned: true },
    { id: "n-003", category: "기타", title: "튜터링/Q&A 운영 시간 안내", createdAt: "2026-01-18", author: "튜터팀", views: 62 },
    { id: "n-004", category: "시스템", title: "서비스 점검 안내 (01/27 02:00~03:00)", createdAt: "2026-01-17", author: "개발팀", views: 98 },
    { id: "n-005", category: "평가", title: "온라인 테스트 응시 가이드", createdAt: "2026-01-10", author: "평가팀", views: 77 },
  ];

  const PAGE_SIZE = 10;
  let currentPage = 1;

  function escapeHtml(str) {
    return String(str ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function sortNotices(list) {
    // 고정 먼저 + 최신순
    return [...list].sort((a, b) => {
      const ap = a.pinned ? 1 : 0;
      const bp = b.pinned ? 1 : 0;
      if (ap !== bp) return bp - ap;
      return (b.createdAt || "").localeCompare(a.createdAt || "");
    });
  }

  const notices = sortNotices(dummyNotices);

  function renderTable() {
    const pageList = notices;
    if (!pageList.length) {
      tbody.innerHTML = `<tr><td class="notice-empty" colspan="6">공지사항이 없습니다.</td></tr>`;
      return;
    }
    tbody.innerHTML = pageList.map((notice, idx) => `
      <tr data-id="${escapeHtml(notice.id)}">
        <td>${notice.pinned ? '<span title="상단고정">📌</span>' : (idx + 1)}</td>
        <td>${escapeHtml(notice.category)}</td>
        <td class="title-cell">${escapeHtml(notice.title)}</td>
        <td>${escapeHtml(notice.createdAt)}</td>
        <td>${escapeHtml(notice.author)}</td>
        <td>${notice.views}</td>
      </tr>
    `).join("");
    tbody.querySelectorAll("tr[data-id]").forEach(tr => {
      tr.addEventListener("click", () => {
        const id = tr.getAttribute("data-id");
        // 상세 페이지로 이동 (id 파라미터 포함)
        window.location.href = `/templates/trainee/notices-detail.html`;
      });
    });
  }

  // 페이지 로드 시 테이블 렌더링
  renderTable();

})();