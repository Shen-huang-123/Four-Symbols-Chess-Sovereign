package com.dorm.fourchess;

import android.content.Context;
import android.view.ViewGroup;

/** Status and controls always fit; only the move history can scroll. */
final class MatchPanelLayout extends ViewGroup {
    private final int[] heights=new int[4];
    private int gap;
    MatchPanelLayout(Context context){super(context);}
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int w=MeasureSpec.getSize(widthSpec),h=MeasureSpec.getSize(heightSpec);
        int available=Math.max(1,h-getPaddingTop()-getPaddingBottom());
        int width=Math.max(1,w-getPaddingLeft()-getPaddingRight());
        gap=Math.max(2,Math.min(dp(4),available/80));
        heights[0]=Math.min(dp(24),Math.round(available*.09f));
        heights[1]=Math.min(dp(96),Math.round(available*.32f));
        heights[3]=Math.min(dp(156),Math.round(available*.42f));
        heights[2]=Math.max(1,available-heights[0]-heights[1]-heights[3]-gap*3);
        for(int i=0;i<4;i++)getChildAt(i).measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,heights[i]),MeasureSpec.EXACTLY));
        setMeasuredDimension(w,h);
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        int top=getPaddingTop();for(int i=0;i<4;i++){getChildAt(i).layout(getPaddingLeft(),top,r-l-getPaddingRight(),top+heights[i]);top+=heights[i]+gap;}
    }
    @Override protected LayoutParams generateDefaultLayoutParams(){return new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);}
}

