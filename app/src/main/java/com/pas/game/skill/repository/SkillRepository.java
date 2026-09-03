package com.pas.game.skill.repository;

import com.pas.game.skill.data.SkillData;
import java.util.List;

/** 캐릭터별 스킬 저장소가 공유하는 조회 규약. */
public interface SkillRepository {
    List<SkillData> all();
    SkillData find(String id);
}
