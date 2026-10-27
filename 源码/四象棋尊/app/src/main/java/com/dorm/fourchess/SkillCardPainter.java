package com.dorm.fourchess;

import android.graphics.*;
import android.text.*;

/** Collectible manuscript composition, with semantic vector artwork and grade-specific engraving. */
final class SkillCardPainter {
    private final TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final SkillEmblemPainter emblem=new SkillEmblemPainter();
    private final float density,scaled;
    SkillCardPainter(android.content.Context context){density=context.getResources().getDisplayMetrics().density;scaled=context.getResources().getDisplayMetrics().scaledDensity;}
    private float dp(float n){return n*density;}
    RectF emblemBounds(RectF r){
        float pad=Math.min(dp(10),r.width()*.055f),h=r.height();
        float badgeH=Math.min(dp(20),h*.22f),badgeY=r.top+pad+badgeH/2;
        float top=badgeY+badgeH*.65f+dp(4),bottom=r.top+h*.54f;
        float size=Math.min(r.width()*.51f,Math.max(dp(12),bottom-top)),cy=(top+bottom)/2;
        return new RectF(r.centerX()-size/2,cy-size/2,r.centerX()+size/2,cy+size/2);
    }
    float minimumCardHeight(float width){return Math.max(width*.51f+dp(82)+scaled*26,(width*.50f+dp(12)+20*scaled)/.54f);}
    private int muted(int color,boolean disabled){return disabled?BattleSkin.blend(color,0xff27353c,.68f):color;}
    private void text(Canvas c,String value,float x,float y,float size,float width,int color,Paint.Align align){
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setTypeface(GameDesign.EMPHASIS);p.setTextSize(size);p.setColor(color);p.setTextAlign(align);
        if(p.measureText(value)>width)p.setTextSize(size*width/p.measureText(value));c.drawText(value,x,y-(p.ascent()+p.descent())/2,p);
    }
    StaticLayout description(String value,float width,float height,float preferred,int color){return layout(value,width,height,preferred,color,Integer.MAX_VALUE);}
    StaticLayout summary(String value,float width,float height,float preferred,int color){return layout(value,width,height,preferred,color,2);}
    private StaticLayout layout(String value,float width,float height,float preferred,int color,int maxLines){
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setTypeface(GameDesign.BODY);p.setColor(color);p.setTextAlign(Paint.Align.LEFT);
        StaticLayout result;float size=preferred;
        do{p.setTextSize(size);result=StaticLayout.Builder.obtain(value,0,value.length(),p,Math.max(1,(int)width)).setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).setLineSpacing(0,1.08f).build();
            if((result.getHeight()<=height&&result.getLineCount()<=maxLines)||size<=dp(5))break;size-=dp(.25f);
        }while(true);return result;
    }
    private Path frame(RectF r,SkillCardTheme t){
        float cut=dp(t.pattern==0?8:t.pattern==1?5:2);Path path=new Path();
        path.moveTo(r.left+cut,r.top);path.lineTo(r.right-cut,r.top);path.lineTo(r.right,r.top+cut);path.lineTo(r.right,r.bottom-cut);path.lineTo(r.right-cut,r.bottom);path.lineTo(r.left+cut,r.bottom);path.lineTo(r.left,r.bottom-cut);path.lineTo(r.left,r.top+cut);path.close();return path;
    }
    void draw(Canvas c,RectF r,int id,int remaining,boolean selected,boolean disabled,String reason,boolean compact,float shine){
        SkillCardTheme t=SkillCardTheme.forSkill(id);float pad=Math.min(dp(10),r.width()*.055f),h=r.height(),w=r.width();Path outline=frame(r,t);
        p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(0x20081016);c.save();c.translate(0,dp(1));c.drawPath(outline,p);c.restore();
        p.setColor(muted(t.middle,disabled));c.drawPath(outline,p);
        c.save();c.clipPath(outline);
        // Bound pages and a quiet reading panel replace the previous edge-to-edge spotlight gradients.
        p.setColor(GameDesign.alpha(muted(t.deep,disabled),90));c.drawRect(r.left,r.top+h*.54f,r.right,r.bottom,p);
        p.setColor(GameDesign.alpha(t.edge,18));c.drawRect(r.left,r.top,r.left+dp(3),r.bottom,p);
        fibres(c,r,t);if(!compact)ornament(c,r,t);p.setColor(GameDesign.alpha(t.edge,35));p.setStrokeWidth(dp(.5f));c.drawLine(r.left+pad,r.top+h*.54f,r.right-pad,r.top+h*.54f,p);
        c.restore();
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(selected?1.0f:.65f));p.setColor(disabled?0xff667777:selected?t.title:GameDesign.alpha(t.edge,190));c.drawPath(outline,p);
        corners(c,r,t,disabled);p.setStyle(Paint.Style.FILL);
        int title=disabled?0xffbec8c4:t.title,body=disabled?0xffa9b7b4:t.body;
        float badgeSize=Math.min(10*scaled,h*.15f),badgeH=Math.min(dp(20),h*.22f),badgeY=r.top+pad+badgeH/2;
        if(!compact){p.setColor(muted(t.badgeInk,disabled));c.drawRect(r.left+pad-dp(3),badgeY-badgeH*.6f,r.left+pad+badgeSize*2.6f,badgeY+badgeH*.6f,p);}
        text(c,t.label,r.left+pad,badgeY,badgeSize,w*.4f,disabled?body:t.edge,Paint.Align.LEFT);
        float countW=Math.min(w*.25f,Math.max(dp(26),badgeSize*2.6f)),countLeft=r.right-pad-countW;
        p.setColor(muted(t.badgeInk,disabled));c.drawRoundRect(countLeft,badgeY-badgeH/2,r.right-pad,badgeY+badgeH/2,dp(2),dp(2),p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(.5f));p.setColor(GameDesign.alpha(disabled?body:t.edge,150));c.drawRoundRect(countLeft,badgeY-badgeH/2,r.right-pad,badgeY+badgeH/2,dp(2),dp(2),p);p.setStyle(Paint.Style.FILL);
        text(c,"×"+remaining,countLeft+countW/2,badgeY,badgeSize,countW-dp(3),title,Paint.Align.CENTER);
        float titleSize=Math.min((compact?15:17)*scaled,Math.min(h*(compact?.23f:.12f),w*.17f));
        float titleY=r.top+h*(compact?.46f:.61f);
        if(!compact){
            emblem.draw(c,emblemBounds(r),id,t,disabled);
        }
        text(c,SkillRules.name(id),r.centerX(),titleY,titleSize,w-pad*2,title,Paint.Align.CENTER);
        float descTop=titleY+titleSize*.65f,descBottom=r.bottom-(compact?dp(4):Math.max(11*scaled+dp(7),h*.13f));
        String copy=compact?SkillCardTheme.brief(id):SkillCardCopy.summary(id);
        if(compact&&disabled&&reason!=null&&!reason.isEmpty())copy=reason;
        StaticLayout lines=summary(copy,w-pad*2,Math.max(dp(8),descBottom-descTop),compact?Math.min(9*scaled,h*.17f):11*scaled,body);
        c.save();c.translate(r.left+pad,descTop+Math.max(0,(descBottom-descTop-lines.getHeight())*.25f));lines.draw(c);c.restore();
        if(!compact){String rule=disabled&&reason!=null&&!reason.isEmpty()?reason:"整活".equals(SkillRules.tier(id))?"整活秘术 · 十回合换牌":"耗1次行动 · 十回合换牌";
            text(c,rule,r.centerX(),r.bottom-Math.max(6*scaled+dp(3),h*.055f),Math.min(9*scaled,h*.07f),w-pad*2,GameDesign.alpha(body,210),Paint.Align.CENTER);}
    }
    private void ornament(Canvas c,RectF r,SkillCardTheme t){
        c.save();c.translate(r.centerX(),r.top+r.height()*.30f);float unit=r.width()*.45f;c.scale(unit,unit);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.007f);p.setColor(GameDesign.alpha(t.edge,17));
        if(t.pattern==0){for(int side:new int[]{-1,1}){Path cloud=new Path();cloud.moveTo(side*.65f,-.27f);cloud.cubicTo(side*1.07f,-.66f,side*1.02f,.08f,side*.7f,.02f);cloud.cubicTo(side*.5f,-.09f,side*.7f,.46f,side*1.1f,.32f);c.drawPath(cloud,p);}}
        else if(t.pattern==1){Path stars=new Path();stars.moveTo(-1,-.2f);stars.lineTo(-.78f,.22f);stars.lineTo(-.96f,.5f);stars.moveTo(.8f,-.46f);stars.lineTo(1,-.16f);stars.lineTo(.86f,.22f);c.drawPath(stars,p);for(int side:new int[]{-1,1})c.drawCircle(side*.9f,-.16f,.022f,p);}
        else{for(int side:new int[]{-1,1}){Path bamboo=new Path();bamboo.moveTo(side*.9f,.5f);bamboo.quadTo(side*.63f,-.03f,side*.94f,-.5f);bamboo.moveTo(side*.82f,.12f);bamboo.quadTo(side*.57f,-.06f,side*.57f,-.21f);bamboo.moveTo(side*.83f,-.15f);bamboo.quadTo(side*1.07f,-.27f,side*1.03f,-.39f);c.drawPath(bamboo,p);}}
        c.restore();p.setStyle(Paint.Style.FILL);
    }
    private void fibres(Canvas c,RectF r,SkillCardTheme t){
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(.35f));p.setColor(GameDesign.alpha(t.body,5));
        for(int i=0;i<21;i++){float f=(i*.618034f)%1f,y=r.top+r.height()*f,x=r.left+r.width()*((i*.381966f)%1f);Path fibre=new Path();fibre.moveTo(x,y);fibre.quadTo(x+r.width()*.07f,y-dp(.5f),Math.min(r.right,x+r.width()*.2f),y+dp(.6f));c.drawPath(fibre,p);}p.setStyle(Paint.Style.FILL);
    }
    private void corners(Canvas c,RectF r,SkillCardTheme t,boolean disabled){
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(.6f));p.setColor(GameDesign.alpha(t.edge,disabled?35:110));
        if(t.pattern==0){for(int side=0;side<2;side++){float x=side==0?r.left:r.right,sign=side==0?1:-1;Path wing=new Path();wing.moveTo(x+sign*dp(4),r.top+dp(19));wing.lineTo(x+sign*dp(4),r.top+dp(9));wing.lineTo(x+sign*dp(10),r.top+dp(4));wing.lineTo(x+sign*dp(23),r.top+dp(4));c.drawPath(wing,p);c.drawLine(x+sign*dp(10),r.top+dp(8),x+sign*dp(15),r.top+dp(8),p);}}
        else if(t.pattern==1){float x=r.right-dp(5),y=r.bottom-dp(5);c.drawLine(x-dp(11),y,x,y,p);c.drawLine(x,y,x,y-dp(8),p);}
        else{c.drawLine(r.left+dp(7),r.bottom-dp(4),r.left+dp(18),r.bottom-dp(4),p);}p.setStyle(Paint.Style.FILL);
    }
}
