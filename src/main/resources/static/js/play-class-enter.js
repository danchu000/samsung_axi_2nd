function toggleCommPanel() {
    const panel = document.getElementById('commPanel');
    const lectureArea = document.getElementById('lectureArea');
    const btn = document.getElementById('commToggleBtn');
    const isClosed = panel.classList.toggle('closed');
    lectureArea.style.marginRight = isClosed ? '0' : '340px';
    btn.textContent = isClosed ? '우측열기' : '우측접기';
    }
        function sendCommMessage(type) {
            let input, content;
            if (type === 'chat') {
                input = document.getElementById('commChatInput');
                content = document.getElementById('commChat');
                const value = input.value.trim();
                if (!value) return;
                const bubble = document.createElement('div');
                bubble.className = 'chat-bubble';
                bubble.textContent = value;
                content.appendChild(bubble);
                content.scrollTop = content.scrollHeight;
                input.value = '';
                input.focus();
            }
        }

    function saveMemo() {
    const textarea = document.getElementById('commMemoTextarea');
    const value = textarea.value.trim();
    if (!value) return;
        // 서버 저장 로직 필요
        alert('메모가 저장되었습니다. (실제 저장은 서버 연동 필요)');
    textarea.value = '';
    textarea.focus();
    }

    // 예시 학생 데이터
  const students = [
    { name: '홍길동', status: '출석', img: '/static/imgs/profile-default.png' },
    { name: '김철수', status: '지각', img: '/static/imgs/profile-default.png' },
    { name: '이영희', status: '결석', img: '/static/imgs/profile-default.png' },
    { name: '박민수', status: '출석', img: '/static/imgs/profile-default.png' },
    { name: '최지우', status: '출석', img: '/static/imgs/profile-default.png' },
    { name: '정하늘', status: '지각', img: '/static/imgs/profile-default.png' },
    { name: '오세훈', status: '출석', img: '/static/imgs/profile-default.png' },
    { name: '유수연', status: '출석', img: '/static/imgs/profile-default.png' },
    { name: '한가람', status: '결석', img: '/static/imgs/profile-default.png' }
  ];
  const statusDotColor = {
    '출석': '#27ae60', // green
    '지각': '#f1c40f', // yellow
    '결석': '#e74c3c'  // red
  };
  function renderStudents() {
    const list = document.getElementById('studentList');
    if (!list) return;
    list.innerHTML = '';
    // 전체보기 카드 추가
    // 학생 카드들
    students.forEach(s => {
      const card = document.createElement('div');
      card.className = 'student-card';
      card.innerHTML = `
        <span class="student-status-dot" style="background:${statusDotColor[s.status]||'#bbb'}"></span>
        <img class="student-img" src="${s.img}" alt="학생">
        <div class="student-info">
          <span class="student-name">${s.name}</span>
          <span class="student-status-label">${s.status}</span>
        </div>
      `;
      card.style.cursor = 'pointer';
      card.addEventListener('click', function() {
        window.location.href = `/templates/admin/admin-02-user/admin-user-trainee.html?name=${encodeURIComponent(s.name)}`;
      });
      list.appendChild(card);
    });
  }

  document.addEventListener('DOMContentLoaded', function() {
    renderStudents();
    var bodyRoleRaw = (document.body.dataset.userRole || '').trim();
    console.log(bodyRoleRaw);
    
    // student-list-scroll
    let anyHidden = false;
    document.querySelectorAll('.student-list-scroll[data-user-role]').forEach(function(el) {
        var elRoles = (el.dataset.userRole || '').split(',').map(r => r.trim()).filter(r => r);
        el.style.display = elRoles.includes(bodyRoleRaw) ? '' : 'none';
        console.log(elRoles.includes(bodyRoleRaw) ? 's' : 'none');
        if (el.style.display === 'none') anyHidden = true;
    });
    // comm-memo-section
    document.querySelectorAll('.comm-memo-section[data-user-role]').forEach(function(el) {
        var elRoles = (el.dataset.userRole || '').split(',').map(r => r.trim()).filter(r => r);
        el.style.display = elRoles.includes(bodyRoleRaw) ? '' : 'none';
        console.log(elRoles.includes(bodyRoleRaw) ? 'ss' : 'none');
        if (el.style.display === 'none') anyHidden = true;
    });
    // 하나라도 숨겨졌으면 .comm-chat-section 높이 100%
    var commChatSection = document.querySelector('.comm-chat-section');
    if (commChatSection) {
      if (anyHidden) {
        commChatSection.style.height = '100%';
      } else {
        commChatSection.style.height = '';
      }
    }
  });
// });

