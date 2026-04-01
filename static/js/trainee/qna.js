(function () {
  const $ = (id) => document.getElementById(id);

  function openModal(el) {
    if (!el) return;
    el.classList.add("open");
    el.setAttribute("aria-hidden", "false");
    document.body.style.overflow = "hidden";
  }

  function closeModal(el) {
    if (!el) return;
    el.classList.remove("open");
    el.setAttribute("aria-hidden", "true");
    document.body.style.overflow = "";
  }

  document.addEventListener("DOMContentLoaded", () => {

    // ===== 1️⃣ 질문하기 모달 =====
    const btnOpenAsk = $("btnOpenAsk");
    const askModal = $("askModal");
    const btnSubmitAsk = $("btnSubmitAsk");

    const aTitle = $("aTitle");
    const aBody = $("aBody");
    const askError = $("askError");

    btnOpenAsk?.addEventListener("click", () => {
      if (askError) {
        askError.hidden = true;
        askError.textContent = "";
      }
      openModal(askModal);
    });

    // 모달 바깥 클릭 닫기
    askModal?.addEventListener("click", (e) => {
      if (e.target === askModal) closeModal(askModal);
    });

    // 닫기 버튼
    document.querySelectorAll('[data-close="askModal"]').forEach((btn) => {
      btn.addEventListener("click", () => closeModal(askModal));
    });

    // ESC 닫기
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && askModal?.classList.contains("open")) {
        closeModal(askModal);
      }
    });

    // ===== 2️⃣ 제목/내용 유효성 검사 =====
    btnSubmitAsk?.addEventListener("click", () => {
      const title = aTitle?.value.trim() || "";
      const body = aBody?.value.trim() || "";

      if (!title) {
        showError("제목을 입력해주세요.");
        return;
      }

      if (!body) {
        showError("내용을 입력해주세요.");
        return;
      }

      // 통과 시 (지금은 데모)
      closeModal(askModal);
      alert("등록(데모): 서버 연동 필요");
    });

    function showError(message) {
      if (!askError) return;
      askError.hidden = false;
      askError.textContent = message;
    }

    // ===== 3️⃣ tr 클릭 → 상세 페이지 이동 =====
    const rows = document.querySelectorAll("#qnaTbody tr");

    rows.forEach((row) => {
      row.style.cursor = "pointer";

      row.addEventListener("click", () => {
        window.location.href = "./qna-detail.html";

        // ID 기반으로 바꾸려면:
        // const id = row.getAttribute("data-item-id");
        // window.location.href = "/trainee/qna/" + id;
      });
    });

  });
})();