package com.dorm.fourchess;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.widget.Button;

/** Real accessible Button; independent icon and label painting, compact touch surface. */
final class ActionDockButton extends Button {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    ActionDockButton(Context context){super(context);setBackground(null);}
    float labelSize(){float d=getResources().getDisplayMetrics().density,sd=getResources().getDisplayMetrics().scaledDensity;return Math.min(12*sd,Math.min(getWidth()*.28f,getHeight()*.23f));}
    @Override protected void onDraw(Canvas c){
        float w=getWidth(),h=getHeight(),d=getResources().getDisplayMetrics().density;
        float text=labelSize(),radius=Math.min(w*.31f,(h-text-5*d)*.42f),cx=w/2,cy=(h-text-3*d)/2;
        boolean on=isSelected(),down=isPressed();
        p.setStyle(Paint.Style.FILL);p.setColor(down?0xd23e4d5b:on?0xb9324a53:0x881b2c39);c.drawCircle(cx,cy,radius,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d*.7f);p.setColor(down?0xfff0d9a0:on?0xffb8a271:0xff817867);c.drawCircle(cx,cy,radius,p);
        p.setColor(0x44d9c396);c.drawArc(cx-radius+2*d,cy-radius+2*d,cx+radius-2*d,cy+radius-2*d,210,95,false,p);p.setStyle(Paint.Style.FILL);
        Drawable icon=getCompoundDrawables()[0];if(icon!=null){float size=Math.min(radius*1.12f,23*d);Rect bounds=icon.getBounds();c.save();c.translate(cx-size/2,cy-size/2);c.scale(size/Math.max(1,bounds.width()),size/Math.max(1,bounds.height()));icon.draw(c);c.restore();}
        p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(text);p.setColor(isEnabled()?0xffeadcc0:0xff9a9589);
        c.drawText(getText().toString(),cx,h-2*d-p.descent(),p);
        if(down){p.setColor(0x18ffdf9c);c.drawCircle(cx,cy,radius*1.15f,p);}
    }
}
