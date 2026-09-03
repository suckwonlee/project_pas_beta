package com.pas.game.character;

import com.pas.game.R;
import com.pas.game.passive.PassiveRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 구버전 CharacterDataRegistry의 캐릭터 이름과 플레이버 문구. */
public final class CharacterRepository {
    private final List<CharacterData> characters=Collections.unmodifiableList(Arrays.asList(
            new CharacterData("HERO","용사후보","추상적이었기에 수많았던 용사 후보 중 한 명. 자신이 용사라 믿고 세상의 구원을 위해 일어섰다.",R.drawable.hero,true,180,12,14,6,6,PassiveRepository.HEAVY_ARMOR,2),
            new CharacterData("HUNTER","사냥꾼","오랜 세월 이계의 존재들을 감시해온 감시단의 생존자. 이제 형제들의 복수를 위한 사냥에 나섰다.",R.drawable.hunter,true,160,14,11,9,8,PassiveRepository.SURPRISE_ATTACK,2),
            new CharacterData("CLERIC","성직자","이계의 침략으로 흔들리고 있는 교단 추기경의 딸. 세상의 평화를 기원하며 여정을 떠났다.",R.drawable.priest,true,160,11,18,3,4,PassiveRepository.PURIFICATION,2),
            new CharacterData("WIZARD","마법사","금지된 마법을 연구하다 마탑에서 추방된 최연소 졸업생. 누구의 방식이 옳았는지 보여줄 시간이 왔다.",R.drawable.wizard,true,150,16,10,9,6,PassiveRepository.MANA,2),
            new CharacterData("DRUID","드루이드","오랜 세월동안 잠들어있던 야생신들의 직계 혈통. 이계의 침략을 막기위해 오랜 잠에서 눈을 떴다.",R.drawable.druid,false),
            new CharacterData("ENGINEER","기술자","기술로 마법을 모두에게 선사하겠다 다짐한 기술자. 이제 그 기술로 모두를 구할 때가 도래했다.",R.drawable.artificer,false),
            new CharacterData("BARD","음유시인","몽환계의 힘을 내려받아 노래로써 빚어내는 음유시인. 세상에 이야기를 남기기 위해 모험을 떠났다.",R.drawable.bard,false),
            new CharacterData("EXOTIC","이계종","침략해온 이계의 존재들과는 달리 물질계를 관찰하는걸 즐기는 존재. 죽어가던 병사와 계약을 맺어 빙의했다.",R.drawable.exotic,false)
    ));
    public List<CharacterData> all(){return characters;}
}
