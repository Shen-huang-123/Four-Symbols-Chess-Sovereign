package com.dorm.fourchess;

import android.graphics.*;
import android.text.*;

/** Shared responsive card material: no game-state ownership, no skill-specific styling branches. */
final class SkillCardPainter {
    private final TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final float density,scaled;
    SkillCardPainter(android.content.Context context){density=context.getResources().getDisplayMetrics().density;scaled=context.getResources().getDisplayMetrics().scaledDensity;}
    private float dp(float n){return n*density;}
    private int muted(int color,boolean disabled){return disabled?BattleSkin.blend(color,0xff27353c,.68f):color;}
    private void text(Canvas c,String value,float x,float y,float size,float width,int color,Paint.Align align){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setTypeface(GameDesign.EMPHASIS);p.setTextSize(size);p.setColor(color);p.setTextAlign(align);String fit=TextUtils.ellipsize(value,p,Math.max(1,width),TextUtils.TruncateAt.END).toString();c.drawText(fit,x,y-(p.ascent()+p.descent())/2,p);}
    void draw(Canvas c,RectF r,int id,int remaining,boolean selected,boolean disabled,String reason,boolean compact,float shine){
        SkillCardTheme theme=SkillCardTheme.forSkill(id);float radius=dp(compact?7:8),pad=Math.min(dp(10),r.width()*.055f),height=r.height(),w=r.width();
        p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(0x33051018);c.drawRoundRect(r.left,r.top+dp(2),r.right,r.bottom+dp(2),radius,radius,p);
        p.setColor(Color.WHITE);p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{muted(theme.deep,disabled),muted(theme.middle,disabled),muted(theme.light,disabled)},new float[]{0,.55f,1},Shader.TileMode.CLAMP));c.drawRoundRect(r,radius,radius,p);p.setShader(null);
        if(selected){p.setColor(GameDesign.alpha(theme.edge,24));c.drawRoundRect(r,radius,radius,p);}
        c.save();Path clip=new Path();clip.addRoundRect(r,radius,radius,Path.Direction.CW);c.clipPath(clip);pattern(c,r,theme,disabled);
        if(shine>0&&shine<1){float x=r.left+w*(-.4f+1.8f*shine),band=w*.24f;p.setColor(Color.WHITE);p.setShader(new LinearGradient(x-band,r.top,x+band,r.bottom,new int[]{0x00ffffff,GameDesign.alpha(theme.title,(int)(65*Math.sin(shine*Math.PI))),0x00ffffff},null,Shader.TileMode.CLAMP));c.drawRect(r,p);p.setShader(null);}c.restore();
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(selected?1.4f:.8f));p.setColor(disabled?0xff718083:selected?theme.title:theme.edge);c.drawRoundRect(r,radius,radius,p);
        RectF inner=new RectF(r);inner.inset(dp(3),dp(3));p.setStrokeWidth(dp(.45f));p.setColor(GameDesign.alpha(theme.edge,disabled?35:85));c.drawRoundRect(inner,Math.max(dp(2),radius-dp(2)),Math.max(dp(2),radius-dp(2)),p);
        // Small geometric corner fretwork stays in the padding, away from all content.
        p.setColor(GameDesign.alpha(theme.edge,disabled?55:150));p.setStrokeWidth(dp(.6f));float k=Math.min(dp(9),height*.12f);for(int side=0;side<2;side++){float x=side==0?r.left+dp(4):r.right-dp(4),sign=side==0?1:-1;c.drawLine(x,r.bottom-dp(4),x+sign*k,r.bottom-dp(4),p);c.drawLine(x,r.bottom-dp(4),x,r.bottom-dp(4)-k,p);}p.setStyle(Paint.Style.FILL);
        int title=disabled?0xffc0c7c5:theme.title,body=disabled?0xffa8b4b4:theme.body;
        float badgeSize=Math.min(9*scaled,height*.17f),badgeH=Math.min(dp(17),height*.24f),top=r.top+pad+badgeH/2;
        float badgeW=Math.min(w*.37f,Math.max(dp(33),badgeSize*2.7f));p.setColor(disabled?0xff718083:theme.edge);c.drawRoundRect(r.left+pad,top-badgeH/2,r.left+pad+badgeW,top+badgeH/2,dp(3),dp(3),p);
        text(c,theme.label,r.left+pad+badgeW/2,top,badgeSize,badgeW-dp(4),theme.badgeInk,Paint.Align.CENTER);
        float countW=Math.min(w*.26f,Math.max(dp(28),badgeSize*2.8f)),countLeft=r.right-pad-countW;p.setColor(muted(theme.badgeInk,disabled));c.drawRoundRect(countLeft,top-badgeH/2,r.right-pad,top+badgeH/2,dp(4),dp(4),p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(.6f));p.setColor(disabled?0xff718083:theme.edge);c.drawRoundRect(countLeft,top-badgeH/2,r.right-pad,top+badgeH/2,dp(4),dp(4),p);p.setStyle(Paint.Style.FILL);
        text(c,"×"+remaining,countLeft+countW/2,top,badgeSize,countW-dp(3),title,Paint.Align.CENTER);
        float titleY=r.top+height*(compact?.49f:.43f),titleSize=Math.min((compact?16:24)*scaled,Math.min(height*(compact?.26f:.21f),w*.22f));
        text(c,SkillRules.name(id),r.centerX(),titleY,titleSize,w-pad*2,title,Paint.Align.CENTER);
        float descY=r.top+height*(compact?.81f:.65f),descSize=Math.min((compact?10:11)*scaled,height*(compact?.20f:.12f));
        p.setTypeface(GameDesign.BODY);text(c,disabled&&!reason.isEmpty()?reason:SkillCardTheme.brief(id),r.centerX(),descY,descSize,w-pad*2,body,Paint.Align.CENTER);
        if(!compact){String rule=disabled&&!reason.isEmpty()?reason:"整活".equals(SkillRules.tier(id))?"整活秘术 · 十回合换牌":"施放耗1次 · 十回合换牌";text(c,rule,r.centerX(),r.top+height*.87f,Math.min(9*scaled,height*.09f),w-pad*2,body,Paint.Align.CENTER);}
    }
    private void pattern(Canvas c,RectF r,SkillCardTheme t,boolean disabled){
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(.7f));p.setColor(GameDesign.alpha(t.edge,disabled?12:25));
        float x=r.right-r.width()*.19f,y=r.centerY(),unit=Math.min(r.width()*.21f,r.height()*.29f);Path path=new Path();
        if(t.pattern==0){ // Curling flame / cloud lines, on the outer right only.
            for(int i=0;i<3;i++){float offset=unit*i*.3f;path.reset();path.moveTo(x-unit*.5f,y+unit+offset);path.cubicTo(x+unit,y+unit*.4f,x-unit,y-unit*.3f,x+unit*.2f,y-unit-offset);path.cubicTo(x+unit*1.1f,y-unit*.1f,x+unit*.4f,y+unit*.6f,x+unit*.8f,y+unit);c.drawPath(path,p);}
        }else if(t.pattern==1){
            for(int i=0;i<3;i++){float yy=y+unit*(i-1)*.5f;path.reset();path.moveTo(x-unit,yy);path.cubicTo(x-unit*.3f,yy-unit*.5f,x+unit*.3f,yy+unit*.5f,x+unit,yy);c.drawPath(path,p);}c.drawLine(x,y-unit*1.2f,x,y-unit*.65f,p);c.drawLine(x-unit*.27f,y-unit*.93f,x+unit*.27f,y-unit*.93f,p);
        }else{
            c.drawLine(x-unit*.4f,y+unit,x+unit*.4f,y-unit,p);for(int i=0;i<3;i++){float yy=y+unit*(i-1)*.6f;path.reset();path.moveTo(x,yy);path.quadTo(x-unit*.8f,yy-unit*.65f,x-unit*.8f,yy-unit*.1f);path.quadTo(x-unit*.3f,yy+unit*.2f,x,yy);path.moveTo(x,yy);path.quadTo(x+unit*.75f,yy-unit*.5f,x+unit*.8f,yy);path.quadTo(x+unit*.3f,yy+unit*.2f,x,yy);c.drawPath(path,p);}
        }p.setStyle(Paint.Style.FILL);
    }
}
