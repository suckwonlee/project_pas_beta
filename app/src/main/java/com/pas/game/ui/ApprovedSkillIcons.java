package com.pas.game.ui;

import com.pas.game.R;

/** Approved skill artwork shared by selection and battle screens. */
public final class ApprovedSkillIcons {
    private ApprovedSkillIcons() {}

    public static int find(String id) {
        if (id == null) return 0;
        switch (id) {
            case "banish_profane": return R.drawable.skill_cleric_banish_profane;
            case "holy_strike": return R.drawable.skill_cleric_holy_strike;
            case "purifying_flame": return R.drawable.skill_cleric_purifying_flame;
            case "shield_of_faith": return R.drawable.skill_cleric_shield_of_faith;
            case "healing_prayer": return R.drawable.skill_cleric_healing_prayer;
            // TODO: 신성 개입 이미지 완성 후 임시 아이콘 교체.
            case "divine_intervention": return R.drawable.placeholder_defense;
            case "slash": return R.drawable.skill_hero_slash_v2;
            case "shield_art": return R.drawable.skill_hero_shield_art_v2;
            case "sword_faith": return R.drawable.skill_hero_sword_faith_v2;
            case "defend": return R.drawable.skill_hero_defend_v2;
            case "weapon_guard": return R.drawable.skill_hero_weapon_guard_v2;
            case "unyielding_faith": return R.drawable.skill_hero_unyielding_faith_v2;
            case "head_bash": return R.drawable.skill_hero_head_bash_v2;
            case "first_aid": return R.drawable.skill_hero_first_aid_v2;
            case "certain_strike": return R.drawable.skill_hero_certain_strike_v2;
            case "defense_focus": return R.drawable.skill_hero_defense_focus_v2;
            case "guardian_aura": return R.drawable.skill_hero_guardian_aura_v3;
            case "battle_cry": return R.drawable.skill_hero_battle_cry_v3;
            case "stone_throw": return R.drawable.skill_hero_stone_throw_v2;
            case "mangle": return R.drawable.skill_hero_mangle_v2;
            case "reckless_charge": return R.drawable.skill_hero_reckless_charge_v2;
            case "resolve": return R.drawable.skill_hero_resolve_v2;
            case "full_will": return R.drawable.skill_hero_full_will_v2;
            case "hero_swordsmanship": return R.drawable.skill_hero_swordsmanship_v7;
            case "judgment": return R.drawable.skill_judgment;
            case "salvation_vow": return R.drawable.skill_hero_salvation_vow_v4;
            case "archery": return R.drawable.skill_hunter_archery;
            case "rapid_fire": return R.drawable.skill_hunter_rapid_fire;
            case "brow_shot": return R.drawable.skill_hunter_brow_shot_v2;
            case "hunter_defend": return R.drawable.skill_hunter_defend;
            case "evasion_focus": return R.drawable.skill_hunter_evasion_focus_v2;
            case "emergency_escape": return R.drawable.skill_hunter_emergency_escape_v2;
            case "fixed_trap": return R.drawable.skill_hunter_fixed_trap_v2;
            case "stealth_movement": return R.drawable.skill_hunter_stealth_movement_v2;
            case "cold_aim": return R.drawable.skill_hunter_cold_aim_v2;
            case "venom_injection": return R.drawable.skill_hunter_venom_injection;
            case "piercing_shot": return R.drawable.skill_hunter_piercing_shot_v2;
            case "install_decoy": return R.drawable.skill_hunter_install_decoy_v2;
            case "swift_movement": return R.drawable.skill_hunter_swift_movement_v2;
            case "venom_chase": return R.drawable.skill_hunter_venom_chase_v2;
            case "execution_shot": return R.drawable.skill_hunter_execution_shot;
            case "joint_shot": return R.drawable.skill_hunter_joint_shot;
            case "solitude": return R.drawable.skill_hunter_solitude_v2;
            case "arrow_rain": return R.drawable.skill_hunter_arrow_rain_v2;
            case "deadly_poison_curse": return R.drawable.skill_hunter_deadly_poison_curse_v4;
            case "shadow_chase": return R.drawable.skill_hunter_shadow_chase_v2;
            case "blessing_of_resolve": return R.drawable.skill_cleric_blessing_of_resolve;
            case "healing_touch": return R.drawable.skill_cleric_healing_touch;
            case "absolution": return R.drawable.skill_cleric_absolution;
            case "blessing_of_life": return R.drawable.skill_cleric_blessing_of_life;
            case "blessing_of_courage": return R.drawable.skill_cleric_blessing_of_courage;
            case "healing_wave": return R.drawable.skill_cleric_healing_wave;
            case "heavenly_passage": return R.drawable.skill_cleric_heavenly_passage;
            case "baptism_of_purification": return R.drawable.skill_cleric_baptism_of_purification;
            case "withdraw_grace": return R.drawable.skill_cleric_withdraw_grace;
            case "answered_prayer": return R.drawable.skill_cleric_answered_prayer;
            case "crusade": return R.drawable.skill_cleric_crusade;
            case "sanctuary": return R.drawable.skill_cleric_sanctuary;
            case "divine_fragment": return R.drawable.skill_cleric_divine_fragment;
            case "divine_punishment": return R.drawable.skill_cleric_divine_punishment;
            case "mana_discharge": return R.drawable.skill_wizard_mana_discharge;
            case "gluttonous_hand": return R.drawable.skill_wizard_gluttonous_hand;
            case "nether_repulsion": return R.drawable.skill_wizard_nether_repulsion;
            case "magic_ward": return R.drawable.skill_wizard_magic_ward;
            case "chaos_distortion": return R.drawable.skill_wizard_chaos_distortion;
            case "devour": return R.drawable.skill_wizard_devour;
            case "mana_disruption": return R.drawable.skill_wizard_mana_disruption;
            case "phantom_body": return R.drawable.skill_wizard_phantom_body;
            case "mana_payment": return R.drawable.skill_wizard_mana_payment;
            case "abyssal_mark": return R.drawable.skill_wizard_abyssal_mark;
            case "dimensional_drift": return R.drawable.skill_wizard_dimensional_drift;
            case "otherworld_power": return R.drawable.skill_wizard_otherworld_power;
            case "hungry_star": return R.drawable.skill_wizard_hungry_star;
            case "past_echo": return R.drawable.skill_wizard_past_echo;
            case "spell_theft": return R.drawable.skill_wizard_spell_theft;
            case "existence_loan": return R.drawable.skill_wizard_existence_loan;
            case "infinite_fragment": return R.drawable.skill_wizard_infinite_fragment;
            case "double_cast": return R.drawable.skill_wizard_double_cast;
            case "meteor_shower": return R.drawable.skill_wizard_meteor_shower;
            case "otherworld_gate": return R.drawable.skill_wizard_otherworld_gate;
            default: return 0;
        }
    }
}
