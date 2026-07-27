 /**
   * 학생용 출결현황 페이지 (더미 데이터 기반)
   * - 출결상태: 출석/지각/결석/조퇴/병가
   * - 변경이력: 모달 내부에서만 표시
   * - 학생은 수정 불가(보기만)
   */

  // ====== 더미: 과정/기수 ======
  const mockCourses = [
    { id: "c1", name: "풀스택 웹 개발 (React & Node.js)", instructor: "홍길동" },
    { id: "c2", name: "AI 활용 실무 (Python)", instructor: "김철수" }
  ];

  const mockCohorts = [
    { id: "k1", name: "1기" },
    { id: "k2", name: "2기" }
  ];

  // ====== 더미: 출결 데이터 ======
  // status: ATTEND/LATE/ABSENT/LEAVE/SICK
  const mockAttendance = [
    {
      id: "a1",
      courseId: "c1",
      cohortId: "k1",
      sessionNo: 1,
      date: "2026-01-05",
      status: "ATTEND",
      totalMinutes: 75,
      studyMinutes: 70,
      pass: true,
      connectTime: "09:05 ~ 10:20",
      contents: [
        { name: "JavaScript 기초 - 변수와 자료형", percent: 100 },
        { name: "JavaScript 기초 - 연산자와 표현식", percent: 100 }
      ],
      history: [
        { at: "2026-01-05 10:30", who: "시스템", desc: "출석 처리 완료" }
      ]
    },
    {
      id: "a2",
      courseId: "c1",
      cohortId: "k1",
      sessionNo: 2,
      date: "2026-01-06",
      status: "LATE",
      totalMinutes: 60,
      studyMinutes: 48,
      pass: true,
      connectTime: "09:20 ~ 10:20",
      contents: [
        { name: "JavaScript 기초 - 조건문", percent: 80 },
        { name: "JavaScript 기초 - 반복문", percent: 75 }
      ],
      history: [
        { at: "2026-01-06 10:25", who: "시스템", desc: "지각 처리 완료" }
      ]
    },
    {
      id: "a3",
      courseId: "c1",
      cohortId: "k1",
      sessionNo: 3,
      date: "2026-01-07",
      status: "ABSENT",
      totalMinutes: 60,
      studyMinutes: 0,
      pass: false,
      connectTime: "-",
      contents: [
        { name: "JavaScript 기초 - 함수", percent: 0 },
        { name: "JavaScript 기초 - 스코프", percent: 0 }
      ],
      history: [
        { at: "2026-01-07 18:00", who: "시스템", desc: "미접속으로 결석 처리" }
      ]
    },
    {
      id: "a4",
      courseId: "c1",
      cohortId: "k1",
      sessionNo: 4,
      date: "2026-01-08",
      status: "LEAVE",
      totalMinutes: 60,
      studyMinutes: 35,
      pass: false,
      connectTime: "09:05 ~ 09:40",
      contents: [
        { name: "JavaScript 기초 - 배열", percent: 55 },
        { name: "JavaScript 기초 - 객체", percent: 20 }
      ],
      history: [
        { at: "2026-01-08 09:45", who: "시스템", desc: "중도 이탈로 조퇴 처리" }
      ]
    },
    {
      id: "a5",
      courseId: "c1",
      cohortId: "k1",
      sessionNo: 5,
      date: "2026-01-09",
      status: "SICK",
      totalMinutes: 60,
      studyMinutes: 0,
      pass: false,
      connectTime: "-",
      contents: [
        { name: "JavaScript 기초 - DOM 기초", percent: 0 },
        { name: "JavaScript 기초 - 이벤트", percent: 0 }
      ],
      history: [
        { at: "2026-01-09 09:10", who: "관리자A", desc: "결석 → 병가 변경 (사유: 진단서 확인)" }
      ]
    },
    {
      id: "a6",
      courseId: "c2",
      cohortId: "k2",
      sessionNo: 1,
      date: "2026-01-10",
      status: "ATTEND",
      totalMinutes: 80,
      studyMinutes: 80,
      pass: true,
      connectTime: "13:00 ~ 14:20",
      contents: [
        { name: "Python 기초 - 자료형", percent: 100 },
        { name: "Python 기초 - 연산", percent: 100 }
      ],
      history: [
        { at: "2026-01-10 14:25", who: "시스템", desc: "출석 처리 완료" }
      ]
    }
  ];

  // ====== State ======
  const state = {
    courseId: mockCourses[0].id,
    cohortId: mockCohorts[0].id,
    startDate: "",
    endDate: "",
    status: "ALL",
    keyword: "",
    sort: "DATE_DESC",
    rows: []
  };

  // ====== Elements ======
  const courseSelect = document.getElementById("courseSelect");
  const cohortSelect = document.getElementById("cohortSelect");
  const btnFetch = document.getElementById("btnFetch");

  const startDate = document.getElementById("startDate");
  const endDate = document.getElementById("endDate");
  const statusFilter = document.getElementById("statusFilter");
  const keywordInput = document.getElementById("keywordInput");
  const sortSelect = document.getElementById("sortSelect");
  const btnReset = document.getElementById("btnReset");

  const summaryGrid = document.getElementById("summaryGrid");
  const tbody = document.getElementById("attendanceTbody");
  const emptyState = document.getElementById("emptyState");

  const backdrop = document.getElementById("detailBackdrop");
  const btnCloseModal = document.getElementById("btnCloseModal");
  const btnCloseModal2 = document.getElementById("btnCloseModal2");

  // Modal fields
  const dCourseName = document.getElementById("dCourseName");
  const dInstructor = document.getElementById("dInstructor");
  const dSession = document.getElementById("dSession");
  const dDate = document.getElementById("dDate");
  const dStatus = document.getElementById("dStatus");
  const dConnectTime = document.getElementById("dConnectTime");
  const dTotal = document.getElementById("dTotal");
  const dStudy = document.getElementById("dStudy");
  const dPass = document.getElementById("dPass");
  const detailSub = document.getElementById("detailSub");
  const contentList = document.getElementById("contentList");
  const historyList = document.getElementById("historyList");

  // ====== Helpers ======
  function minutesToText(min) {
    if (typeof min !== "number") return "-";
    const h = Math.floor(min / 60);
    const m = min % 60;
    if (h <= 0) return `${m}분`;
    if (m === 0) return `${h}시간`;
    return `${h}시간 ${m}분`;
  }

  function statusLabel(status) {
    switch (status) {
      case "ATTEND": return "출석";
      case "LATE": return "지각";
      case "ABSENT": return "결석";
      case "LEAVE": return "조퇴";
      case "SICK": return "병가";
      default: return "-";
    }
  }

  function statusBadgeClass(status) {
    switch (status) {
      case "ATTEND": return "attend";
      case "LATE": return "late";
      case "ABSENT": return "absent";
      case "LEAVE": return "leave";
      case "SICK": return "sick";
      default: return "";
    }
  }

  function withinDateRange(dateStr, start, end) {
    if (!dateStr) return false;
    const d = new Date(dateStr + "T00:00:00");
    if (start) {
      const s = new Date(start + "T00:00:00");
      if (d < s) return false;
    }
    if (end) {
      const e = new Date(end + "T23:59:59");
      if (d > e) return false;
    }
    return true;
  }

  function includesKeyword(row, keyword) {
    if (!keyword) return true;
    const k = keyword.trim().toLowerCase();
    const hay = [
      String(row.sessionNo),
      row.date,
      statusLabel(row.status),
      ...(row.contents || []).map(c => c.name)
    ].join(" ").toLowerCase();
    return hay.includes(k);
  }

  function sortRows(rows, sort) {
    const copy = [...rows];
    if (sort === "DATE_DESC") {
      copy.sort((a, b) => (b.date || "").localeCompare(a.date || ""));
    } else if (sort === "DATE_ASC") {
      copy.sort((a, b) => (a.date || "").localeCompare(b.date || ""));
    } else if (sort === "SESSION_ASC") {
      copy.sort((a, b) => (a.sessionNo || 0) - (b.sessionNo || 0));
    } else if (sort === "SESSION_DESC") {
      copy.sort((a, b) => (b.sessionNo || 0) - (a.sessionNo || 0));
    }
    return copy;
  }

  function getSelectedCourse() {
    return mockCourses.find(c => c.id === state.courseId);
  }

  // ====== Render ======
  function renderSummary(rows) {
    const counts = { ATTEND: 0, LATE: 0, ABSENT: 0, LEAVE: 0, SICK: 0 };
    const total = rows.length;
    let passed = 0;

    rows.forEach(r => {
      if (counts[r.status] !== undefined) counts[r.status] += 1;
      if (r.pass) passed += 1;
    });

    const rate = total === 0 ? 0 : Math.round((counts.ATTEND / total) * 100);

    const cards = [
      { label: "전체 차시", value: total, sub: "선택 과정/기수 기준" },
      { label: "출석", value: counts.ATTEND, sub: "정상 출석" },
      { label: "지각", value: counts.LATE, sub: "지각 처리" },
      { label: "결석", value: counts.ABSENT, sub: "미접속/미이수" },
      { label: "조퇴", value: counts.LEAVE, sub: "중도 이탈" },
      { label: "병가", value: counts.SICK, sub: "예외 처리" }
    ];

    summaryGrid.innerHTML = cards.map(c => `
      <div class="summary-card">
        <div class="summary-label">${c.label}</div>
        <div class="summary-value">${c.value}</div>
        <div class="summary-sub">${c.sub}</div>
      </div>
    `).join("");

    // 추가 정보는 첫 카드 하단 문구로 노출(간단히)
    const first = summaryGrid.querySelector(".summary-card");
    if (first) {
      const extra = document.createElement("div");
      extra.className = "summary-sub";
      extra.style.marginTop = "6px";
      extra.textContent = `출석률(출석/전체): ${rate}% · 기준 충족: ${passed}/${total}`;
      first.appendChild(extra);
    }
  }

  function renderTable(rows) {
    if (!rows || rows.length === 0) {
      tbody.innerHTML = "";
      emptyState.style.display = "block";
      return;
    }
    emptyState.style.display = "none";

    tbody.innerHTML = rows.map(r => `
      <tr>
        <td>${r.sessionNo}차시</td>
        <td>${r.date || "-"}</td>
        <td>
          <span class="badge ${statusBadgeClass(r.status)}">${statusLabel(r.status)}</span>
        </td>
        <td>${minutesToText(r.totalMinutes)}</td>
        <td>${minutesToText(r.studyMinutes)}</td>
        <td>
          <span class="pass ${r.pass ? "o" : "x"}">${r.pass ? "O" : "X"}</span>
        </td>
        <td>
          <button type="button" class="link-btn" data-detail="${r.id}">보기</button>
        </td>
      </tr>
    `).join("");

    // detail 버튼 바인딩
    tbody.querySelectorAll("[data-detail]").forEach(btn => {
      btn.addEventListener("click", () => {
        const id = btn.getAttribute("data-detail");
        const row = rows.find(x => x.id === id);
        if (row) openDetail(row);
      });
    });
  }

  function renderModalContents(row) {
    const course = getSelectedCourse();
    dCourseName.textContent = course ? course.name : "-";
    dInstructor.textContent = course ? course.instructor : "-";
    dSession.textContent = `${row.sessionNo}차시`;
    dDate.textContent = row.date || "-";
    dStatus.textContent = statusLabel(row.status);
    dConnectTime.textContent = row.connectTime || "-";
    dTotal.textContent = minutesToText(row.totalMinutes);
    dStudy.textContent = minutesToText(row.studyMinutes);
    dPass.textContent = row.pass ? "O" : "X";

    detailSub.textContent = `${row.date || "-"} · ${row.sessionNo}차시 · ${statusLabel(row.status)}`;

    // 콘텐츠별 수강률
    const contents = row.contents || [];
    contentList.innerHTML = contents.length === 0
      ? `<div class="history-item"><div class="history-desc">콘텐츠 정보가 없어요.</div></div>`
      : contents.map(c => {
          const pct = Math.max(0, Math.min(100, Number(c.percent ?? 0)));
          return `
            <div class="content-row">
              <div>
                <div class="content-name">${c.name}</div>
              </div>
              <div>
                <div class="progress"><span style="width:${pct}%"></span></div>
                <div class="percent">${pct}%</div>
              </div>
            </div>
          `;
        }).join("");

    // 변경이력
    const history = row.history || [];
    historyList.innerHTML = history.length === 0
      ? `<div class="history-item"><div class="history-desc">변경이력이 없어요.</div></div>`
      : history.map(h => `
          <div class="history-item">
            <div class="history-top">
              <span class="history-date">${h.at}</span>
              <span class="history-who">${h.who}</span>
            </div>
            <div class="history-desc">${h.desc}</div>
          </div>
        `).join("");
  }

  // ====== Modal Open/Close ======
  function openDetail(row) {
    renderModalContents(row);
    backdrop.style.display = "flex";
    backdrop.setAttribute("aria-hidden", "false");
    document.body.style.overflow = "hidden";
  }

  function closeDetail() {
    backdrop.style.display = "none";
    backdrop.setAttribute("aria-hidden", "true");
    document.body.style.overflow = "";
  }

  // ====== Data pipeline ======
  function applyFilters() {
    let rows = mockAttendance.filter(r =>
      r.courseId === state.courseId && r.cohortId === state.cohortId
    );

    // 기간
    rows = rows.filter(r => withinDateRange(r.date, state.startDate, state.endDate));

    // 상태
    if (state.status !== "ALL") {
      rows = rows.filter(r => r.status === state.status);
    }

    // 키워드
    rows = rows.filter(r => includesKeyword(r, state.keyword));

    // 정렬
    rows = sortRows(rows, state.sort);

    state.rows = rows;
  }

  function renderAll() {
    applyFilters();
    renderSummary(state.rows);
    renderTable(state.rows);
  }

  // ====== Init ======
  function initSelectOptions() {
    courseSelect.innerHTML = mockCourses.map(c => `<option value="${c.id}">${c.name}</option>`).join("");
    cohortSelect.innerHTML = mockCohorts.map(k => `<option value="${k.id}">${k.name}</option>`).join("");

    courseSelect.value = state.courseId;
    cohortSelect.value = state.cohortId;
  }

  function bindEvents() {
    courseSelect.addEventListener("change", () => {
      state.courseId = courseSelect.value;
    });
    cohortSelect.addEventListener("change", () => {
      state.cohortId = cohortSelect.value;
    });

    btnFetch.addEventListener("click", () => {
      renderAll();
    });

    startDate.addEventListener("change", () => {
      state.startDate = startDate.value;
      renderAll();
    });
    endDate.addEventListener("change", () => {
      state.endDate = endDate.value;
      renderAll();
    });

    statusFilter.addEventListener("change", () => {
      state.status = statusFilter.value;
      renderAll();
    });

    keywordInput.addEventListener("input", () => {
      state.keyword = keywordInput.value || "";
      renderAll();
    });

    sortSelect.addEventListener("change", () => {
      state.sort = sortSelect.value;
      renderAll();
    });

    btnReset.addEventListener("click", () => {
      startDate.value = "";
      endDate.value = "";
      statusFilter.value = "ALL";
      keywordInput.value = "";
      sortSelect.value = "DATE_DESC";

      state.startDate = "";
      state.endDate = "";
      state.status = "ALL";
      state.keyword = "";
      state.sort = "DATE_DESC";

      renderAll();
    });

    // 모달 닫기
    btnCloseModal.addEventListener("click", closeDetail);
    btnCloseModal2.addEventListener("click", closeDetail);

    // 백드랍 클릭 닫기
    backdrop.addEventListener("click", (e) => {
      if (e.target === backdrop) closeDetail();
    });

    // ESC 닫기
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && backdrop.style.display === "flex") closeDetail();
    });
  }

  initSelectOptions();
  bindEvents();
  renderAll();