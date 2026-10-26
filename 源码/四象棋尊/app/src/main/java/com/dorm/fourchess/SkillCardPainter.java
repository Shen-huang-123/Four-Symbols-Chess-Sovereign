package com.dorm.fourchess;

import android.graphics.*;
import android.text.*;

/** Matte manuscript material. Coordinates and game state remain owned by callers. */
final class SkillCardPainter {
    private final TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final float density,scaled;
    SkillCardPainter(android.content.Context context){density=context.getResources().getDisplayMetrics().density;scaled=context.getResources().getDisplayMetrics().scaledDensity;}
    private float dp(float n){return n*density;}
    private int muted(int color,boolean disabled){return disabled?BattleSkin.blend(color,0xff27353c,.68f):color;}
    private void text(Canvas c,String value,float x,float y,float size,float width,int color,Paint.Align align){
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setTypeface(GameDesign.EMPHASIS);p.setTextSize(size);p.setColor(color);p.setTextAlign(align);
        if(p.measureText(value)>width)p.setTextSize(size*width/p.measureText(value));
        c.drawText(value,x,y-(p.ascent()+p.descent())/2,p);
    }
    /** Complete copy, wrapped without ellipsis; fit is determined by both actual width and height. */
    StaticLayout description(String value,float width,float height,float preferred,int color){
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setTypeface(GameDesign.BODY);p.setColor(color);p.setTextAlign(Paint.Align.LEFT);
        StaticLayout result;float size=preferred;
        do{p.setTextSize(size);result=StaticLayout.Builder.obtain(value,0,value.length(),p,Math.max(1,(int)width))
            .setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).setLineSpacing(0,1.08f).build();
            if(result.getHeight()<=height||size<=dp(5))break;size-=dp(.25f);
        }while(true);
        return result;
    }
    void draw(Canvas c,RectF r,int id,int remaining,boolean selected,boolean disabled,String reason,boolean compact,float shine){
        SkillCardTheme t=SkillCardTheme.forSkill(id);float radius=dp(5),pad=Math.min(dp(10),r.width()*.05f),h=r.height(),w=r.width();
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(0x20091016);c.drawRoundRect(r.left,r.top+dp(1),r.right,r.bottom+dp(1),radius,radius,p);
        // The base is pigment, not a luminous gradient: only a small tonal drift from the middle tone.
        int top=BattleSkin.blend(t.middle,t.light,.10f),bottom=BattleSkin.blend(t.middle,t.deep,.16f);
        p.setColor(Color.WHITE);p.setShader(new LinearGradient(r.left,r.top,r.left,r.bottom,muted(top,disabled),muted(bottom,disabled),Shader.TileMode.CLAMP));
        c.drawRoundRect(r,radius,radius,p);p.setShader(null);
        c.save();Path clip=new Path();clip.addRoundRect(r,radius,radius,Path.Direction.CW);c.clipPath(clip);
        fibres(c,r,t,disabled);pattern(c,r,t,disabled);c.restore();
        // A single etched perimeter; selection changes the line rather than adding another shell.
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(selected?.95f:.65f));p.setColor(disabled?0xff667777:selected?t.title:GameDesign.alpha(t.edge,185));c.drawRoundRect(r,radius,radius,p);
        // One restrained binding mark, rather than four repeated ornamental corners.
        p.setStrokeWidth(dp(.55f));p.setColor(GameDesign.alpha(t.edge,85));c.drawLine(r.left+pad,r.bottom-dp(4),r.left+pad+dp(12),r.bottom-dp(4),p);p.setStyle(Paint.Style.FILL);
        int title=disabled?0xffbec8c4:t.title,body=disabled?0xffa9b7b4:t.body;
        float badgeSize=Math.min(9*scaled,h*.16f),badgeH=Math.min(dp(16),h*.22f),badgeY=r.top+pad+badgeH/2;
        // Quality is a typographic seal, not a brightly filled capsule.
        text(c,t.label,r.left+pad,badgeY,badgeSize,w*.4f,disabled?body:t.edge,Paint.Align.LEFT);
        float countW=Math.min(w*.25f,Math.max(dp(26),badgeSize*2.6f)),countLeft=r.right-pad-countW;
        p.setColor(muted(t.badgeInk,disabled));c.drawRoundRect(countLeft,badgeY-badgeH/2,r.right-pad,badgeY+badgeH/2,dp(2),dp(2),p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(.5f));p.setColor(GameDesign.alpha(disabled?body:t.edge,165));c.drawRoundRect(countLeft,badgeY-badgeH/2,r.right-pad,badgeY+badgeH/2,dp(2),dp(2),p);p.setStyle(Paint.Style.FILL);
        text(c,"×"+remaining,countLeft+countW/2,badgeY,badgeSize,countW-dp(3),title,Paint.Align.CENTER);
        float titleSize=Math.min((compact?15:20)*scaled,Math.min(h*(compact?.23f:.18f),w*.18f));
        float titleY=compact?r.top+h*.46f:Math.max(badgeY+badgeH*.6f+titleSize*.75f,r.top+h*.34f);
        text(c,SkillRules.name(id),r.centerX(),titleY,titleSize,w-pad*2,title,Paint.Align.CENTER);
        float descTop=titleY+titleSize*.65f,descBottom=r.bottom-(compact?dp(4):Math.max(dp(18),h*.15f));
        String copy=compact?SkillCardTheme.brief(id):SkillRules.DETAILS[id-1];
        if(disabled&&reason!=null&&!reason.isEmpty())copy=compact?reason:copy+"\n"+reason;
        StaticLayout lines=description(copy,w-pad*2,Math.max(dp(8),descBottom-descTop),Math.min((compact?9:11)*scaled,h*(compact?.17f:.11f)),body);
        c.save();c.translate(r.left+pad,descTop+Math.max(0,(descBottom-descTop-lines.getHeight())*.35f));lines.draw(c);c.restore();
        if(!compact){String rule="整活".equals(SkillRules.tier(id))?"整活秘术 · 十回合换牌":"施放耗1次 · 十回合换牌";
            text(c,rule,r.centerX(),r.bottom-Math.max(dp(8),h*.065f),Math.min(8*scaled,h*.08f),w-pad*2,GameDesign.alpha(body,210),Paint.Align.CENTER);}
    }
    private void fibres(Canvas c,RectF r,SkillCardTheme t,boolean disabled){
        // Deterministic sub-visible curved fibres: no bitmap tiling, random noise or per-frame flicker.
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(.35f));p.setColor(GameDesign.alpha(t.body,disabled?3:5));
        for(int i=0;i<17;i++){float f=(i*.618034f)%1f,y=r.top+r.height()*f,x=r.left+r.width()*((i*.381966f)%1f);Path path=new Path();path.moveTo(x,y);path.quadTo(x+r.width()*.09f,y-dp(.5f),Math.min(r.right,x+r.width()*.26f),y+dp(.6f));c.drawPath(path,p);}
        p.setStyle(Paint.Style.FILL);
    }
    private void pattern(Canvas c,RectF r,SkillCardTheme t,boolean disabled){
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(.6f));p.setColor(GameDesign.alpha(t.edge,disabled?8:17));
        float x=r.right-r.width()*.12f,y=r.centerY(),u=Math.min(r.width()*.2f,r.height()*.3f);Path path=new Path();
        if(t.pattern==0){for(int i=0;i<2;i++){path.reset();path.moveTo(x-u*.6f,y+u);path.cubicTo(x+u,y+u*.4f,x-u,y-u*.3f,x+u*.2f,y-u-i*u*.25f);path.cubicTo(x+u,y-u*.1f,x+u*.4f,y+u*.6f,x+u*.8f,y+u);c.drawPath(path,p);}}
        else if(t.pattern==1){for(int i=0;i<3;i++){float yy=y+u*(i-1)*.5f;path.reset();path.moveTo(x-u,yy);path.cubicTo(x-u*.3f,yy-u*.5f,x+u*.3f,yy+u*.5f,x+u,yy);c.drawPath(path,p);}}
        else{c.drawLine(x-u*.4f,y+u,x+u*.4f,y-u,p);for(int i=0;i<3;i++){float yy=y+u*(i-1)*.6f;path.reset();path.moveTo(x,yy);path.quadTo(x-u*.8f,yy-u*.65f,x-u*.8f,yy-u*.1f);path.quadTo(x-u*.3f,yy+u*.2f,x,yy);c.drawPath(path,p);}}
        p.setStyle(Paint.Style.FILL);
    }
}
