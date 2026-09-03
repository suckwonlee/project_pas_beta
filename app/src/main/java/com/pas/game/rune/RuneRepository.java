package com.pas.game.rune;

import static com.pas.game.rune.RuneData.Stat.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** RuneEffectApplier와 PassiveSkill에서 추출한 룬 이름 및 수치표. */
public final class RuneRepository {
    private final List<RuneData> primary=Collections.unmodifiableList(Arrays.asList(
            RuneData.primary("fire","화염의 룬","방화",new int[]{5,7,10,18,25},ATTACK,1),
            RuneData.primary("frost","혹한의 룬","중갑",new int[]{2,4,7,11,16},DEFENSE,1),
            RuneData.primary("guardian","수호자의 룬","의지",new int[]{5,10,15,20,30},MAX_HP,10),
            RuneData.primary("venom","독사의 룬","침독",new int[]{1,1,2,3,4},CRITICAL_RATE,3),
            RuneData.primary("fighting","투지의 룬","투쟁심",new int[]{1,2,4,7,10},MAX_HP,10),
            RuneData.primary("vampire","혈귀의 룬","흡혈",new int[]{3,5,8,12,20},ATTACK,1),
            RuneData.primary("must_live","필생의 룬","생존력",new int[]{3,5,8,12,20},MAX_HP,10),
            RuneData.primary("charge","돌격의 룬","출격",new int[]{10,20,40,70,100},MAX_HP,10)
    ));
    private final List<RuneData> secondary=Collections.unmodifiableList(Arrays.asList(
            RuneData.secondary("smash","강격의 룬",ATTACK,1),
            RuneData.secondary("immovable","부동의 룬",DEFENSE,1),
            RuneData.secondary("mountain","태산의 룬",MAX_HP,20),
            RuneData.secondary("killing_intent","살의의 룬",CRITICAL_RATE,3),
            RuneData.secondary("afterimage","잔상의 룬",EVASION_RATE,1)
    ));
    public List<RuneData> primary(){return primary;} public List<RuneData> secondary(){return secondary;}
    public RuneData primary(String id){return find(primary,id);} public RuneData secondary(String id){return find(secondary,id);}
    private RuneData find(List<RuneData> values,String id){for(RuneData value:values)if(value.getId().equals(id))return value;return null;}
}
