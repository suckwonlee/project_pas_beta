# PAS 전투 프로토타입

## 싱글 난이도 / 보상 포기 골드 (2026-09-24, 최신)
싱글(1인 1/2캐릭터)의 일반·이벤트 전투 시작 편성은 기존 후보 중 1마리. 선정 결과는 저장/복원 시 유지. 보스 HP/공격력/치명타율은 원본의 60%(올림): 330/9/3%. 보스 소환과 온라인 협동 원본 편성/능력치는 유지.
보상 포기 시 40골드 1회 지급. 싱글 공용 지갑, 온라인 개인 보상 소유자 지갑(공동 보상은 방장)으로 지급하며 재시도 중복 방지. 검증 기록은 REWARD_CHOICES_HANDOFF.md 참고.
검증: 단위 227 통과/4 조건부 스킵, 서버 53 통과, 챕터 UI 3 통과. 최신 APK: `app/build/outputs/apk/archive/PAS-beta-solo-balance-20260924.apk`. 기존 서명 유지·클라우드 미배포.

## 보상 선택·양도 / 물약 보관함 (2026-09-24, 최신)
스킬 등 선택형 보상은 후보 3개 중 1개 선택 또는 선택 안 함. 패시브 5개면 부여석 제외, 부여석은 항상 LV1. 물약만 보관하며 UI 이름은 물약 보관함. 2캐릭터 보상 양도 및 서버 권한 검증 연결.
아래 이전 보상 보관/LV1~5 부여 기록은 과거 규칙. [REWARD_CHOICES_HANDOFF.md](REWARD_CHOICES_HANDOFF.md)를 우선한다.
검증: Android 단위 224 통과/4 조건부 스킵, UI 9개 통과, 서버 52개 통과, APK/lint 성공. 설치 APK: `app/build/outputs/apk/archive/PAS-beta-reward-choices-20260924.apk`. 기존 서명 유지, 클라우드 미배포.

## 스킨 상점 연결 (2026-09-23, 최신)
베타 무료 해금 및 P1/P2 슬롯별 장착/로컬 저장 연결 완료. 미리보기는 장착하지 않으며 기존 장착 스킨은 소유 상태로 이관한다. 아래 미리보기 전용/미연결 기록은 과거 상태다.
APK: `app/build/outputs/apk/archive/PAS-beta-skin-shop-20260923.apk`. 업데이트 전 APK는 `PAS-before-skin-shop-20260923.apk`로 보존. 기존 서명 유지.
Android 단위 테스트 218 통과/4 조건부 스킵, assembleDebug/lintDebug 성공. 화면 테스트 최종 결과와 서버 상태는 [SKIN_SHOP_HANDOFF.md](SKIN_SHOP_HANDOFF.md) 참고.

## 챕터1 연결 완료 기록 (2026-09-23, 최신)
임시 10단계 동선과 6종 몬스터·비전투 전투·처치 보상 연결. **6단계 상점, 10단계 보스**. 시작 상점은 제거했다.
싱글 1/2캐릭터 및 서버 권위형 협동 모두 반영. 온라인 인당 200골드, 파티 공유 처치 보상 1개, 본인 캐릭터에만 적용, 방장 진행.
Android 단위 테스트 217 통과/4 조건부 스킵, 에뮬레이터 UI 테스트 7개 통과, 서버 테스트 48개 통과. APK 및 lint 오류 0 확인(경고는 남음).
클라우드 미배포·실기기 2대 협동 미검증. 서명/접속 주소 변경 없음.
자세한 재개 위치·임시 확률·복원 한계는 [CHAPTER1_HANDOFF.md](CHAPTER1_HANDOFF.md). 아래 과거의 시작 상점/멀티 미연결 설명보다 이 항목이 우선한다.

## 스킨 상점 UI (2026-09-23)

시작 화면의 스킨 상점 버튼 → 가로 미리보기 화면. 한복 2종 카드와 원본 전체 미리보기, 구매 준비 중(비활성)·돌아가기 버튼만 구현. 가격/통화/결제/골드 차감/구매/해금/서버/장착 연결 없음. 상품 카드는 미리보기만 바꾸며 기존 스킨 선택값을 변경하지 않는다. SkinShopPreview.java 및 activity_skin_shop.xml 참고. assembleDebug 성공 및 skinShopIsPreviewOnly 에뮬레이터 테스트 통과, 실제 화면 확인. 캡처는 보조 worktree build/skin-shop.png. 실기기 미검증.

