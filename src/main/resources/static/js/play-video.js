// 동영상 컨트롤
const video = document.getElementById('videoPlayer');
const playPauseBtn = document.getElementById('playPauseBtn');
const playIcon = document.getElementById('playIcon');
const pauseIcon = document.getElementById('pauseIcon');
const progressContainer = document.getElementById('progressContainer');
const progressBar = document.getElementById('progressBar');
const progressHandle = document.getElementById('progressHandle');
const timeDisplay = document.getElementById('timeDisplay');
const speedSelect = document.getElementById('speedSelect');
const volumeBtn = document.getElementById('volumeBtn');
const volumeSlider = document.getElementById('volumeSlider');
const volumeLevel = document.getElementById('volumeLevel');
const fullscreenBtn = document.getElementById('fullscreenBtn');

// 재생/일시정지
playPauseBtn.addEventListener('click', () => {
    if (video.paused) {
        video.play();
        playIcon.style.display = 'none';
        pauseIcon.style.display = 'block';
    } else {
        video.pause();
        playIcon.style.display = 'block';
        pauseIcon.style.display = 'none';
    }
});

// 시간 포맷
function formatTime(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

// 진행 업데이트
video.addEventListener('timeupdate', () => {
    const progress = (video.currentTime / video.duration) * 100;
    progressBar.style.width = progress + '%';
    progressHandle.style.left = progress + '%';
    timeDisplay.textContent = `${formatTime(video.currentTime)} / ${formatTime(video.duration)}`;
});

// 진행 바 클릭
progressContainer.addEventListener('click', (e) => {
    const rect = progressContainer.getBoundingClientRect();
    const pos = (e.clientX - rect.left) / rect.width;
    video.currentTime = pos * video.duration;
});

// 재생 속도
speedSelect.addEventListener('change', (e) => {
    video.playbackRate = parseFloat(e.target.value);
});

// 볼륨 조절
volumeSlider.addEventListener('click', (e) => {
    const rect = volumeSlider.getBoundingClientRect();
    const pos = (e.clientX - rect.left) / rect.width;
    video.volume = pos;
    volumeLevel.style.width = (pos * 100) + '%';
});

// 볼륨 버튼
volumeBtn.addEventListener('click', () => {
    video.muted = !video.muted;
    volumeLevel.style.width = video.muted ? '0%' : (video.volume * 100) + '%';
});

// 전체화면
fullscreenBtn.addEventListener('click', () => {
    const container = document.querySelector('.video-container');
    if (!document.fullscreenElement) {
        container.requestFullscreen();
    } else {
        document.exitFullscreen();
    }
});

// 초기 볼륨 설정
video.volume = 1;
volumeLevel.style.width = '100%';




