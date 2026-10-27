package com.dorm.fourchess;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Quiet scenery behind the manuscript window, independent of game scenery and rules. */
final class SecretScrollPanel extends Drawable {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    SecretScrollPanel(float density){this.density=density;}
    @Override public void draw(Canvas c){
        RectF r=new RectF(getBounds());p.setStyle(Paint.Style.FILL);p.setColor(0xff12232e);c.drawRoundRect(r,8*density,8*density,p);
        c.save();c.clipRect(r);p.setColor(0x0bafc5c5);
        for(int layer=0;layer<2;layer++){Path ridge=new Path();float base=r.bottom-r.height()*(.12f+layer*.13f);ridge.moveTo(r.left,base);for(int i=0;i<7;i++){float x=r.left+r.width()*i/6;ridge.lineTo(x,base-r.height()*(i%2==0?.05f:.13f));}ridge.lineTo(r.right,r.bottom);ridge.lineTo(r.left,r.bottom);ridge.close();c.drawPath(ridge,p);}c.restore();
        p.setStyle(Paint.Style.STROKE);p.setColor(0xff7a7865);p.setStrokeWidth(.7f*density);r.inset(.5f*density,.5f*density);c.drawRoundRect(r,8*density,8*density,p);
    }
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(ColorFilter filter){}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
