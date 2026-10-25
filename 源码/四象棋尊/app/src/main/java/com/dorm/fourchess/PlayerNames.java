package com.dorm.fourchess;

/** Shared validation for saved names, the HUD and LAN messages. */
final class PlayerNames {
    static final String[] DEFAULTS={"红尘弈心","花间一弈","清风入弈","金陵弈梦"};
    static String clean(String value,String fallback){
        StringBuilder result=new StringBuilder();if(value!=null)for(int at=0,count=0;at<value.length()&&count<12;){int cp=value.codePointAt(at);at+=Character.charCount(cp);if(Character.isISOControl(cp)||Character.getType(cp)==Character.FORMAT||cp=='|')continue;result.appendCodePoint(cp);count++;}
        String text=result.toString().trim();return text.isEmpty()?fallback:text;
    }
    private PlayerNames(){}
}