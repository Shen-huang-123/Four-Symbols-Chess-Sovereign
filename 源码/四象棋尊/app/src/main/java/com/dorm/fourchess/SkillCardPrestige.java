package com.dorm.fourchess;

import android.graphics.*;

/** Cosmetic rarity treatment. Never reads or changes a skill's gameplay state. */
final class SkillCardPrestige {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path rim=new Path(),trail=new Path(),shape=new Path();
    private final PathMeasure measure=new PathMeasure();
    private final float[] point=new float[2];
    static final long LEGEND_PERIOD=6400,RARE_PERIOD=10400;
    static boolean animated(int id,boolean disabled,boolean motion){return motion&&!disabled&&SkillCardTheme.forSkill(id).pattern<2;}
    static long period(int id){return SkillCardTheme.forSkill(id).pattern==0?LEGEND_PERIOD:RARE_PERIOD;}
    static Path outline(RectF r,SkillCardTheme t,float d){
        float cut=(t.pattern==0?8:t.pattern==1?5:2)*d;Path result=new Path();
        result.moveTo(r.left+cut,r.top);result.lineTo(r.right-cut,r.top);result.lineTo(r.right,r.top+cut);result.lineTo(r.right,r.bottom-cut);result.lineTo(r.right-cut,r.bottom);result.lineTo(r.left+cut,r.bottom);result.lineTo(r.left,r.bottom-cut);result.lineTo(r.left,r.top+cut);result.close();return result;
    }
    void regalia(Canvas c,RectF r,SkillCardTheme t,float d,boolean compact){
        if(t.pattern!=0)return;
        c.save();c.clipPath(outline(r,t,d));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.65f*d);p.setShader(null);p.setColor(GameDesign.alpha(t.edge,180));
        // A single phoenix coronet, separated from both the quality and charge badges.
        float x=r.centerX(),y=r.top+(compact?5:8)*d,s=Math.min(r.width()*.095f,17*d);
        shape.reset();shape.moveTo(x-s*1.5f,y+s*.38f);shape.cubicTo(x-s*.8f,y+s*.4f,x-s*.9f,y-s*.18f,x-s*.45f,y+s*.04f);shape.lineTo(x,y+s*.5f);shape.lineTo(x+s*.45f,y+s*.04f);shape.cubicTo(x+s*.9f,y-s*.18f,x+s*.8f,y+s*.4f,x+s*1.5f,y+s*.38f);c.drawPath(shape,p);
        diamond(c,x,y+s*.16f,s*.14f,t.edge,210);
        if(!compact){
            // Open ruyi corner engravings; no second enclosing border.
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.65f*d);p.setColor(GameDesign.alpha(t.edge,155));
            for(int side=0;side<2;side++)for(int end=0;end<2;end++){
                float px=side==0?r.left:r.right,py=end==0?r.top:r.bottom,sx=side==0?1:-1,sy=end==0?1:-1;
                shape.reset();shape.moveTo(px+sx*5*d,py+sy*27*d);shape.lineTo(px+sx*5*d,py+sy*12*d);shape.quadTo(px+sx*11*d,py+sy*4*d,px+sx*18*d,py+sy*5*d);shape.cubicTo(px+sx*24*d,py+sy*3*d,px+sx*27*d,py+sy*8*d,px+sx*21*d,py+sy*9*d);c.drawPath(shape,p);
            }
            // A small gold mounting under the artwork, away from the skill name.
            float cy=r.top+r.height()*.535f,span=r.width()*.16f;
            p.setColor(GameDesign.alpha(t.edge,115));c.drawLine(x-span,cy,x-5*d,cy,p);c.drawLine(x+5*d,cy,x+span,cy,p);diamond(c,x,cy,2.4f*d,t.edge,200);
        }
        c.restore();p.setStyle(Paint.Style.FILL);
    }
    void metalRim(Canvas c,RectF r,SkillCardTheme t,float d,boolean selected){
        if(t.pattern!=0)return;
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth((selected?1.45f:1.15f)*d);
        p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{0xff8d632e,0xffffe9ad,0xffba853e,0xffefd390,0xff9e7035,0xffffe4a0},new float[]{0,.18f,.38f,.57f,.77f,1},Shader.TileMode.CLAMP));
        c.drawPath(outline(r,t,d),p);p.setShader(null);p.setStyle(Paint.Style.FILL);
    }
    void artworkMount(Canvas c,RectF field,SkillCardTheme t,float d){
        if(t.pattern!=0)return;
        c.save();c.clipRect(field);float x=field.centerX(),y=field.centerY(),radius=field.width()*.48f;
        p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(x,y,radius,new int[]{0x28e4be78,0x08e4be78,0x00e4be78},new float[]{0,.66f,1},Shader.TileMode.CLAMP));c.drawCircle(x,y,radius,p);p.setShader(null);
        // Broken sun-ray engraving rather than a ring shared by every skill.
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.45f*d);p.setColor(0x42e4be78);
        for(int i=0;i<12;i++){double a=(i*30+15)*Math.PI/180;float cos=(float)Math.cos(a),sin=(float)Math.sin(a);c.drawLine(x+cos*radius*.86f,y+sin*radius*.86f,x+cos*radius,y+sin*radius,p);}
        c.restore();p.setStyle(Paint.Style.FILL);
    }
    void ambient(Canvas c,RectF r,int id,long elapsed,float d,boolean compact,boolean disabled,boolean motion){
        if(!animated(id,disabled,motion)||elapsed<0)return;
        SkillCardTheme t=SkillCardTheme.forSkill(id);boolean legendary=t.pattern==0;
        float phase=(elapsed%period(id))/(float)period(id);
        // Rare silver glides once, then rests; legendary has two continuously orbiting gold glints.
        if(!legendary&&phase>.28f)return;
        c.save();c.clipRect(r);rim.set(outline(r,t,d));measure.setPath(rim,true);float length=measure.getLength();
        int heads=legendary?2:1;
        for(int head=0;head<heads;head++){
            float at=((legendary?phase:phase/.28f)+head*.5f)%1f*length;
            float tail=length*(legendary?.115f:.055f),strength=legendary?1:(float)Math.sin(Math.PI*phase/.28f)*.55f;
            for(int i=0;i<7;i++){
                float end=at-tail*(6-i)/7,start=end-tail/7;trail.reset();
                segment(start,end,length);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);
                p.setColor(GameDesign.alpha(t.edge,Math.round((12+i*7)*strength)));p.setStrokeWidth((legendary?3.5f:1.8f)*d);c.drawPath(trail,p);
                p.setColor(GameDesign.alpha(legendary?0xffffedb3:t.title,Math.round((50+i*30)*strength)));p.setStrokeWidth((legendary?1.25f:.7f)*d);c.drawPath(trail,p);
            }
            measure.getPosTan(at,point,null);
            if(legendary){float pulse=.65f+.35f*(float)Math.sin(phase*Math.PI*4+head);star(c,point[0],point[1],(compact?2.1f:3.2f)*d*pulse,t.title,235);}
        }
        if(legendary&&!compact){
            RectF field=new RectF(r);field.bottom=r.top+r.height()*.54f;
            c.save();c.clipRect(field);
            for(int i=0;i<3;i++){
                float cycle=(phase+i*.333f)%1f,opacity=(float)Math.pow(Math.sin(Math.PI*cycle),4);
                float x=r.left+r.width()*new float[]{.25f,.77f,.69f}[i],y=r.top+r.height()*(.40f-cycle*.16f);
                star(c,x,y,(1.1f+opacity)*d,t.edge,Math.round(opacity*140));
            }c.restore();
        }
        c.restore();p.setShader(null);p.setStyle(Paint.Style.FILL);
    }
    private void segment(float start,float end,float length){
        if(end<0){start+=length;end+=length;}
        if(start<0){measure.getSegment(length+start,length,trail,true);measure.getSegment(0,end,trail,true);}
        else measure.getSegment(start,end,trail,true);
    }
    private void diamond(Canvas c,float x,float y,float size,int color,int alpha){shape.reset();shape.moveTo(x,y-size);shape.lineTo(x+size*.58f,y);shape.lineTo(x,y+size);shape.lineTo(x-size*.58f,y);shape.close();p.setStyle(Paint.Style.FILL);p.setColor(GameDesign.alpha(color,alpha));c.drawPath(shape,p);}
    private void star(Canvas c,float x,float y,float s,int color,int alpha){
        shape.reset();shape.moveTo(x,y-s);shape.lineTo(x+s*.21f,y-s*.21f);shape.lineTo(x+s*.7f,y);shape.lineTo(x+s*.21f,y+s*.21f);shape.lineTo(x,y+s);shape.lineTo(x-s*.21f,y+s*.21f);shape.lineTo(x-s*.7f,y);shape.lineTo(x-s*.21f,y-s*.21f);shape.close();p.setStyle(Paint.Style.FILL);p.setColor(GameDesign.alpha(color,alpha));c.drawPath(shape,p);
    }
}
