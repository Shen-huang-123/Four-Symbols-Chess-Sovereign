package com.dorm.fourchess;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Layered vector illustrations with an optional drop-in transparent artwork per skill. */
final class SkillEmblemPainter {
    private final Context context;
    private final Drawable[] art=new Drawable[SkillRules.COUNT];
    private static final ColorMatrixColorFilter DISABLED;
    static {ColorMatrix tone=new ColorMatrix();tone.setSaturation(.12f);DISABLED=new ColorMatrixColorFilter(tone);}
    SkillEmblemPainter(Context context){this.context=context;}
    private Drawable artwork(int id){
        if(art[id-1]==null){SkillCardVisual visual=SkillCardVisual.of(id);int override=context.getResources().getIdentifier(visual.illustrationOverride,"drawable",context.getPackageName());
            art[id-1]=context.getDrawable(override!=0?override:visual.artworkResource).mutate();}
        return art[id-1];
    }
    void draw(Canvas c,RectF field,int id,SkillCardTheme theme,boolean disabled){
        if(id<1||id>SkillRules.COUNT||field.isEmpty())return;
        Drawable subject=artwork(id);subject.setColorFilter(disabled?DISABLED:null);subject.setAlpha(disabled?145:255);
        float size=Math.min(field.width(),field.height());int left=Math.round(field.centerX()-size/2),top=Math.round(field.centerY()-size/2);
        subject.setBounds(left,top,left+Math.round(size),top+Math.round(size));
        c.save();c.clipRect(field);subject.draw(c);c.restore();
    }
}
