package com.pas.game.skill.data;

public enum SkillGrade {
    NORMAL("일반"), UNCOMMON("비범"), MAGIC("마법"), EPIC("서사"), LEGENDARY("전설"), OTHERWORLD("이계");
    private final String label;
    SkillGrade(String label) { this.label = label; }
    public String getLabel() { return label; }
    public SkillGrade upgraded(int steps) {
        int index = Math.min(values().length - 1, ordinal() + Math.max(0, steps));
        return values()[index];
    }
}
