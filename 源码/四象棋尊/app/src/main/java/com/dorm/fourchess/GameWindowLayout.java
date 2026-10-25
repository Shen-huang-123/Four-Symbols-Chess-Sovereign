package com.dorm.fourchess;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;
import android.widget.FrameLayout;

/** Full-window scenery; child controls continue to respect all existing safe insets. */
final class GameWindowLayout extends FrameLayout {
    private final Paint wash=new Paint();
    GameWindowLayout(Context context){super(context);setBackground(new CourtyardScene());setWillNotDraw(false);}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        if(h>0)wash.setShader(new LinearGradient(0,0,0,h,new int[]{0x6b0c1c2a,0x1f102437,0x98101d2b},new float[]{0,.43f,1},Shader.TileMode.CLAMP));
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(getChildCount()>1&&getChildAt(1).getVisibility()==View.VISIBLE)canvas.drawRect(0,0,getWidth(),getHeight(),wash);
    }
}