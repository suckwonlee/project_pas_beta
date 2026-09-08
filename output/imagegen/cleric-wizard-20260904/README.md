# 성직자·마법사 스킬 아이콘

- 제작: 내장 image_gen 도구, 스킬별 독립 생성 1장씩.
- 성직자 14장, 마법사 20장, 합계 34장.
- 규격: 전체 1254 × 1254 PNG. 도트풍, 금색 테두리, 이미지 안 텍스트 없음.
- 서사·전설·이계 14장: 해당 캐릭터의 얼굴과 상반신 포함을 시각 확인함.
- 성직자 기본 공격·방어 계열 6종 제외: 불경 퇴치, 거룩한 일격, 정화의 불길, 신앙의 방패, 치유의 기도, 신성 개입.
- 현재 프로젝트의 ClericSkillRepository.java 및 WizardSkillRepository.java에 있는 스킬명·등급·효과를 기준으로 제작.
- 캐릭터 참조: app/src/main/res/drawable-nodpi/priest.png 및 wizard.png.
- 스타일 참조: app/src/main/res/drawable-nodpi/skill_hunter_fixed_trap.png.

index.html을 열면 모든 아이콘을 스킬명과 등급별로 확인하고 원본을 열 수 있습니다.
prompts.json에는 각 이미지의 최종 생성 프롬프트가 있습니다.
manifest.json에는 파일명, 스킬 ID, 등급, 실제 크기와 SHA-256이 있습니다.

이 폴더는 이미지 제작본입니다. 기존 이미지와 게임 코드는 변경하지 않았습니다.