이미지 경로 재점검: app/build.gradle의 sourceSets에 res-art 분류 폴더 및 skins 폴더가 등록되어 있다. Java/XML의 R.drawable 및 @drawable은 파일명을 참조하므로 폴더명으로 변경하지 않는다. 앱 코드/테스트/스크립트에서 옛 res/drawable-nodpi 경로를 직접 읽는 코드는 검색 결과 없음. 최신 빌드 source-set/리소스 컴파일 매핑도 새 경로 사용을 확인했다. Android Studio에서는 Gradle Sync 후 Project 보기로 확인한다.

## 스킨 준비 (2026-09-23)

최종 검증 완료: 아래 UI 테스트 진행 중 기록은 완료됨. 캐릭터 UI 테스트 4개, 스킨/파티 단위 테스트 14개와 빌드 통과. 실제 스킨 선택창 캡처 확인. 휴대폰/온라인 검증 미실시.

최신 추가: 임시명을 `성직자-한복`/`용사후보-한복`으로 변경. 캐릭터 초상화 옆 스킨 버튼에서 기본/한복을 선택할 수 있고 슬롯+직업별 선택을 로컬 저장한다. 로컬 파티·상점 캐릭터 사본에도 스킨 ID를 보존. 선택 UI/로컬 저장 미구현이라는 아래 과거 기록은 이 항목으로 대체한다. 온라인 동기화/구매·해금은 여전히 미구현. 빌드 및 스킨/파티 단위 테스트 14개 통과, UI 테스트 진행 중.

성직자·용사후보의 사용자 제공 스킨 이미지 등록 완료(`skin_01`, 임시명 `스킨 1`). 기본 외형은 유지. 이미지 원본 SHA256 동일 확인, 선택/초상화/마커 공용 등록. 선택 UI 등은 아직 미연결이며 `SKIN_SYSTEM_HANDOFF.md` 참고. 등록 후 CharacterSkinTest 6개와 assembleDebug 통과. 실기기 표시 검증은 미실시.

외형 전용 CharacterSkin/SkinRepository 및 캐릭터별 skins 리소스 경로 추가. 기존 getter는 기본 외형 유지, skinId를 받는 선택/초상화/마커 조회 API 준비. 아직 선택 UI·저장·구매/해금·온라인 동기화는 없음. 자세한 추가 방법과 재개 지점은 [SKIN_SYSTEM_HANDOFF.md](SKIN_SYSTEM_HANDOFF.md). CharacterSkinTest 5개 통과(실패/스킵 0), assembleDebug 성공. 이번 실기기/온라인 실행 검증은 하지 않았다.

## 이미지 폴더 분류 (2026-09-23)

Android Studio 왼쪽 보기를 `Android` 대신 `Project`로 바꾸고 `app/src/main/res-art`를 연다. Android 보기는 여러 리소스 루트를 drawable로 합쳐 표시할 수 있다.

- `skills/hero`, `hunter`, `cleric`, `wizard`: 용사후보/사냥꾼/성직자/마법사 스킬(기존 버전도 삭제하지 않고 보관)
- `characters`: 캐릭터 초상화 / `enemies`: 적 이미지
- `runes/primary`, `secondary`: 룬 1/2
- `items/potions`, `elixirs`, `upgrades`: 물약/영약/강화·진화 아이템
- `backgrounds/continents`, `battle`, `shop`: 대륙/전장/상점 배경
- `placeholders`: 임시 이미지

각 분류 안의 `drawable-nodpi`에 이미지를 추가한다. 파일명은 모든 분류에서 고유한 소문자 영문·숫자·밑줄을 사용한다. 기존 파일명, 이미지 내용, `R.drawable` 참조는 변경하지 않는다. XML 도형·버튼·공통 아이콘과 런처 리소스는 기존 `res`에 유지한다. 새로운 분류를 추가할 때는 `app/build.gradle`의 `sourceSets.main.res.srcDirs` 목록에도 등록한다. drawable 내부에 임의 하위 폴더를 만들지 않는다. Gradle Sync 후 적용된다.

검증 완료: 이미지 161개를 15개 분류로 이동했고 SHA256 비교로 전 파일 내용 보존 확인. assembleDebug 통과, processDebugResources --rerun-tasks로 캐시 없이 리소스 재컴파일/연결 통과. 파일명과 R.drawable 참조 유지. 실기기 실행은 이번 작업에서 하지 않음. 서명/전투/서버 설정 변경 없음.

Android 네이티브 Java 기반의 가로형 4×3 턴제 전투 프로토타입이다.

## 최근 작업 / 재개 지점 (2026-09-22)

