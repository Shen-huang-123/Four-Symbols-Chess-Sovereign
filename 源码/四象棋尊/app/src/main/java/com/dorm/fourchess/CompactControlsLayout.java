package com.dorm.fourchess;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/** Compact six-entry dock; the state ribbon and hint retain their original positions. */
final class CompactControlsLayout extends ViewGroup {
    private boolean portrait,twoColumns;
    private int gap,stateHeight,hintHeight,actionHeight,actionsTop;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    CompactControlsLayout(Context context){super(context);setWillNotDraw(false);}
    void setPortrait(boolean value){
        if(portrait!=value){portrait=value;((TextView)getChildAt(0)).setSingleLine(value);((TextView)getChildAt(0)).setHorizontallyScrolling(false);((TextView)getChildAt(0)).setMaxLines(value?1:2);}
        getChildAt(getChildCount()-1).setVisibility(value?View.VISIBLE:View.GONE);
    }
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int w=MeasureSpec.getSize(widthSpec),h=MeasureSpec.getSize(heightSpec),count=getChildCount()-2;gap=dp(4);twoColumns=!portrait&&w>=dp(100);
        int rows=twoColumns?(count+1)/2:count;
        hintHeight=portrait?dp(15):0;stateHeight=portrait?dp(26):Math.min(dp(44),Math.max(dp(30),h/7));
        actionHeight=portrait?Math.max(1,h-stateHeight-hintHeight-gap*2):Math.min(dp(66),Math.max(1,(h-stateHeight-gap*rows)/rows));
        actionsTop=stateHeight+gap;measureAt(0,w,stateHeight);
        for(int i=1;i<=count;i++)measureAt(i,portrait?Math.max(1,(w-gap*(count-1))/count):twoColumns?(w-gap)/2:w,actionHeight);
        if(portrait)measureAt(count+1,w,hintHeight);setMeasuredDimension(w,h);
    }
    private void measureAt(int index,int w,int h){getChildAt(index).measure(MeasureSpec.makeMeasureSpec(Math.max(1,w),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,h),MeasureSpec.EXACTLY));}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        int w=r-l,count=getChildCount()-2;getChildAt(0).layout(0,0,w,stateHeight);
        for(int i=1;i<=count;i++){
            int left=portrait?(i-1)*(w+gap)/count:twoColumns?(i-1)%2*(w+gap)/2:0,right=portrait?i*(w+gap)/count-gap:twoColumns?left+(w-gap)/2:w;
            int top=actionsTop+(portrait?0:(twoColumns?(i-1)/2:i-1)*(actionHeight+gap));getChildAt(i).layout(left,top,right,top+actionHeight);
        }
        if(portrait)getChildAt(count+1).layout(0,b-t-hintHeight,w,b-t);
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float d=getResources().getDisplayMetrics().density,w=getWidth();
        p.setStyle(Paint.Style.FILL);p.setColor(GameDesign.PANEL);c.drawRoundRect(d,0,w-d,stateHeight,stateHeight*.5f,stateHeight*.5f,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d*GameDesign.STROKE);p.setColor(GameDesign.EDGE);c.drawRoundRect(d,0,w-d,stateHeight,stateHeight*.5f,stateHeight*.5f,p);
        p.setStyle(Paint.Style.FILL);p.setColor(GameDesign.PANEL_QUIET);RectF tray=new RectF(d,actionsTop-d,w-d,getHeight()-hintHeight);c.drawRoundRect(tray,dp(GameDesign.RADIUS_PANEL),dp(GameDesign.RADIUS_PANEL),p);
        p.setColor(GameDesign.EDGE_QUIET);p.setStyle(Paint.Style.STROKE);c.drawRoundRect(tray,dp(GameDesign.RADIUS_PANEL),dp(GameDesign.RADIUS_PANEL),p);p.setStyle(Paint.Style.FILL);
    }
    @Override protected LayoutParams generateDefaultLayoutParams(){return new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);}
}