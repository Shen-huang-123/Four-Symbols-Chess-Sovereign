package com.dorm.fourchess;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.widget.Button;

/** Real accessible Button; independent icon and label painting, compact touch surface. */
final class ActionDockButton extends Button {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private android.animation.ValueAnimator pressAnimator;
    private float pressAmount;
    @Override protected void drawableStateChanged(){
        super.drawableStateChanged();if(pressAnimator!=null)pressAnimator.cancel();float target=isPressed()&&isEnabled()?1:0;
        if(!isAttachedToWindow()||(android.os.Build.VERSION.SDK_INT>=26&&!android.animation.ValueAnimator.areAnimatorsEnabled())){pressAmount=target;invalidate();return;}
        pressAnimator=android.animation.ValueAnimator.ofFloat(pressAmount,target);pressAnimator.setDuration(target>0?GameDesign.PRESS_MS:GameDesign.RELEASE_MS);pressAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());pressAnimator.addUpdateListener(a->{pressAmount=(Float)a.getAnimatedValue();invalidate();});pressAnimator.start();
    }
    @Override protected void onDetachedFromWindow(){if(pressAnimator!=null)pressAnimator.cancel();pressAmount=0;super.onDetachedFromWindow();}
    ActionDockButton(Context context){super(context);setBackground(null);}
    float labelSize(){float d=getResources().getDisplayMetrics().density,sd=getResources().getDisplayMetrics().scaledDensity;float size=Math.min(GameDesign.TYPE_LABEL*sd,Math.min(getWidth()*.28f,getHeight()*.23f));p.setTypeface(GameDesign.EMPHASIS);p.setTextSize(size);float width=p.measureText(getText().toString());return width>getWidth()*.94f?size*getWidth()*.94f/width:size;}
    @Override protected void onDraw(Canvas c){
        float w=getWidth(),h=getHeight(),d=getResources().getDisplayMetrics().density;
        int save=c.saveLayerAlpha(0,0,w,h,isEnabled()?255:145);c.scale(1-.045f*pressAmount,1-.045f*pressAmount,w/2,h/2);
        float text=labelSize(),radius=Math.min(w*.31f,(h-text-5*d)*.42f),cx=w/2,cy=(h-text-3*d)/2;
        boolean on=isSelected(),down=isPressed()||isFocused();
        p.setStyle(Paint.Style.FILL);p.setColor(down?GameDesign.PANEL_RAISED:on?GameDesign.alpha(GameDesign.INK_TEAL,220):GameDesign.PANEL_QUIET);c.drawCircle(cx,cy,radius,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d*GameDesign.STROKE);p.setColor(down?GameDesign.GOLD_BRIGHT:on?GameDesign.GOLD:GameDesign.EDGE);c.drawCircle(cx,cy,radius,p);
        p.setColor(0x44d9c396);c.drawArc(cx-radius+2*d,cy-radius+2*d,cx+radius-2*d,cy+radius-2*d,210,95,false,p);p.setStyle(Paint.Style.FILL);
        Drawable icon=getCompoundDrawables()[0];if(icon!=null){float size=Math.min(radius*1.12f,23*d);Rect bounds=icon.getBounds();c.save();c.translate(cx-size/2,cy-size/2);c.scale(size/Math.max(1,bounds.width()),size/Math.max(1,bounds.height()));icon.draw(c);c.restore();}
        p.setTypeface(GameDesign.EMPHASIS);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(text);p.setColor(isEnabled()?GameDesign.TEXT:GameDesign.DISABLED);
        c.drawText(getText().toString(),cx,h-2*d-p.descent(),p);
        if(down){p.setColor(0x18ffdf9c);c.drawCircle(cx,cy,radius*1.15f,p);}c.restoreToCount(save);
    }
}
