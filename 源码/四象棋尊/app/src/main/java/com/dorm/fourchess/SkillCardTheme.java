package com.dorm.fourchess;

/** Presentation mapping only. Stable skill IDs, effects and original tier labels are untouched. */
final class SkillCardTheme {
    static final SkillCardTheme LEGENDARY=new SkillCardTheme("legendary","传说",0xff681d25,0xff7e292d,0xff963b35,0xffc5a973,0xfff5e5bf,0xffeadac6,0xff461c22,0);
    static final SkillCardTheme RARE=new SkillCardTheme("rare","稀有",0xff203746,0xff294b5e,0xff355a6c,0xff99b6c3,0xffe5eff1,0xffd6e4e7,0xff1b303e,1);
    static final SkillCardTheme COMMON=new SkillCardTheme("common","常规",0xff263f36,0xff315447,0xff3d6250,0xff9bbba5,0xffedf1df,0xffdce7d8,0xff20362e,2);
    final String key,label;final int deep,middle,light,edge,title,body,badgeInk,pattern;
    private SkillCardTheme(String key,String label,int deep,int middle,int light,int edge,int title,int body,int ink,int pattern){this.key=key;this.label=label;this.deep=deep;this.middle=middle;this.light=light;this.edge=edge;this.title=title;this.body=body;badgeInk=ink;this.pattern=pattern;}
    static SkillCardTheme forSkill(int id){String tier=SkillRules.tier(id);return "传说".equals(tier)?LEGENDARY:"奇策".equals(tier)?RARE:COMMON;}
    static String label(int id){return forSkill(id).label+("整活".equals(SkillRules.tier(id))?" · 整活":"");}
    static String brief(int id){String s=SkillRules.DETAILS[id-1];int end=s.indexOf('，');if(end<0)end=s.indexOf('。');return end>0?s.substring(0,end):s;}
}
