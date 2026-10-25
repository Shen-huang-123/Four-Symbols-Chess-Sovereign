package com.dorm.fourchess;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.ViewGroup;

/** Safe-window geometry with an uninterrupted phone battle stack and scene beneath it. */
final class DuelLayout extends ViewGroup {
    static final int PHONE_LANDSCAPE=0,PHONE_PORTRAIT=1,WIDE=2;
    private int layoutMode,hudHeight,hudTop,headerHeight,sidebarWidth,gap,boardLeft,boardTop,boardSide,controlsLeft,controlsTop,controlsWidth,controlsHeight;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    DuelLayout(Context context){super(context);setWillNotDraw(false);}
    int layoutMode(){return layoutMode;}
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void measureChildAt(int index,int width,int height){getChildAt(index).measure(MeasureSpec.makeMeasureSpec(Math.max(1,width),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,height),MeasureSpec.EXACTLY));}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int w=MeasureSpec.getSize(widthSpec),h=MeasureSpec.getSize(heightSpec);
        float density=getResources().getDisplayMetrics().density,wdp=w/density,hdp=h/density;
        layoutMode=wdp>=840&&hdp>=600?WIDE:w>h?PHONE_LANDSCAPE:PHONE_PORTRAIT;
        boolean wide=layoutMode==WIDE,portrait=layoutMode==PHONE_PORTRAIT;
        gap=dp(wide?8:4);headerHeight=0;hudTop=gap;
        ((FactionStripView)getChildAt(0)).setColumns(portrait?2:4);((FourChessView)getChildAt(1)).setCompactUi(!wide);
        getChildAt(2).setVisibility(wide?View.VISIBLE:View.GONE);getChildAt(3).setVisibility(wide?View.GONE:View.VISIBLE);
        hudHeight=dp(wide?54:portrait?92:hdp<300?28:34);hudHeight=Math.min(hudHeight,h/5);
        int top=hudHeight+gap*2,bottom=h-gap;
        if(wide){
            sidebarWidth=Math.min(dp(280),Math.max(dp(228),Math.round(w*.23f)));
            int availableW=w-sidebarWidth-gap*3,availableH=bottom-top;boardSide=Math.max(1,Math.min(availableW,availableH));
            boardLeft=gap+(availableW-boardSide)/2;boardTop=top+(availableH-boardSide)/2;
            controlsLeft=w-sidebarWidth-gap;controlsTop=top;controlsWidth=sidebarWidth;controlsHeight=availableH;measureChildAt(2,controlsWidth,controlsHeight);
        }else if(portrait){
            controlsHeight=dp(hdp<620?100:112);
            int spare=Math.max(0,h-hudHeight-controlsHeight-w-gap*5);
            headerHeight=Math.min(dp(58),spare/3);hudTop=gap+headerHeight;
            int availableH=bottom-hudTop-hudHeight-controlsHeight-gap*2;
            boardSide=Math.max(1,Math.min(w-gap*2,availableH));boardLeft=(w-boardSide)/2;boardTop=hudTop+hudHeight+gap;
            controlsTop=boardTop+boardSide+gap;controlsLeft=dp(10);controlsWidth=w-dp(20);
            ((CompactControlsLayout)getChildAt(3)).setPortrait(true);measureChildAt(3,controlsWidth,controlsHeight);
        }else{
            float fontScale=getResources().getConfiguration().fontScale;
            controlsWidth=dp(hdp<300?124:fontScale>1.2f?76:64);controlsWidth=Math.min(controlsWidth,w/(hdp<300?3:5));
            controlsHeight=bottom-top;controlsTop=top;boardSide=Math.max(1,Math.min(w-controlsWidth-gap*3,controlsHeight));
            int groupWidth=boardSide+gap+controlsWidth;boardLeft=(w-groupWidth)/2;boardTop=top+(controlsHeight-boardSide)/2;
            controlsLeft=boardLeft+boardSide+gap;((CompactControlsLayout)getChildAt(3)).setPortrait(false);measureChildAt(3,controlsWidth,controlsHeight);
        }
        measureChildAt(0,w-gap*2,hudHeight);measureChildAt(1,boardSide,boardSide);setMeasuredDimension(w,h);
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        getChildAt(0).layout(gap,hudTop,r-l-gap,hudTop+hudHeight);getChildAt(1).layout(boardLeft,boardTop,boardLeft+boardSide,boardTop+boardSide);
        getChildAt(layoutMode==WIDE?2:3).layout(controlsLeft,controlsTop,controlsLeft+controlsWidth,controlsTop+controlsHeight);
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float d=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight();
        if(layoutMode==PHONE_PORTRAIT){
            if(headerHeight>=dp(24)){p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextSize(Math.min(dp(24),headerHeight*.52f));p.setTextAlign(Paint.Align.CENTER);p.setColor(0xffead6ad);c.drawText("四 象 棋 尊",w/2,hudTop*.64f,p);}
            float foot=controlsTop+controlsHeight+dp(24);
            if(h-foot>dp(40)){p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("serif",Typeface.NORMAL));p.setTextSize(dp(12));p.setColor(0xffc2b49a);c.drawText("四方落子 · 棋会天下",w/2,foot,p);p.setStrokeWidth(d*.6f);p.setColor(0x667e8f97);c.drawLine(w*.18f,foot-dp(4),w*.30f,foot-dp(4),p);c.drawLine(w*.70f,foot-dp(4),w*.82f,foot-dp(4),p);}
        }
        // Sparse foreground petals only in scenery; never cross HUD, controls or board view.
        if(android.os.Build.VERSION.SDK_INT>=26&&!android.animation.ValueAnimator.areAnimatorsEnabled())return;
        float phase=(android.os.SystemClock.uptimeMillis()%18000)/18000f;c.save();
        c.clipRect(boardLeft,boardTop,boardLeft+boardSide,boardTop+boardSide,Region.Op.DIFFERENCE);
        c.clipRect(gap,hudTop,w-gap,hudTop+hudHeight,Region.Op.DIFFERENCE);
        c.clipRect(controlsLeft,controlsTop,controlsLeft+controlsWidth,controlsTop+controlsHeight,Region.Op.DIFFERENCE);
        for(int i=0;i<12;i++){float f=(phase+i*.083f)%1,x=w*((i*.271f+f*.11f)%1),y=h*f;p.setColor(i%3==0?0x778aa0ad:0x998f6570);c.save();c.rotate(f*160+i*27,x,y);c.drawOval(x-3*d,y-d,x+3*d,y+d,p);c.restore();}c.restore();
        if(isShown()&&hasWindowFocus())postInvalidateDelayed(90);
    }
    @Override protected LayoutParams generateDefaultLayoutParams(){return new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);}
}
