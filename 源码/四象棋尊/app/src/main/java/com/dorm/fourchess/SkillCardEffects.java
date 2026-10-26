package com.dorm.fourchess;

import android.graphics.*;

/** Finite, quality-scaled effects. Draws only inside the artwork field, never over rules. */
final class SkillCardEffects {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF orbit=new RectF();
    static int duration(int id,boolean selection){
        int grade=SkillCardTheme.forSkill(id).pattern;
        return selection?(grade==0?950:grade==1?650:400):(grade==0?1450:grade==1?1000:650);
    }
    static boolean active(int id,long elapsed,boolean selection){return elapsed>=0&&elapsed<duration(id,selection);}
    void draw(Canvas c,RectF field,int id,long elapsed,boolean selection){
        if(!active(id,elapsed,selection))return;
        SkillCardTheme t=SkillCardTheme.forSkill(id);
        float progress=elapsed/(float)duration(id,selection);
        float envelope=(float)Math.sin(Math.PI*progress),size=Math.min(field.width(),field.height());
        if(size<=0)return;
        c.save();c.clipRect(field);c.translate(field.centerX(),field.centerY());c.scale(size/2,size/2);
        p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(.018f);
        p.setColor(GameDesign.alpha(t.edge,Math.round(envelope*(selection?145:205))));
        float radius=.57f+.22f*progress;
        orbit.set(-radius,-radius,radius,radius);
        if(t.pattern==2){
            // Common: two jade strokes unfurl, with four drifting leaf cuts.
            c.drawArc(orbit,200-35*progress,105*progress,false,p);
            c.drawArc(orbit,20-35*progress,105*progress,false,p);
            for(int i=0;i<4;i++){double a=i*Math.PI/2+progress*.3;float r=.65f+progress*.2f;
                float x=(float)Math.cos(a)*r,y=(float)Math.sin(a)*r;
                c.drawLine(x,y,x+.07f,y-.045f,p);
            }
        }else if(t.pattern==1){
            // Rare: counter-rotating constellation trails and six silver stars.
            c.drawArc(orbit,progress*150,145*envelope,false,p);
            c.drawArc(orbit,180-progress*110,110*envelope,false,p);
            for(int i=0;i<6;i++){double a=i*Math.PI/3+progress*.9;float x=(float)Math.cos(a)*radius,y=(float)Math.sin(a)*radius;
                float s=.018f+.026f*envelope;c.drawLine(x-s,y,x+s,y,p);c.drawLine(x,y-s,x,y+s,p);
            }
        }else{
            // Legendary: ritual rays, seal corners and twelve warm-gold comet cuts.
            c.drawArc(orbit,-90+progress*100,270*envelope,false,p);
            float outer=.79f+.1f*progress;
            for(int i=0;i<8;i++){double a=i*Math.PI/4;float x=(float)Math.cos(a),y=(float)Math.sin(a);
                c.drawLine(x*(outer-.13f*envelope),y*(outer-.13f*envelope),x*outer,y*outer,p);
            }
            c.save();c.rotate(45*(1-progress));float s=.6f+.1f*progress,l=.12f*envelope;
            for(int x:new int[]{-1,1})for(int y:new int[]{-1,1}){
                c.drawLine(x*s,y*s,x*(s-l),y*s,p);c.drawLine(x*s,y*s,x*s,y*(s-l),p);
            }c.restore();
            for(int i=0;i<12;i++){double a=i*Math.PI/6+progress*.48;float r=.73f+.2f*progress;
                float x=(float)Math.cos(a)*r,y=(float)Math.sin(a)*r;
                c.drawLine(x,y,x-(float)Math.cos(a-.3)*.06f*envelope,y-(float)Math.sin(a-.3)*.06f*envelope,p);
            }
        }
        c.restore();
    }
}
