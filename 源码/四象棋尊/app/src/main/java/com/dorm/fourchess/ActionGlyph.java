package com.dorm.fourchess;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/** Small vector-like glyphs: independent of the device's emoji/font coverage. */
final class ActionGlyph extends Drawable {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int type;
    private boolean muted;
    void setMuted(boolean value){muted=value;invalidateSelf();}
    ActionGlyph(int type){this.type=type;}
    @Override public void draw(Canvas canvas){
        canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/24f,getBounds().height()/24f);
        p.setColor(0xffdbc79f);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.6f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        if(type==0){for(int i=0;i<3;i++){float y=6+i*6;canvas.drawLine(3,y,21,y,p);p.setStyle(Paint.Style.FILL);canvas.drawCircle(i==1?9:15,y,2.3f,p);p.setStyle(Paint.Style.STROKE);}}
        else if(type==1){canvas.drawRoundRect(3,3,21,21,2,2,p);canvas.drawLine(8,3,8,21,p);canvas.drawLine(12,8,17,8,p);canvas.drawLine(12,12,17,12,p);canvas.drawLine(12,16,15,16,p);}
        else if(type==2){Path arrow=new Path();arrow.moveTo(8,3);arrow.lineTo(3,8);arrow.lineTo(8,13);canvas.drawPath(arrow,p);Path curve=new Path();curve.moveTo(3,8);curve.lineTo(13,8);curve.cubicTo(23,8,23,20,12,20);canvas.drawPath(curve,p);}
        else if(type==4){canvas.drawRoundRect(4,3,20,21,2,2,p);for(int i=0;i<3;i++){float y=7+i*5;canvas.drawCircle(8,y,.5f,p);canvas.drawLine(11,y,17,y,p);}}
        else if(type==5){canvas.drawLine(10,3,4,3,p);canvas.drawLine(4,3,4,21,p);canvas.drawLine(4,21,10,21,p);canvas.drawLine(10,12,22,12,p);canvas.drawLine(17,7,22,12,p);canvas.drawLine(22,12,17,17,p);}
        else if(type==6){Path star=new Path();star.moveTo(12,2);star.lineTo(15,9);star.lineTo(22,12);star.lineTo(15,15);star.lineTo(12,22);star.lineTo(9,15);star.lineTo(2,12);star.lineTo(9,9);star.close();canvas.drawPath(star,p);canvas.drawLine(19,2,19,6,p);canvas.drawLine(17,4,21,4,p);}
        else{Path speaker=new Path();speaker.moveTo(3,9);speaker.lineTo(7,9);speaker.lineTo(12,5);speaker.lineTo(12,19);speaker.lineTo(7,15);speaker.lineTo(3,15);speaker.close();canvas.drawPath(speaker,p);if(muted){canvas.drawLine(16,9,22,15,p);canvas.drawLine(22,9,16,15,p);}else{canvas.drawArc(11,7,19,17,-65,130,false,p);canvas.drawArc(10,3,24,21,-60,120,false,p);}}
        canvas.restore();
    }
    @Override public void setAlpha(int alpha){p.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}