- 캐릭터 선택 문구 추가 수정: `캐릭터 소개` 제목만 제거하고 원문은 유지. 최종 진행 버튼은 `상점으로` → `다음으로`(XML/동적 표시 모두). P2 생성 단계 문구는 유지. 이번 목표는 챕터1 완성이며, 이 문구 수정 자체는 기존 이동 동선을 변경하지 않는다.

- 중단 작업 재개: 룬 상세창은 완료 상태이며, 룬 목록/캐릭터 화면 추가 캡처 전달만 남아 있었다.
- 캐릭터 목록 스크롤 시 모서리 장식이 이동하는 문제: 장식을 ScrollView에서 스크롤하지 않는 바깥 FrameLayout으로 분리. 같은 구조인 룬 패널도 함께 수정했다.
- 검증 완료: 2026-09-22 debug 빌드와 Android 17 에뮬레이터 UI 테스트 6개 통과(실패/스킵 0). 스크롤 전후 바깥 틀의 위치/스크롤값 고정, 룬 선택·취소, P1/P2 독립 설정 및 대륙 선택을 확인했다. 실기기 검증은 미실시.
- 새 캡처: 보조 worktree의 `build/character-scrolled.png`(스크롤 후 테두리 고정), `build/character-landscape.png`(캐릭터 선택), `build/rune-list.png`(룬 목록). 이번 요청의 미완료 작업은 없다. 전투/서버/서명은 변경하지 않았다.

- 실제 작업본: `C:/Users/USER/AndroidStudioProjects/project_pas`. 오래된 보조 worktree로 덮어쓰지 않는다.
- 대륙·캐릭터 선택 가로 UI 반영 완료. 캐릭터 8칸, 설명/시작 패시브, 별도 P1/P2 설정을 유지한다.
- 캐릭터 제작의 스킬 선택 UI는 제거했다. 기본 공격/방어만 설정하고 습득 스킬은 비워 둔다. 룬 1/2 버튼은 항상 표시한다.
- 룬 후보 터치 → 전용 상세창(아이콘, 이름/LV, 효과, 플레이버) → **선택**을 눌러야 적용한다. **뒤로** 또는 시스템 뒤로가기는 목록으로 돌아가며 미리보기만으로 변경하지 않는다.
- 효과는 `RuneDetailFormatter`가 RuneData/PassiveRepository의 레벨별 실제 데이터에서 만든다. 전투·서버 룬 모델과 서명 설정은 변경하지 않았다.
- 플레이버 수정 위치: `app/src/main/res/values/rune_detail_strings.xml`. 각 `rune_flavor_*` 내용만 바꾸면 된다. 현재는 사용자 요청대로 `화염의 룬 플레이버 텍스트 입력칸` 등 룬별 자리표시자를 넣었다.
- 상세창 구현: `ui/view/RuneDetailDialog.java`, `res/layout/dialog_rune_detail.xml`. 긴 설명은 스크롤, 하단 선택/뒤로 버튼은 고정이다.
- 검증 완료: debug 빌드 성공. Android 17 에뮬레이터에서 CharacterSelectionUiTest/ContinentSelectionUiTest 총 5개 통과. 미리보기 미적용, 선택 확정, 뒤로/시스템 뒤로 목록 복귀, 룬2, 독립 P1/P2, 전투 진입 확인. 실제 캡처도 확인했다. 실기기·온라인 검증은 이번 작업 범위가 아니다.
- 캡처: `C:/Users/USER/.codex/worktrees/4066/project_pas/build/rune-detail.png`. 이번 룬 상세창 작업은 완료. 추후 원문이 준비되면 위 문자열 파일만 수정한다. 전체 단위 테스트 재실행은 하지 않았다. 기존 ShopEconomyTest 기대값 실패는 별도 이슈다.

## 실행 흐름

1. 시작 화면을 터치한다.
2. 대륙 선택 전에 `1인 1캐릭터`, `1인 2캐릭터`, `2인 온라인 협동`을 고른다.
3. 대륙을 선택한 뒤 싱글은 캐릭터·룬을 설정한다. 2캐릭터는 P1/P2를 개별 생성하며 자동 복제하지 않는다. 멀티는 대기실 미리보기로 이동한다.
4. `이동` 또는 스킬을 선택한 뒤 대상 칸/유닛을 고르고 `실행`한다.

캐릭터 선택에서는 룬 1/2 버튼으로 목록과 상세창을 열고 선택을 확정한다. 제작 단계의 스킬 선택창은 제거했으며, 시작 시 기본 공격/방어만 설정하고 습득 스킬 네 칸은 비워 둔다. 공개 기능은 `BetaFeatures`에서 관리한다.

