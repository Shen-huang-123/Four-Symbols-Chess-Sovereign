package com.dorm.fourchess;

/** Presentation mapping only. Stable skill IDs, effects and original tier labels are untouched. */
final class SkillCardTheme {
    static final SkillCardTheme LEGENDARY=new SkillCardTheme("legendary","传说",0xff6e171f,0xffa8242b,0xffc63b32,0xfff3d58a,0xfffff0c2,0xfffff0c2,0xff59121b,0);
    static final SkillCardTheme RARE=new SkillCardTheme("rare","稀有",0xff152e49,0xff214d70,0xff2f688a,0xffa9d8f0,0xffeaf7ff,0xffe1f1f8,0xff122a43,1);
    static final SkillCardTheme COMMON=new SkillCardTheme("common","常规",0xff173a32,0xff245d49,0xff33765b,0xffa8d9b8,0xfff1f7e9,0xffedf6ef,0xff12342b,2);
    final String key,label;final int deep,middle,light,edge,title,body,badgeInk,pattern;
    private SkillCardTheme(String key,String label,int deep,int middle,int light,int edge,int title,int body,int ink,int pattern){this.key=key;this.label=label;this.deep=deep;this.middle=middle;this.light=light;this.edge=edge;this.title=title;this.body=body;badgeInk=ink;this.pattern=pattern;}
    static SkillCardTheme forSkill(int id){String tier=SkillRules.tier(id);return "传说".equals(tier)?LEGENDARY:"奇策".equals(tier)?RARE:COMMON;}
    static String label(int id){return forSkill(id).label+("整活".equals(SkillRules.tier(id))?" · 整活":"");}
    static String brief(int id){String s=SkillRules.DETAILS[id-1];int end=s.indexOf('，');if(end<0)end=s.indexOf('。');return end>0?s.substring(0,end):s;}
}
