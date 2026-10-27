package com.dorm.fourchess;

import android.graphics.*;

/** Shared sealed back. No skill ID or quality enters this renderer. */
final class SecretScrollBackPainter {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private void stroke(Canvas c,Path path,int alpha,float width){p.setStyle(Paint.Style.STROKE);p.setColor(GameDesign.alpha(0xffc9ad76,alpha));p.setStrokeWidth(width);c.drawPath(path,p);}
    void draw(Canvas c,RectF r,float density,float scaled,float opening){
        draw(c,r,density,scaled,opening,0);
    }
    void draw(Canvas c,RectF r,float density,float scaled,float opening,int slot){
        float pad=Math.min(10*density,r.width()*.06f),cx=r.centerX(),cy=r.top+r.height()*.49f;
        float radius=Math.min(r.width()*.25f,r.height()*.30f),corner=5*density;
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(0xff172735);c.drawRoundRect(r,corner,corner,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.7f*density);p.setColor(0xff786f58);c.drawRoundRect(r,corner,corner,p);
        c.save();c.clipRect(r);p.setColor(0x0dceba8b);p.setStrokeWidth(.3f*density);
        for(int k=0;k<9;k++){float y=r.top+r.height()*((k*.173f+slot*.071f)%1);float x=r.left+r.width()*((k*.381f+slot*.11f)%1);c.drawLine(x,y,Math.min(r.right,x+r.width()*.16f),y+.4f*density,p);}c.restore();
        c.save();c.clipRect(r);c.translate(cx,cy);c.scale(radius,radius);
        p.setStrokeWidth(.013f);p.setColor(GameDesign.alpha(0xffc9ad76,165+Math.round(opening*80)));c.drawCircle(0,0,1,p);c.drawCircle(0,0,.77f,p);
        // Four continuous cloud-knots and etched seal-script strokes, never counterfeit text.
        for(int side=0;side<4;side++){
            c.save();c.rotate(side*90);
            Path cloud=new Path();cloud.moveTo(-.30f,-.87f);cloud.cubicTo(-.46f,-1.13f,-.12f,-1.26f,0,-1.06f);cloud.cubicTo(.12f,-1.26f,.46f,-1.13f,.30f,-.87f);stroke(c,cloud,100,.016f);
            Path rune=new Path();rune.moveTo(-.18f,-.62f);rune.lineTo(.18f,-.62f);rune.moveTo(0,-.69f);rune.lineTo(0,-.46f);rune.lineTo(-.10f,-.40f);rune.moveTo(-.12f,-.53f);rune.lineTo(.12f,-.53f);stroke(c,rune,180,.018f);
            p.setStyle(Paint.Style.FILL);p.setColor(0xffc9ad76);c.drawCircle(.67f,-.67f,.024f,p);c.restore();
        }
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.012f);p.setColor(GameDesign.alpha(0xffc9ad76,50));
        Path square=new Path();square.moveTo(0,-1.28f);square.lineTo(1.28f,0);square.lineTo(0,1.28f);square.lineTo(-1.28f,0);square.close();c.drawPath(square,p);
        c.save();c.scale(.01f,.01f);p.setStyle(Paint.Style.FILL);p.setTypeface(GameDesign.EMPHASIS);p.setTextAlign(Paint.Align.CENTER);p.setColor(0xffd9be86);p.setTextSize(65);c.drawText("秘",0,-(p.ascent()+p.descent())/2,p);c.restore();c.restore();
        text(c,"四 象 秘 卷",cx,r.top+pad+10*scaled,10*scaled,0xffd5bd8c);
        text(c,opening>0?"封 印 将 启":"点击揭晓",cx,r.bottom-pad-8*scaled,10*scaled,0xffb3b7b5);
    }
    private void text(Canvas c,String s,float x,float y,float size,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTypeface(GameDesign.BODY);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);c.drawText(s,x,y-(p.ascent()+p.descent())/2,p);}
}
