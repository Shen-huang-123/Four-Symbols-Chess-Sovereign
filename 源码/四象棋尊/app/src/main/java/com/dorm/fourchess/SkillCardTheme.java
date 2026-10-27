package com.dorm.fourchess;

/** Presentation mapping only. Stable skill IDs, effects and original tier labels are untouched. */
final class SkillCardTheme {
    static final SkillCardTheme LEGENDARY=new SkillCardTheme("legendary","传说",0xff481e29,0xff68252c,0xff89333a,0xffe5c181,0xffffedc8,0xfffff0dc,0xff4c2027,0);
    static final SkillCardTheme RARE=new SkillCardTheme("rare","稀有",0xff182c3b,0xff203b50,0xff2b536b,0xffa8d4e8,0xffeaf6ff,0xffdeedf4,0xff152c45,1);
    static final SkillCardTheme COMMON=new SkillCardTheme("common","常规",0xff1e332e,0xff28483d,0xff355b4b,0xffa8ceb0,0xfff0f3df,0xffe2eddd,0xff1a3631,2);
    final String key,label;final int deep,middle,light,edge,title,body,badgeInk,pattern;
    private SkillCardTheme(String key,String label,int deep,int middle,int light,int edge,int title,int body,int ink,int pattern){this.key=key;this.label=label;this.deep=deep;this.middle=middle;this.light=light;this.edge=edge;this.title=title;this.body=body;badgeInk=ink;this.pattern=pattern;}
    static SkillCardTheme forSkill(int id){String tier=SkillRules.tier(id);return "传说".equals(tier)?LEGENDARY:"奇策".equals(tier)?RARE:COMMON;}
    static String label(int id){return forSkill(id).label+("整活".equals(SkillRules.tier(id))?" · 整活":"");}
    static String brief(int id){String s=SkillRules.DETAILS[id-1];int end=s.indexOf('，');if(end<0)end=s.indexOf('。');return end>0?s.substring(0,end):s;}
}
