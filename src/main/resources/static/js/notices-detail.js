// 동적 생성: 추후 타임리프 변수로 치환하기 쉽게 구조화
document.addEventListener('DOMContentLoaded', function() {
    const noticeDetail = {
        id: 12,
        title: '2026년 1월 교육 일정 안내',
        author: '관리자',
        date: '2026-01-10',
        editDate: '2026-01-15',
        editor: '운영팀',
        views: 123,
        prev: { id: 11, title: '2025년 12월 마감 공지', url: 'notice-detail.html?id=11', date: '2025-12-31' },
        next: { id: 13, title: '2026년 2월 일정 안내', url: 'notice-detail.html?id=13', date: '2025-12-31' },
        attachments: [
            { name: '교육일정표.pdf', url: '/files/교육일정표.pdf' },
            { name: '운영안내.docx', url: '/files/운영안내.docx' }
        ],
        content: `안녕하세요, 삼성 아카데미 LXP 교육생 여러분!\n\n2026년 1월 교육 일정 및 운영 방안을 안내드립니다. 올 한 해도 여러분의 성장과 발전을 위해 최선을 다하겠습니다.\n\n1. 교육 일정 안내\n- KDT 과정: 2026년 1월 20일 개강 예정입니다. 사전 준비사항을 확인해주세요.\n- 고교위탁 과정: 2026년 1월 25일 개강 예정입니다. 고등학교와의 연계를 강화하겠습니다.\n- 심화 과정: 2026년 1월 27일 개강 예정입니다. 고급 기술 교육에 집중하겠습니다.\n\n2. 운영 방안\n- 온라인 플랫폼을 통한 실시간 강의 제공\n- 프로젝트 기반 학습 강화\n- 멘토링 프로그램 확대 운영\n- 취업 지원 프로그램 강화\n\n3. 유의사항\n- 출석률 80% 이상 유지 필수\n- 과제 제출 기한 엄수\n- 커뮤니티 활동 적극 참여\n- 건강 관리 및 스트레스 관리 중요\n\n자세한 내용은 첨부파일을 참고해주시기 바랍니다. 궁금한 사항은 언제든지 문의해주세요.\n\n삼성 아카데미 LXP 운영팀 드림`
    };
    const container = document.getElementById('noticeDetailDynamic');
    container.innerHTML = `
        <table class="notice-table">
            <colgroup>
                <col style="width: 6%;">
                <col style="width: 8%;">
                <col style="width: 6%;">
                <col style="width: 12%;">
                <col style="width: 6%;">
                <col style="width: 14%;">
                <col style="width: 6%;">
                <col style="width: 14%;">
                <col style="width: 6%;">
                <col style="width: 10%;">
                <col style="width: 6%;">
                <col style="width: 6%;">
            </colgroup>
            <tbody>
                <tr>
                    <th colspan="12" style="font: 18px;">${noticeDetail.title}</th>
                </tr>
                <tr>
                    <th>번호</th>
                    <td>${noticeDetail.id}</td>
                    <th>작성자</th>
                    <td>${noticeDetail.author}</td>
                    <th>작성일</th>
                    <td>${noticeDetail.date}</td>
                    <th>수정일</th>
                    <td>${noticeDetail.editDate}</td>
                    <th>수정자</th>
                    <td>${noticeDetail.editor}</td>
                    <th>조회수</th>
                    <td>${noticeDetail.views}</td>
                </tr>
                <tr>
                    <th>첨부파일</th>
                    <td colspan="12" style="text-align: start;">
                        ${noticeDetail.attachments && noticeDetail.attachments.length > 0 ? noticeDetail.attachments.map(att => `<a href="${att.url}" download>${att.name}</a>`).join(', ') : '없음'}
                    </td>
                </tr>
            </tbody>
        </table>
        <div class="notice-detail-content">
            ${noticeDetail.content.replace(/\n/g, '<br>')}
        </div>
        <table class="notice-table">
            <tbody>
                <tr>
                    <th style="width: 100px;">이전 글</th>
                    <td colspan="11" style="text-align: left;">
                        ${noticeDetail.prev ? `<p class="between"><a href="${noticeDetail.prev.url}">${noticeDetail.prev.title}</a><span style="color:#999;">${noticeDetail.prev.date}</span></p>` : '없음'}
                    </td>
                </tr>
                <tr>
                    <th>다음 글</th>
                    <td colspan="11" style="text-align: left;">
                        ${noticeDetail.next ? `<p class="between"><a href="${noticeDetail.next.url}">${noticeDetail.next.title}</a><span style="color:#999;">${noticeDetail.next.date}</span></p>` : '없음'}
                    </td>
                </tr>
            </tbody>
        </table>
        <div class="notice-detail-footer">
            <button id="noticeDeleteBtn" class="btn btn-gray" style="padding: 12px 40px; font-size: 16px;" data-user-role="admin">삭제</button>
            <div>
                <button class="btn btn-secondary" style="padding: 12px 40px; font-size: 16px;">수정</button>
                <button class="btn btn-save" style="padding: 12px 40px; font-size: 16px;" onclick="window.history.back()">목록</button>
            </div>
        </div>
    `;
    // 삭제 버튼 role 체크 후 숨김
    var bodyRoleRaw = (document.body.dataset.userRole || '').trim();
    var delBtn = document.getElementById('noticeDeleteBtn');
    if (delBtn) {
        var elRoles = (delBtn.dataset.userRole || '').split(',').map(function(r) { return r.trim(); }).filter(Boolean);
        if (!elRoles.includes(bodyRoleRaw)) {
            delBtn.style.display = 'none';
        }
    }
});
