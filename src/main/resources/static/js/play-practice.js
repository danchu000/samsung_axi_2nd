
         // 서버(타임리프)에서 내려줄 값이라고 생각하면 됨
  // document | link | text | document_text
  const assignment = {
    type: "document",
    title: "[과제] 객체지향 프로그래밍 실습",
    description: "아래 실습 문서를 참고하여 객체지향 프로그래밍의 주요 개념(클래스, 상속, 캡슐화, 다형성)을 코드로 구현해보세요.",
    dueDate: "2026-02-10 23:59",
    file: {
      name: "OOP-Practice.pdf",
      url: "#"
    },
    submitStatus: "미제출",
    feedback: "제출 후 피드백이 제공됩니다."
  };

    function applyType(type) {
        // 1) 모든 제출 블록 숨김
        document.querySelectorAll(".submit-block").forEach(block => {
            block.style.display = "none";
            // 숨길 때 내부 입력값 초기화(선택)
            block.querySelectorAll("input, textarea").forEach(el => {
                if (el.type === "file") el.value = "";
                else el.value = "";
            });
        });

        // 2) 해당 타입만 보여줌
        const target = document.querySelector(`.submit-block[data-type="${type}"]`);
        if (target) target.style.display = "block";
    }

    function renderAssignmentDesc() {
        const descEl = document.getElementById('assignmentDesc');
        if (!descEl) return;
        descEl.innerHTML = `
            <strong>과제 안내:</strong><br>
            ${assignment.description}<br>
            <span style="color:#888; font-size:0.98em;">제출 기한: ${assignment.dueDate}</span>
        `;
    }

    document.addEventListener("DOMContentLoaded", () => {
        // 과제 설명 동적 렌더링
        renderAssignmentDesc();
        // 초기 타입 적용
        applyType(assignment.type);

        // (테스트용) select로 타입 바꾸기
        const typeSelect = document.getElementById("typeSelect");
        if (typeSelect) {
            typeSelect.value = assignment.type;
            typeSelect.addEventListener("change", (e) => {
                assignment.type = e.target.value;
                applyType(assignment.type);
            });
        }
    });

