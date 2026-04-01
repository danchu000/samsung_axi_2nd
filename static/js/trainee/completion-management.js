 /**
   * 학생용 이수관리(종료된 과정만)
   * - 과정 단위 이수 확인
   * - 상세 모달에서 출결현황 이동 링크 제공
   * - 출력 버튼은 "최종판정 확정"일 때만 활성
   */

  // ====== 더미: 종료된 과정 목록(학생에게 보이는 단위) ======
  const mockCompletions = [
    {
      id: "cc1",
      courseName: "풀스택 웹 개발 (React & Node.js)",
      cohortName: "1기",
      instructor: "홍길동",
      startDate: "2025-12-01",
      endDate: "2026-01-20",
      attendanceRate: 92,
      completionStatus: "PASS",   // PASS/FAIL/PENDING
      finalDecision: "CONFIRMED", // CONFIRMED/UNCONFIRMED
      summary: {
        totalSessions: 40,
        attend: 35,
        late: 2,
        absent: 2,
        leave: 1,
        sick: 0,
        passSessions: 36,
        failSessions: 4
      },
      ruleText: "출결률 80% 이상 AND 기준 미충족 차시 5회 이하",
      ruleResult: "충족"
    },
    {
      id: "cc2",
      courseName: "AI 활용 실무 (Python)",
      cohortName: "2기",
      instructor: "김철수",
      startDate: "2025-10-10",
      endDate: "2026-01-05",
      attendanceRate: 74,
      completionStatus: "FAIL",
      finalDecision: "CONFIRMED",
      summary: {
        totalSessions: 30,
        attend: 20,
        late: 1,
        absent: 6,
        leave: 2,
        sick: 1,
        passSessions: 22,
        failSessions: 8
      },
      ruleText: "출결률 80% 이상 AND 기준 미충족 차시 5회 이하",
      ruleResult: "미충족"
    },
    {
      id: "cc3",
      courseName: "ESG 실무 과정",
      cohortName: "1기",
      instructor: "이영희",
      startDate: "2025-07-23",
      endDate: "2026-01-22",
      attendanceRate: 86,
      completionStatus: "PENDING",
      finalDecision: "UNCONFIRMED",
      summary: {
        totalSessions: 28,
        attend: 22,
        late: 2,
        absent: 2,
        leave: 1,
        sick: 1,
        passSessions: 24,
        failSessions: 4
      },
      ruleText: "출결률 80% 이상 AND 기준 미충족 차시 5회 이하",
      ruleResult: "검토 중"
    }
  ];

  // ====== State ======
  const s2 = {
    status: "ALL",
    endStart: "",
    endEnd: "",
    keyword: "",
    sort: "END_DESC",
    rows: []
  };

  // ====== Elements ======
  const statusFilter2 = document.getElementById("statusFilter2");
  const endStart = document.getElementById("endStart");
  const endEnd = document.getElementById("endEnd");
  const keyword2 = document.getElementById("keyword2");
  const sort2 = document.getElementById("sort2");
  const btnReset2 = document.getElementById("btnReset2");

  const completionTbody = document.getElementById("completionTbody");
  const emptyState2 = document.getElementById("emptyState2");

  // Modal elements
  const completionBackdrop = document.getElementById("completionBackdrop");
  const btnCloseCompletion = document.getElementById("btnCloseCompletion");
  const btnCloseCompletion2 = document.getElementById("btnCloseCompletion2");
  const completionSub = document.getElementById("completionSub");

  const cCourse = document.getElementById("cCourse");
  const cCohort = document.getElementById("cCohort");
  const cPeriod = document.getElementById("cPeriod");
  const cInstructor = document.getElementById("cInstructor");
  const cStatus = document.getElementById("cStatus");
  const cFinal = document.getElementById("cFinal");

  const summaryRows2 = document.getElementById("summaryRows2");
  const cRule = document.getElementById("cRule");
  const cRuleResult = document.getElementById("cRuleResult");

  const btnGoAttendance = document.getElementById("btnGoAttendance");
  const certDesc = document.getElementById("certDesc");
  const btnPrintCert = document.getElementById("btnPrintCert");

  let currentDetail = null;

  // ====== Helpers ======
  function dateText(s, e) {
    return `${s || "-"} ~ ${e || "-"}`;
  }

  function statusLabel2(v) {
    if (v === "PASS") return "이수";
    if (v === "FAIL") return "미이수";
    if (v === "PENDING") return "대기";
    return "-";
  }

  function statusBadgeClass2(v) {
    if (v === "PASS") return "pass";
    if (v === "FAIL") return "fail";
    if (v === "PENDING") return "pending";
    return "";
  }

  function finalLabel(v) {
    return v === "CONFIRMED" ? "확정" : "미확정";
  }

  function withinEndRange(endDateStr, start, end) {
    if (!endDateStr) return false;
    const d = new Date(endDateStr + "T00:00:00");
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

  function matchKeyword(row, keyword) {
    if (!keyword) return true;
    const k = keyword.trim().toLowerCase();
    const hay = `${row.courseName} ${row.cohortName}`.toLowerCase();
    return hay.includes(k);
  }

  function sortRows2(rows, sort) {
    const copy = [...rows];
    if (sort === "END_DESC") copy.sort((a, b) => (b.endDate || "").localeCompare(a.endDate || ""));
    if (sort === "END_ASC") copy.sort((a, b) => (a.endDate || "").localeCompare(b.endDate || ""));
    if (sort === "RATE_DESC") copy.sort((a, b) => (b.attendanceRate || 0) - (a.attendanceRate || 0));
    if (sort === "RATE_ASC") copy.sort((a, b) => (a.attendanceRate || 0) - (b.attendanceRate || 0));
    return copy;
  }

  // ====== Render ======
  function renderTable2(rows) {
    if (!rows || rows.length === 0) {
      completionTbody.innerHTML = "";
      emptyState2.style.display = "block";
      return;
    }
    emptyState2.style.display = "none";

    completionTbody.innerHTML = rows.map((r, idx) => `
      <tr>
        <td>${idx + 1}</td>
        <td>
          <div style="color:#111;">${r.courseName}</div>
          <div style="font-size:12px;color:#777;margin-top:4px;">${r.cohortName}</div>
        </td>
        <td>${dateText(r.startDate, r.endDate)}</td>
        <td>${r.attendanceRate ?? 0}</td>
        <td>
          <span class="badge ${statusBadgeClass2(r.completionStatus)}">${statusLabel2(r.completionStatus)}</span>
        </td>
        <td>
          <span class="badge final">${finalLabel(r.finalDecision)}</span>
        </td>
        <td>
          <button type="button" class="link-btn" data-detail2="${r.id}">보기</button>
        </td>
      </tr>
    `).join("");

    completionTbody.querySelectorAll("[data-detail2]").forEach(btn => {
      btn.addEventListener("click", () => {
        const id = btn.getAttribute("data-detail2");
        const row = rows.find(x => x.id === id);
        if (row) openCompletion(row);
      });
    });
  }

  function renderModal2(row) {
    currentDetail = row;

    cCourse.textContent = row.courseName || "-";
    cCohort.textContent = row.cohortName || "-";
    cPeriod.textContent = dateText(row.startDate, row.endDate);
    cInstructor.textContent = row.instructor || "-";
    cStatus.textContent = statusLabel2(row.completionStatus);
    cFinal.textContent = finalLabel(row.finalDecision);

    completionSub.textContent = `${row.endDate || "-"} 종료 · ${row.courseName} · ${row.cohortName}`;

    // 요약
    const s = row.summary || {};
    const summaryPairs = [
      ["총 차시", `${s.totalSessions ?? "-"}회`],
      ["출석", `${s.attend ?? 0}회`],
      ["지각", `${s.late ?? 0}회`],
      ["결석", `${s.absent ?? 0}회`],
      ["조퇴", `${s.leave ?? 0}회`],
      ["병가", `${s.sick ?? 0}회`],
      ["기준충족 차시", `${s.passSessions ?? 0}회`],
      ["미충족 차시", `${s.failSessions ?? 0}회`]
    ];

    summaryRows2.innerHTML = summaryPairs.map(([k, v]) => `
      <div class="summary-row">
        <div class="k">${k}</div>
        <div class="v">${v}</div>
      </div>
    `).join("");

    // 기준
    cRule.textContent = row.ruleText || "-";
    cRuleResult.textContent = row.ruleResult || "-";

    // 이수증 버튼 활성 조건: 최종판정 확정 + 이수(PASS)
    const canPrint = row.finalDecision === "CONFIRMED" && row.completionStatus === "PASS";
    btnPrintCert.disabled = !canPrint;

    if (canPrint) {
      certDesc.textContent = "최종판정이 확정되어 이수증을 출력할 수 있어요.";
    } else {
      const reason =
        row.finalDecision !== "CONFIRMED" ? "최종판정이 아직 미확정 상태예요." :
        row.completionStatus !== "PASS" ? "이수 상태가 '이수'가 아니어서 출력할 수 없어요." :
        "현재 상태에서는 출력할 수 없어요.";
      certDesc.textContent = `이수증 출력이 비활성화되어 있어요. (${reason})`;
    }
  }

  // ====== Filter apply ======
  function applyFilters2() {
    // 종료된 과정만: mockCompletions 자체가 종료된 과정이라고 가정
    let rows = [...mockCompletions];

    // 상태
    if (s2.status !== "ALL") rows = rows.filter(r => r.completionStatus === s2.status);

    // 종료일 범위
    rows = rows.filter(r => withinEndRange(r.endDate, s2.endStart, s2.endEnd));

    // 키워드
    rows = rows.filter(r => matchKeyword(r, s2.keyword));

    // 정렬
    rows = sortRows2(rows, s2.sort);

    s2.rows = rows;
  }

  function renderAll2() {
    applyFilters2();
    renderTable2(s2.rows);
  }

  // ====== Modal open/close ======
  function openCompletion(row) {
    renderModal2(row);
    completionBackdrop.style.display = "flex";
    completionBackdrop.setAttribute("aria-hidden", "false");
    document.body.style.overflow = "hidden";
  }

  function closeCompletion() {
    completionBackdrop.style.display = "none";
    completionBackdrop.setAttribute("aria-hidden", "true");
    document.body.style.overflow = "";
    currentDetail = null;
  }

  // ====== Events ======
  statusFilter2.addEventListener("change", () => {
    s2.status = statusFilter2.value;
    renderAll2();
  });

  endStart.addEventListener("change", () => {
    s2.endStart = endStart.value;
    renderAll2();
  });

  endEnd.addEventListener("change", () => {
    s2.endEnd = endEnd.value;
    renderAll2();
  });

  keyword2.addEventListener("input", () => {
    s2.keyword = keyword2.value || "";
    renderAll2();
  });

  sort2.addEventListener("change", () => {
    s2.sort = sort2.value;
    renderAll2();
  });

  btnReset2.addEventListener("click", () => {
    statusFilter2.value = "ALL";
    endStart.value = "";
    endEnd.value = "";
    keyword2.value = "";
    sort2.value = "END_DESC";

    s2.status = "ALL";
    s2.endStart = "";
    s2.endEnd = "";
    s2.keyword = "";
    s2.sort = "END_DESC";

    renderAll2();
  });

  btnCloseCompletion.addEventListener("click", closeCompletion);
  btnCloseCompletion2.addEventListener("click", closeCompletion);

  completionBackdrop.addEventListener("click", (e) => {
    if (e.target === completionBackdrop) closeCompletion();
  });

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && completionBackdrop.style.display === "flex") closeCompletion();
  });

  // 출결현황 이동 링크
  btnGoAttendance.addEventListener("click", () => {
    // 프로젝트 라우팅 규칙에 맞게 경로만 바꾸면 됨
    // 예) location.href = '/trainee/attendance';
    // 여기서는 예시로 상대 이동 처리
    location.href = "./attendance.html";
  });

  // 이수증 출력(예시)
  btnPrintCert.addEventListener("click", () => {
    if (!currentDetail) return;
    // 실제 구현에서는 PDF 생성/다운로드 or 새 창 출력 등을 연결
    alert(`이수증 출력: ${currentDetail.courseName} / ${currentDetail.cohortName}`);
  });

  // init
  renderAll2();