일반 멀티 대기실의 방 코드는 아직 서버에 등록되지 않는 임시 코드다. 기존 온라인 테스트 코드는 보존했지만 베타 앱에서는 진입할 수 없다. 실제 서버 연동은 `BETA_SERVER_HANDOFF.md`를 참고한다.

## 구조

`BattleCommand → BattleEngine → BattleState → BattleResult/Event → UI 갱신` 흐름을 따른다. UI는 HP나 위치를 직접 변경하지 않으며, 로컬 입력은 `PlayerCommandSource` 계약을 통한다. 원격 입력도 같은 명령을 JSON으로 직렬화해 서버에 제출한다.

- `battle/`: 명령, 엔진, 상태, 턴, 결과
- `skill/`: 불변 설계도, 런타임 자원, 조합형 효과, 저장소
- `status/`: 공통 상태 인스턴스와 지속시간
- `unit/`: 고유 `unitId` 기반 플레이어/적
- `ai/`: 교체 가능한 적 AI 전략
- `ui/`: 상태 렌더링 및 입력 변환

## 싱글 파티와 온라인 협동

캐릭터 수와 접속 플레이어 수는 별도 개념이다.

- 1인 1캐릭터: 기존 기본 플레이. 싱글 규칙을 사용한다.
- 1인 2캐릭터: 하나의 `clientId`가 두 `unitId`를 소유한다. 싱글 규칙을 유지한다.
- 2인 협동: 서로 다른 `clientId`가 각 캐릭터를 소유한다. 이때만 온라인 협동 전용 효과를 사용한다.

`MultiplayerMatchFactory`가 로비 구성을 결정적 랜덤 시드와 함께 전투로 만들고, `AuthoritativeBattleSession`만 실제 `BattleEngine`을 변경한다. 서버는 명령마다 프로토콜 버전, 매치 ID, 클라이언트 소유권, 요청 ID, 예상 상태 리비전을 검사한다. 중복 요청은 한 번만 실행되며, 승인 후에는 전체 `BattleSnapshot`과 SHA-256 상태 다이제스트를 반환한다. 적 AI 역시 서버 세션에서 처리되므로 클라이언트마다 전투를 재계산하지 않는다.

`LoopbackBattleTransport`는 로컬 멀티 테스트용이다. 실제 Spring 서버 연결은 `network/RoomApiClient`, `WebSocketBattleTransport`, `OnlineBattleSession`이 담당한다. 서버 응답은 `RemoteBattleSnapshot` 화면 DTO로만 읽으며 클라이언트에서 엔진을 재실행하지 않는다. 재접속은 새 WebSocket 연결의 서버 `SYNC`를 사용한다. 서버 주소 기본값은 비어 있고 `-PpasServerUrl=https://...` 또는 개발 화면에서 설정한다.

## 온라인 연결 테스트 / 작업 재개

**최신 진행 상태와 미완료 목록은 [MULTIPLAYER_HANDOFF.md](MULTIPLAYER_HANDOFF.md)를 먼저 읽는다.** 서버의 9월 3일 인수인계 문서보다 이 문서가 최신이다.

Android Studio의 debug 앱에서 시작 화면 `Project-PAS` 제목 길게 누르기 → `온라인 서버 연결 테스트`. 방 생성/참가, 캐릭터 선택, 준비, 기본 공격/방어, 이동/턴 종료 및 재동기화를 검증할 수 있다. 완성 게임의 전체 스킬/룬 UI와 온라인 연결은 아직 별도 작업이다.

Cloud Run 연결은 [CLOUD_SETUP.md](CLOUD_SETUP.md)를 따른다. `pas-cloud.properties`에 공개 HTTPS 서버 주소를 한 번 설정하면 debug/release 모두 시작 화면에 `온라인 협동 베타`가 나타나고, 사용자는 PAS 접속 키만 입력한다. 키를 APK/BuildConfig에 넣지 않는다. 주소가 없는 release에서는 온라인 진입을 비활성화한다. 아직 실제 GCP 배포는 하지 않았다.

## 설계 기준과 현재 제한

연결된 Google Drive의 `PAS-전반 설계도`를 기준으로 화염·중독의 턴 종료 스택 처리, 방어전념의 싱글/멀티 제한, 광역 난도질, 구원의 맹세를 반영했다. `신념의 검`이 부여하는 정확한 화염 스택 수치는 문서에도 없으므로 기존 프로토타입 피해량을 유지한다. 실제 WebSocket 서버 배포와 저장 기능, 에피소드·챕터·이벤트 진행은 이번 범위에 포함하지 않는다.
