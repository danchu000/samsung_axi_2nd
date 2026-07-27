
  /**
   * ✅ 타임리프 전환 포인트
   * 1) exam, questions를 서버에서 내려주기
   *    - th:inline="javascript" 사용 가능
   *    const exam = /*[[${exam}]]*\/ {...}
   *    const questions = /*[[${questions}]]*\/ [...]
   * 2) 또는 data-*로 HTML에 심고 JS에서 파싱
   */

  // ===== 더미 데이터 (나중에 서버값으로 교체) =====
  const exam = {
    id: "EX-001",
    title: "JavaScript 중간평가",
    notice: "제한시간 내 제출 / 제출 후 수정 불가",
    timeLimitSec: 45 * 60
  };

  // type: single_choice | multi_choice | short | essay
  const questions = [
    {
      id: "Q1",
      no: 1,
      type: "single_choice",
      stem: "다음 중 JavaScript의 기본 자료형이 아닌 것은?",
      options: [
        { id: "A", text: "string" },
        { id: "B", text: "number" },
        { id: "C", text: "boolean" },
        { id: "D", text: "character" }
      ]
    },
    {
      id: "Q2",
      no: 2,
      type: "short",
      stem: "let과 const의 차이를 한 문장으로 작성하세요."
    },
    {
      id: "Q3",
      no: 3,
      type: "multi_choice",
      stem: "다음 중 배열 메서드인 것을 모두 고르세요.",
      options: [
        { id: "A", text: "map" },
        { id: "B", text: "filter" },
        { id: "C", text: "join" },
        { id: "D", text: "push" }
      ]
    },
    {
      id: "Q4",
      no: 4,
      type: "essay",
      stem: "이벤트 버블링/캡처링을 예시와 함께 설명하세요."
    }
  ];

  // ===== 상태 =====
  let currentIndex = 0;

  // answers[qid] =
  //  - single_choice: "A"
  //  - multi_choice: ["A","C"]
  //  - short/essay: "텍스트"
  const answers = {};

  // marked[qid] = true/false (검토 표시)
  const marked = {};

  // ===== 유틸 =====
  function $(id){ return document.getElementById(id); }

  function formatTime(sec){
    const s = Math.max(0, sec);
    const hh = String(Math.floor(s / 3600)).padStart(2, "0");
    const mm = String(Math.floor((s % 3600) / 60)).padStart(2, "0");
    const ss = String(s % 60).padStart(2, "0");
    return `${hh}:${mm}:${ss}`;
  }

  function isAnswered(q){
    const v = answers[q.id];
    if (q.type === "single_choice") return !!v;
    if (q.type === "multi_choice") return Array.isArray(v) && v.length > 0;
    if (q.type === "short" || q.type === "essay") return (v || "").trim().length > 0;
    return false;
  }

  // ===== 렌더: 좌측 네비 =====
  function renderNav(){
    const nav = $("navGrid");
    nav.innerHTML = "";

    questions.forEach((q, idx) => {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "nav-btn";

      if (idx === currentIndex) btn.classList.add("current");
      if (isAnswered(q)) btn.classList.add("answered");
      if (marked[q.id]) btn.classList.add("marked");

      btn.textContent = String(q.no).padStart(2, "0");
      btn.addEventListener("click", () => {
        currentIndex = idx;
        renderAll();
      });
      nav.appendChild(btn);
    });
  }

  // ===== 렌더: 우측 문항 + 답안 =====
  function renderQuestion(){
    const q = questions[currentIndex];

    $("qNo").textContent = `Q${q.no}`;
    $("qStem").textContent = q.stem;

    // 진행 표시
    $("currentNo").textContent = q.no;
    $("totalNo").textContent = questions.length;

    // 검토 체크
    $("markCheck").checked = !!marked[q.id];
    $("markCheck").onchange = (e) => {
      marked[q.id] = e.target.checked;
      renderNav();
    };

    // 답안 영역 렌더
    const area = $("answerArea");
    area.innerHTML = "";

    if (q.type === "single_choice"){
      // 라디오
      (q.options || []).forEach(opt => {
        const row = document.createElement("label");
        row.className = "opt";
        row.innerHTML = `
          <input type="radio" name="singleChoice" value="${opt.id}">
          <div class="opt-text">${opt.id}. ${opt.text}</div>
        `;
        const input = row.querySelector("input");
        input.checked = answers[q.id] === opt.id;
        input.addEventListener("change", () => {
          answers[q.id] = opt.id;
          setSavedState("저장됨");
          renderNav();
        });
        area.appendChild(row);
      });
    }

    if (q.type === "multi_choice"){
      // 체크박스
      const current = Array.isArray(answers[q.id]) ? answers[q.id] : [];
      (q.options || []).forEach(opt => {
        const row = document.createElement("label");
        row.className = "opt";
        row.innerHTML = `
          <input type="checkbox" value="${opt.id}">
          <div class="opt-text">${opt.id}. ${opt.text}</div>
        `;
        const input = row.querySelector("input");
        input.checked = current.includes(opt.id);
        input.addEventListener("change", () => {
          const v = Array.isArray(answers[q.id]) ? answers[q.id] : [];
          if (input.checked) {
            if (!v.includes(opt.id)) v.push(opt.id);
          } else {
            const i = v.indexOf(opt.id);
            if (i >= 0) v.splice(i, 1);
          }
          answers[q.id] = v;
          setSavedState("저장됨");
          renderNav();
        });
        area.appendChild(row);
      });
    }

    if (q.type === "short"){
      const input = document.createElement("input");
      input.className = "text-input";
      input.placeholder = "답안을 입력하세요.";
      input.value = answers[q.id] || "";
      input.addEventListener("input", () => {
        answers[q.id] = input.value;
        setSavedState("저장중...");
        debounceSaved();
      });
      area.appendChild(input);
    }

    if (q.type === "essay"){
      const ta = document.createElement("textarea");
      ta.className = "textarea";
      ta.rows = 10;
      ta.placeholder = "답안을 작성하세요.";
      ta.value = answers[q.id] || "";
      ta.addEventListener("input", () => {
        answers[q.id] = ta.value;
        setSavedState("저장중...");
        debounceSaved();
      });
      area.appendChild(ta);
    }

    // 버튼 상태
    $("prevBtn").disabled = currentIndex === 0;
    $("nextBtn").disabled = currentIndex === questions.length - 1;
  }

  // ===== 저장 상태 (표시만) =====
  let saveTimer = null;
  function setSavedState(text){
    $("saveState").textContent = text;
  }
  function debounceSaved(){
    if (saveTimer) clearTimeout(saveTimer);
    saveTimer = setTimeout(() => setSavedState("저장됨"), 400);
  }

  // ===== 전체 렌더 =====
  function renderAll(){
    renderNav();
    renderQuestion();
  }

  // ===== 이동 버튼 =====
  function bindNavButtons(){
    $("prevBtn").addEventListener("click", () => {
      if (currentIndex > 0){
        currentIndex--;
        renderAll();
      }
    });
    $("nextBtn").addEventListener("click", () => {
      if (currentIndex < questions.length - 1){
        currentIndex++;
        renderAll();
      }
    });
  }

  // ===== 임시저장 / 제출 =====
  function bindSubmitButtons(){
    $("tempSaveBtn").addEventListener("click", () => {
      // TODO: 서버로 answers/marked 저장 (fetch)
      setSavedState("저장됨");
      alert("임시저장 완료(데모)");
    });

    $("submitBtn").addEventListener("click", () => {
      const unanswered = questions.filter(q => !isAnswered(q)).map(q => q.no);
      if (unanswered.length > 0){
        const ok = confirm(`미응답 문항이 ${unanswered.length}개 있습니다. 제출할까요?\n(미응답: ${unanswered.join(", ")})`);
        if (!ok) return;
      } else {
        const ok = confirm("제출 후 수정할 수 없습니다. 제출할까요?");
        if (!ok) return;
      }

      // TODO: 서버 제출 API 호출 (fetch)
      alert("제출 완료(데모)");
    });
  }

  // ===== 타이머 =====
  let remain = exam.timeLimitSec;
  let tick = null;

  function startTimer(){
    $("remainTime").textContent = formatTime(remain);

    tick = setInterval(() => {
      remain--;
      $("remainTime").textContent = formatTime(remain);

      if (remain <= 0){
        clearInterval(tick);
        setSavedState("시간 종료");
        alert("시험 시간이 종료되었습니다. 제출 처리합니다. (데모)");
        // TODO: 자동 제출
      }
    }, 1000);
  }

  // ===== 초기화 =====



  document.addEventListener("DOMContentLoaded", () => {
    // 상단 정보
    $("examTitle").textContent = exam.title;
    $("examNotice").textContent = exam.notice;
    $("totalNo").textContent = questions.length;

    bindNavButtons();
    bindSubmitButtons();

    renderAll();
    startTimer();
    applyRoleBasedButtonDisabling();
  });

