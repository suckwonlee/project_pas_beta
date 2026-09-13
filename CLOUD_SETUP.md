# Google Cloud 접속 설정

> 최신 상태(2026-09-08)는 APP_SERVER_INTEGRATION.md / CLOUD_RESUME.md 참조. 서버는 비공개 배포 완료, 지인 인증은 개인별 1회용 초대 코드다. 아래의 공용 베타 키 입력 및 미배포 안내는 이전 절차다.

## API 키만 넣는다는 의미

Google Cloud 관리용 API 키나 서비스 계정 JSON으로 PAS 전투 서버에 접속하는 구조가 아니다. Cloud Run에 PAS_server를 배포하고, **서버 주소를 앱 빌드 설정에 한 번 지정**한 뒤, 사용자는 **PAS 접속 키**만 입력한다. 방 생성/참가 후에는 서버가 발급한 방별 토큰으로 REST/WS를 사용한다.

현재 온라인 화면은 기본 공격/방어 베타다. 전체 스킬/룬 선택 및 기존 전투 HUD의 온라인 연결은 아직 별도 미완료 항목이다. 클라우드 설정만으로 그 기능이 완성되는 것은 아니다.

## 1. 서버 쪽: 배포 시 한 번

대상 프로젝트: `C:\Users\USER\IdeaProjects\PAS_server`

1. 사용자가 소유한 GCP 프로젝트/리전에서 현재 Dockerfile 기반으로 PAS_server를 Cloud Run에 배포한다. 실제 프로젝트 ID와 서버 주소는 아직 제공받지 않아 배포하지 않았다.
2. Secret Manager에 충분히 무작위인 PAS 접속 키를 등록한다. **공백 없는 ASCII 32~256자**, 예: 비밀번호 관리자가 생성한 무작위 48자. 문서의 예시 문자열이나 소스의 테스트 키를 실제 키로 사용하지 않는다.
3. 해당 비밀 버전을 Cloud Run 환경 변수 **`PAS_BETA_ACCESS_KEY`**로 연결한다. 런타임 서비스 계정에는 해당 secret에 대한 접근 권한만 부여한다. 관리용 JSON 키를 APK에 넣지 않는다.
4. 앱은 Cloud Run IAM ID 토큰을 보내지 않는다. 외부 APK가 서비스에 도달하려면 Cloud Run 호출자 접근 설정을 별도로 허용해야 한다. 이 경우 인터넷 요청을 받으므로 PAS 접속 키 검사가 반드시 켜져 있어야 한다. 비용/노출 범위를 확인한 후 사용자가 설정한다. API 키를 넣는 것만으로 IAM 403이 해결되지는 않는다.
5. 현재 방 저장소는 메모리이므로 초기 테스트에서는 최대 인스턴스 1, WebSocket 요청 제한 3600초를 사용한다. 그래도 재배포/재시작/교체 중 방 보존을 보장하지 않는다. 실제 운영과 다중 인스턴스에는 공유 저장소가 필요하다.

Cloud Run에서 자동 제공하는 `K_SERVICE`가 있으면 `CloudDeploymentSafety`가 빈/짧은 키를 감지하여 시작을 실패시킨다. 로컬 개발에서는 기존처럼 빈 키를 허용한다.

공유 접속 키는 지인 베타용 초대 장치이며, 개인 계정 인증/차단/속도 제한/과금 보호를 대체하지 않는다. 사용자별 운영 인증은 별도 작업이다.

## 2. Android 쪽: 주소 한 번 설정

Android 프로젝트 루트의 **`pas-cloud.properties`** 파일에서 다음 값만 채운다 (빈 파일을 준비해 뒀다):

```properties
pasServerUrl=https://실제-Cloud-Run-서비스-주소
```

- 실제 Cloud Run HTTPS 주소를 그대로 사용한다. `/api`, `/ws`, 키, 쿼리 문자열은 붙이지 않는다.
- 이 파일에는 `pasServerUrl` 외의 항목을 허용하지 않는다. **API 키는 이 파일이나 BuildConfig에 넣지 않는다.**
- 개발자별 파일이므로 Git에서 제외했다. 다른 PC에서는 `pas-cloud.properties.example`을 참고한다.
- 자동 빌드에서는 `-PpasServerUrl=https://...`가 파일보다 우선한다.
- 저장 후 Android Studio에서 Sync/Run 또는 APK 빌드를 한다. **서명 설정은 기존 것을 유지한다.**
- 주소가 비어 있으면 시작 화면은 기존 싱글 모드 그대로다. debug 설정 메뉴에서만 로컬 주소를 직접 입력할 수 있다.

## 3. APK 사용자

시작 화면 **온라인 협동 베타** → 플레이어 이름과 **PAS 접속 키** 입력 → 방 생성 또는 방 코드로 참가 → 캐릭터 선택 → 준비 완료.

앱에는 서버 주소가 고정되어 있으므로 사용자가 주소를 입력하거나 변경하지 않는다. 키는 저장하지 않고 접속 화면에서 입력받는다. 앱 프로세스를 완전히 종료하고 다시 접속하면 다시 입력해야 한다. 방 토큰은 URL/로그에 넣지 않는다.

## 연결이 안 될 때

- 시작 화면에 온라인 버튼 없음: `pas-cloud.properties`가 비어 있거나 APK를 다시 빌드하지 않음.
- 앱에서 PAS 접속 키 요구: 서버용 키를 입력한다. Google API 키/서비스 계정 파일을 입력하지 않는다.
- 서버가 시작되지 않음: Cloud Run에 PAS_BETA_ACCESS_KEY와 Secret Manager 권한/버전 연결 확인.
- HTTP 403: PAS 키 일치 여부와 Cloud Run 호출자 접근 설정을 각각 확인.
- 전투 중 방 소멸: 인메모리 저장소의 서버 재시작/재배포 한계. API 키 문제와 다르다.

공식 참고: [Cloud Run 인증](https://docs.cloud.google.com/run/docs/authenticating/overview), [Secret Manager 연결](https://docs.cloud.google.com/run/docs/configuring/services/secrets), [WebSocket 연결](https://docs.cloud.google.com/run/docs/triggering/websockets).
