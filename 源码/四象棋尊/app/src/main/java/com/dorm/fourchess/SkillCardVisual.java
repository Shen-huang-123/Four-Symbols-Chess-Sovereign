package com.dorm.fourchess;

/** Pure art registry. IDs and rarity come from existing skill definitions, never redealt. */
final class SkillCardVisual {
    enum Pattern { WIND, STRIKE, BRONZE, INK }
    enum Effect { TRAIL, RIFT, IMPACT, GUARD, SPIRIT }
    final int skillId,artworkResource;
    final String illustrationOverride;
    final Pattern background;
    final Effect effect;
    private SkillCardVisual(int id,int art,Pattern pattern,Effect fx){skillId=id;artworkResource=art;background=pattern;effect=fx;illustrationOverride="skill_illustration_"+(id<10?"0":"")+id;}
    SkillCardTheme theme(){return SkillCardTheme.forSkill(skillId);}
    private static SkillCardVisual v(int id,int art,Pattern pattern,Effect fx){return new SkillCardVisual(id,art,pattern,fx);}
    private static final SkillCardVisual[] ENTRIES={
        v(1,R.drawable.skill_art_01,Pattern.WIND,Effect.TRAIL),
        v(2,R.drawable.skill_art_02,Pattern.WIND,Effect.RIFT),
        v(3,R.drawable.skill_art_03,Pattern.INK,Effect.SPIRIT),
        v(4,R.drawable.skill_art_04,Pattern.INK,Effect.RIFT),
        v(5,R.drawable.skill_art_05,Pattern.WIND,Effect.TRAIL),
        v(6,R.drawable.skill_art_06,Pattern.WIND,Effect.RIFT),
        v(7,R.drawable.skill_art_07,Pattern.STRIKE,Effect.IMPACT),
        v(8,R.drawable.skill_art_08,Pattern.STRIKE,Effect.IMPACT),
        v(9,R.drawable.skill_art_09,Pattern.STRIKE,Effect.IMPACT),
        v(10,R.drawable.skill_art_10,Pattern.WIND,Effect.RIFT),
        v(11,R.drawable.skill_art_11,Pattern.WIND,Effect.RIFT),
        v(12,R.drawable.skill_art_12,Pattern.WIND,Effect.RIFT),
        v(13,R.drawable.skill_art_13,Pattern.BRONZE,Effect.GUARD),
        v(14,R.drawable.skill_art_14,Pattern.WIND,Effect.TRAIL),
        v(15,R.drawable.skill_art_15,Pattern.BRONZE,Effect.GUARD),
        v(16,R.drawable.skill_art_16,Pattern.WIND,Effect.TRAIL),
        v(17,R.drawable.skill_art_17,Pattern.WIND,Effect.TRAIL),
        v(18,R.drawable.skill_art_18,Pattern.STRIKE,Effect.IMPACT),
        v(19,R.drawable.skill_art_19,Pattern.STRIKE,Effect.IMPACT),
        v(20,R.drawable.skill_art_20,Pattern.INK,Effect.SPIRIT),
        v(21,R.drawable.skill_art_21,Pattern.INK,Effect.SPIRIT),
        v(22,R.drawable.skill_art_22,Pattern.INK,Effect.SPIRIT),
        v(23,R.drawable.skill_art_23,Pattern.BRONZE,Effect.GUARD),
        v(24,R.drawable.skill_art_24,Pattern.INK,Effect.SPIRIT),
        v(25,R.drawable.skill_art_25,Pattern.INK,Effect.RIFT),
        v(26,R.drawable.skill_art_26,Pattern.INK,Effect.SPIRIT),
        v(27,R.drawable.skill_art_27,Pattern.INK,Effect.SPIRIT),
        v(28,R.drawable.skill_art_28,Pattern.INK,Effect.SPIRIT),
        v(29,R.drawable.skill_art_29,Pattern.INK,Effect.SPIRIT),
        v(30,R.drawable.skill_art_30,Pattern.STRIKE,Effect.IMPACT)
    };
    static SkillCardVisual of(int id){if(id<1||id>ENTRIES.length)throw new IllegalArgumentException("unknown skill art");return ENTRIES[id-1];}
}
