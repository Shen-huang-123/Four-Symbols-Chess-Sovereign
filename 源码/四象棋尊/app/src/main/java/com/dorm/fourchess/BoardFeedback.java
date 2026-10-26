package com.dorm.fourchess;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/** Pure drawing: receives real grid centres and never changes selection or move legality. */
final class BoardFeedback {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path diamond=new Path();
    void recent(Canvas c,float sx,float sy,float tx,float ty,float spacing,float pieceRadius){
        float r=Math.min(spacing*.465f,pieceRadius+spacing*.045f);
        p.setStyle(Paint.Style.FILL);p.setColor(0x26c8a255);c.drawCircle(tx,ty,r,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,spacing*.025f));p.setColor(0xaaa58445);c.drawCircle(tx,ty,r,p);
        float a=spacing*.14f;diamond.reset();diamond.moveTo(sx,sy-a);diamond.lineTo(sx+a,sy);diamond.lineTo(sx,sy+a);diamond.lineTo(sx-a,sy);diamond.close();p.setColor(0xffa48137);c.drawPath(diamond,p);
    }
    void arrival(Canvas c,float x,float y,float spacing,float progress){
        if(progress<0||progress>=1)return;
        float fade=1-progress,r=spacing*(.46f+.24f*progress);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,spacing*.025f));p.setColor(((int)(135*fade)<<24)|0xd6ae58);
        c.drawCircle(x,y,r,p);
        // Short glints stay outside the piece face and disappear within 400 ms.
        for(int i=0;i<4;i++){double angle=Math.PI*.5*i+Math.PI*.25;float cx=x+(float)Math.cos(angle)*r,cy=y+(float)Math.sin(angle)*r,a=spacing*.065f*fade;c.drawLine(cx-a,cy,cx+a,cy,p);c.drawLine(cx,cy-a,cx,cy+a,p);}
    }
    void destination(Canvas c,float x,float y,float spacing){
        p.setStyle(Paint.Style.FILL);p.setColor(0x202b704e);c.drawCircle(x,y,spacing*.19f,p);
        p.setColor(0xfff7efdc);c.drawCircle(x,y,spacing*.135f,p);p.setColor(0xff306b4a);c.drawCircle(x,y,spacing*.105f,p);
    }
    void capture(Canvas c,float x,float y,float spacing,float pieceRadius){
        float r=Math.min(spacing*.465f,pieceRadius+spacing*.045f);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,spacing*.035f));p.setColor(0xffa94735);
        for(int angle=25;angle<360;angle+=90)c.drawArc(x-r,y-r,x+r,y+r,angle,40,false,p);
    }
    void skillTarget(Canvas c,float x,float y,float spacing){
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,spacing*.035f));p.setColor(0xc76c9cc0);float r=spacing*.44f;
        for(int a=0;a<360;a+=90)c.drawArc(x-r,y-r,x+r,y+r,a+12,28,false,p);
    }
    void seal(Canvas c,float x,float y,float spacing){
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,spacing*.035f));p.setColor(0xff347cb0);float r=spacing*.43f;
        diamond.reset();for(int i=0;i<6;i++){float px=x+(float)Math.cos(i*Math.PI/3)*r,py=y+(float)Math.sin(i*Math.PI/3)*r;if(i==0)diamond.moveTo(px,py);else diamond.lineTo(px,py);}diamond.close();c.drawPath(diamond,p);
    }
    void selection(Canvas c,float x,float y,float spacing,float pieceRadius){
        float r=Math.min(spacing*.465f,pieceRadius+spacing*.055f);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,spacing*.04f));p.setColor(0xffb58a32);c.drawCircle(x,y,r,p);
        p.setStrokeWidth(Math.max(1,spacing*.018f));p.setColor(0xfff8e5ad);c.drawCircle(x,y,r-spacing*.025f,p);
    }
}
