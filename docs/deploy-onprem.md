# 온프레미스(Ubuntu) 배포 가이드

> 대상: 사내 Ubuntu 서버 1대에 Docker 로 앱+PostgreSQL 배포.
> 이 절차는 2026-07-28 로컬 리허설로 전 과정 검증됨(가입→승인→로그인 E2E, 31화면 검증 — 기준 커밋 fc94eb6).

## 0. 사전 조건 (서버 담당자 협조)

- SSH 계정 (IP·포트·계정·키/비밀번호)
- Docker + docker compose 설치되어 있거나 설치 권한(sudo)
- 방화벽에서 서비스 포트 오픈 (기본 8080)
- 서버가 GitHub 에 접근 가능해야 함 (안 되면 §6 오프라인 방식)
- 권장 사양: RAM 4GB+, 디스크 50GB+ (VOD 업로드 최대 500MB/개)

## 1. 최초 배포

```bash
ssh <계정>@<서버IP>

# Docker 미설치 시 (Ubuntu)
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER && exit   # 재접속

git clone https://github.com/woongscoding/axi_project.git lxp && cd lxp
cp .env.example .env
nano .env   # 아래 4개 값 채우기
```

`.env` 필수 값:

| 키 | 설명 |
|---|---|
| `LMS_DB_PASSWORD` | DB 비밀번호 — 강한 랜덤값 |
| `LMS_CRYPTO_SECRET` | 개인정보 AES 키(32자+ 랜덤). **한번 정하면 절대 변경 금지, 분실 시 기존 개인정보 복호화 불가 — 반드시 별도 보관** |
| `LMS_ADMIN_INIT_PASSWORD` | 초기 관리자(admin) 비밀번호. 비우면 첫 기동 로그에 무작위 값 1회 출력 |
| `LMS_HTTP_PORT` | 서비스 포트 (기본 8080) |

랜덤값 생성: `openssl rand -base64 32`

```bash
docker compose up -d --build     # 앱+DB 기동 (최초 빌드 5~10분)
docker compose ps                # 둘 다 Up/healthy 확인
docker compose logs -f app       # "Started LmsApplication" 확인 후 Ctrl+C
```

접속: `http://<서버IP>:8080` → admin / (설정한 비밀번호) 로그인 → 사용자 관리·과정 개설 시작.

## 2. 업데이트 배포

```bash
cd ~/lxp && git pull && docker compose up -d --build
```

DB·업로드 파일은 도커 볼륨(lxp-dbdata, lxp-uploads)에 있어 업데이트/재시작에도 유지된다.

## 3. 일상 운영 명령

```bash
docker compose ps                # 상태
docker compose logs -f app       # 앱 로그 (에러 확인)
docker compose restart app       # 앱만 재시작
docker compose down              # 전체 중지 (데이터는 볼륨에 유지)
```

## 4. 백업 (필수 — 3년 보존 요건)

```bash
# DB 백업 (cron 으로 매일 새벽 권장)
docker exec lxp-db-1 pg_dump -U lms lms | gzip > ~/backup/lms-$(date +%F).sql.gz

# 업로드 파일 백업
docker run --rm -v lxp_lxp-uploads:/data -v ~/backup:/out alpine tar czf /out/uploads-$(date +%F).tar.gz /data
```

crontab 예시 (`crontab -e`): `0 4 * * * ~/lxp/scripts/backup.sh`
복구: `gunzip -c 백업파일 | docker exec -i lxp-db-1 psql -U lms lms`
`.env`(특히 CRYPTO_SECRET)도 백업 대상에 포함할 것.

## 5. 트러블슈팅

| 증상 | 확인 |
|---|---|
| 앱이 안 뜸 | `docker compose logs app` — DB healthy 전이면 잠시 대기, `.env` 값 누락 여부 |
| 접속 불가 | 서버 방화벽(ufw status)·사내 방화벽에서 포트 오픈 여부 |
| 화면 500 | `docker compose logs app | grep ERROR` — PostgreSQL 전용 이슈면 개발팀 전달 |
| 디스크 부족 | `docker system df`, 오래된 이미지 정리 `docker image prune -f` |

## 6. 서버가 GitHub 접근 불가(폐쇄망)인 경우

로컬에서 이미지를 말아 파일로 전송:

```bash
# 로컬(Windows)
docker compose build
docker save samsung-lxp-app postgres:16-alpine | gzip > lxp-images.tar.gz
scp lxp-images.tar.gz docker-compose.yml .env.example <계정>@<서버IP>:~/lxp/

# 서버
docker load < lxp-images.tar.gz
docker compose up -d   # --build 없이
```

## 7. 이후 과제 (초안 운영 시작 후)

- HTTPS: 사내 인증서 + nginx 리버스 프록시 (내역서 E-4)
- 모니터링: actuator health + 알림 (B 와 build.gradle 합의 필요)
- 스키마 안정화 후 ddl-auto: update → validate + Flyway 전환
