function toggleSelectAll() {
    const selectAllCheckbox = document.getElementById('selectAll');
    const rowCheckboxes = document.querySelectorAll('.row-checkbox');
    
    rowCheckboxes.forEach(checkbox => {
        checkbox.checked = selectAllCheckbox.checked;
    });
}

function goBack() {
    window.history.back();
}

// Role-based button disabling for .big-btn[data-user-role]
document.addEventListener('DOMContentLoaded', function() {
    var bodyRoleRaw = (document.body.dataset.userRole || '').trim();
    document.querySelectorAll('.big-btn[data-user-role]').forEach(function(btn) {
        var elRoles = (btn.dataset.userRole || '').split(',').map(function(r) { return r.trim(); }).filter(Boolean);
        if (!elRoles.includes(bodyRoleRaw)) {
            btn.disabled = true;
        }
    });
});

// ===== 권한별 버튼 비활성화 (자동실행) =====
document.addEventListener('DOMContentLoaded', function() {
    
    var bodyRoleRaw = (document.body.dataset.userRole || '').trim();
    document.querySelectorAll('button[data-user-role], input[type="button"][data-user-role]').forEach(function(btn) {
        var elRoles = (btn.dataset.userRole || '').split(',').map(function(r) { return r.trim(); }).filter(Boolean);
        btn.disabled = !elRoles.includes(bodyRoleRaw);
    });
});