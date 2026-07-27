    // Floating Chatbot Open/Close Logic
        const chatbotBtn = document.getElementById('floatingChatbotBtn');
        const chatbotWindow = document.getElementById('floatingChatbotWindow');
        const chatbotCloseBtn = document.getElementById('chatbotCloseBtn');
        const chatbotInput = document.getElementById('chatbotInput');
        const chatbotSendBtn = document.getElementById('chatbotSendBtn');
        const chatbotMessages = document.getElementById('chatbotMessages');

        if (chatbotBtn && chatbotWindow) {
          chatbotBtn.addEventListener('click', function() {
            if (chatbotWindow.style.display === 'flex') {
              chatbotWindow.style.display = 'none';
            } else {
              chatbotWindow.style.display = 'flex';
              setTimeout(() => chatbotInput && chatbotInput.focus(), 120);
            }
          });
        }
        if (chatbotCloseBtn && chatbotWindow) {
            chatbotCloseBtn.addEventListener('click', function() {
                chatbotWindow.style.display = 'none';
            });
        }
        function appendChatbotMsg(msg, isUser) {
            const div = document.createElement('div');
            div.className = 'chatbot-msg ' + (isUser ? 'chatbot-msg-user' : 'chatbot-msg-bot');
            div.textContent = msg;
            chatbotMessages.appendChild(div);
            chatbotMessages.scrollTop = chatbotMessages.scrollHeight;
        }
        function handleChatbotSend() {
            const val = chatbotInput.value.trim();
            if (!val) return;
            appendChatbotMsg(val, true);
            chatbotInput.value = '';
            setTimeout(() => appendChatbotMsg('AI 헬피 챗봇이 곧 답변을 드릴 예정입니다.', false), 600);
        }
        if (chatbotSendBtn && chatbotInput) {
            chatbotSendBtn.addEventListener('click', handleChatbotSend);
            chatbotInput.addEventListener('keydown', function(e) {
                if (e.key === 'Enter') handleChatbotSend();
            });
        }
        // 시험 일정 뱃지 클릭 시 응답
        setTimeout(function() {
          var badgeRow = document.querySelector('.chatbot-badge-row');
          if (badgeRow) {
            var badges = badgeRow.querySelectorAll('.chatbot-badge');
            badges.forEach(function(badge) {
              if (badge.textContent.replace(/\s/g,'') === '시험일정') {
                badge.addEventListener('click', function() {
                  appendChatbotMsg('시험 일정', true);
                  setTimeout(function() {
                    appendChatbotMsg('다음 시험 일정은 2월 15일(월) 10:00, 온라인 평가입니다. 자세한 내용은 [시험/과제] 메뉴에서 확인할 수 있습니다.', false);
                    setTimeout(function() {
                      // 말풍선 스타일로 추가
                      var div = document.createElement('div');
                      div.className = 'chatbot-msg chatbot-msg-bot chatbot-msg-cta';
                      div.innerHTML = '<a href="/templates/trainee/online-test.html" style="color:#fff;text-decoration:none;">온라인 시험 바로가기 &gt;</a>';
                      chatbotMessages.appendChild(div);
                      chatbotMessages.scrollTop = chatbotMessages.scrollHeight;
                    }, 500);
                  }, 500);
                });
              }
            });
          }
        }, 300);

// 스타일 추가
(function() {
  var style = document.createElement('style');
  style.innerHTML = '.chatbot-msg-cta { background: #638ECB; color: #fff; align-self: flex-start; font-weight: 600; text-align: center; border-radius: 16px; margin-top: 6px; box-shadow: 0 2px 8px rgba(99,142,203,0.10); padding: 10px 18px; display: inline-block; max-width: 90%; } .chatbot-msg-cta a:hover { text-decoration: underline; }';
  document.head.appendChild(style);
})();
