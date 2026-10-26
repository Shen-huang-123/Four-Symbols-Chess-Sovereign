package com.dorm.fourchess;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
import java.util.ArrayDeque;
import java.util.Iterator;

/** A non-interactive, HUD-only treaty announcement. No game state or timer is delayed. */
final class AllianceAnnouncementView extends View {
    static final long DISPLAY_MS=4000,ENTER_MS=320,EXIT_MS=280;
    private static final class Event {final int a,b,pair;final String first,second;boolean announced;Event(int a,int b,String first,String second){this.a=a;this.b=b;this.first=first;this.second=second;pair=DiplomacyState.pair(a,b);}}
    private final ArrayDeque<Event> pending=new ArrayDeque<>();
    private final android.text.TextPaint paint=new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final CharacterPortraits portraits;
    private Event active;private int liveMask;private long started;
    private final Runnable fade=()->invalidate();
    private final Runnable expire=this::finish;
    AllianceAnnouncementView(Context context){super(context);portraits=new CharacterPortraits(context);setClickable(false);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setVisibility(INVISIBLE);}
    private boolean motion(){return android.os.Build.VERSION.SDK_INT<26||android.animation.ValueAnimator.areAnimatorsEnabled();}
    int activePair(){return active==null?0:active.pair;}
    int pendingCount(){return pending.size();}
    void reconcile(int mask){liveMask=mask;for(Iterator<Event> it=pending.iterator();it.hasNext();)if((it.next().pair&mask)==0)it.remove();if(active!=null&&(active.pair&mask)==0)finish();else resumeIfReady();}
    void enqueue(int a,int b,String first,String second){
        int pair=DiplomacyState.pair(a,b);if(pair==0||(liveMask&pair)==0||activePair()==pair)return;for(Event e:pending)if(e.pair==pair)return;
        pending.addLast(new Event(a,b,first,second));resumeIfReady();
    }
    void resumeIfReady(){
        if(active!=null||!hasWindowFocus()||getWindowVisibility()!=VISIBLE||!(getParent() instanceof View)||!((View)getParent()).isShown())return;
        while(!pending.isEmpty()){Event next=pending.removeFirst();if((liveMask&next.pair)==0)continue;active=next;started=SystemClock.uptimeMillis();setContentDescription(next.first+"与"+next.second+"已结盟，"+AllianceMarkPainter.label(next.pair));setVisibility(VISIBLE);invalidate();removeCallbacks(fade);removeCallbacks(expire);postDelayed(fade,DISPLAY_MS-EXIT_MS);postDelayed(expire,DISPLAY_MS);if(!next.announced){next.announced=true;announceForAccessibility(getContentDescription());}break;}
    }
    private void finish(){removeCallbacks(fade);removeCallbacks(expire);active=null;setVisibility(INVISIBLE);setContentDescription(null);resumeIfReady();}
    void clear(){removeCallbacks(fade);removeCallbacks(expire);pending.clear();active=null;liveMask=0;setVisibility(INVISIBLE);setContentDescription(null);}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(!focused&&active!=null){pending.addFirst(active);active=null;removeCallbacks(fade);removeCallbacks(expire);setVisibility(INVISIBLE);}else if(focused)resumeIfReady();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility!=VISIBLE)clear();}
    @Override protected void onDetachedFromWindow(){clear();super.onDetachedFromWindow();}
    private void fit(String text,float width,float size){paint.setTextSize(size);float measured=paint.measureText(text);if(measured>width)paint.setTextSize(size*width/Math.max(1,measured));}
    @Override protected void onDraw(Canvas canvas){
        if(active==null)return;float w=getWidth(),h=getHeight(),d=getResources().getDisplayMetrics().density,sd=getResources().getDisplayMetrics().scaledDensity;long age=SystemClock.uptimeMillis()-started;
        boolean animate=motion();float enter=animate?Math.min(1,age/(float)ENTER_MS):1,exit=animate?Math.min(1,(DISPLAY_MS-age)/(float)EXIT_MS):1;enter=1-(1-enter)*(1-enter);float alpha=Math.max(0,Math.min(enter,exit));int save=canvas.saveLayerAlpha(0,0,w,h,Math.round(255*alpha));canvas.translate(0,-(1-enter)*Math.min(8*d,h*.15f));
        RectF panel=new RectF(d,d,w-d,h-d);paint.setStyle(Paint.Style.FILL);paint.setColor(0xff19313f);canvas.drawRoundRect(panel,Math.min(12*d,h*.25f),Math.min(12*d,h*.25f),paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);paint.setColor(GameDesign.GOLD);canvas.drawRoundRect(panel,Math.min(12*d,h*.25f),Math.min(12*d,h*.25f),paint);paint.setStyle(Paint.Style.FILL);
        float r=h*.32f,pad=Math.min(16*d,h*.18f),left=pad+r-(1-enter)*7*d,right=w-pad-r+(1-enter)*7*d,cy=h*.5f;
        paint.setStrokeWidth(Math.max(d*.7f,h*.012f));paint.setStyle(Paint.Style.STROKE);paint.setColor(0x99cfb47d);canvas.drawLine(left+r+3*d,cy,left+r+3*d+(w*.5f-left-r-3*d)*enter,cy,paint);canvas.drawLine(right-r-3*d,cy,right-r-3*d-(right-r-3*d-w*.5f)*enter,cy,paint);paint.setStyle(Paint.Style.FILL);
        for(int i=0;i<2;i++){float x=i==0?left:right;int owner=i==0?active.a:active.b;RectF face=new RectF(x-r,cy-r,x+r,cy+r);portraits.draw(canvas,owner,face,255,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);paint.setColor(GameDesign.factionLights()[owner]);canvas.drawCircle(x,cy,r,paint);paint.setStyle(Paint.Style.FILL);}
        float centerWidth=Math.max(1,w-2*(pad+r*2+10*d));RectF textPanel=new RectF((w-centerWidth)/2,2*d,(w+centerWidth)/2,h-2*d);paint.setColor(0xff19313f);canvas.drawRoundRect(textPanel,4*d,4*d,paint);
        paint.setTypeface(GameDesign.EMPHASIS);paint.setTextAlign(Paint.Align.CENTER);paint.setColor(GameDesign.GOLD_BRIGHT);fit("已结盟",centerWidth*.68f,Math.min(18*sd,h*.31f));canvas.drawText("已结盟",w*.5f,h*.42f,paint);
        paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(Math.min(12*sd,h*.24f));String first=android.text.TextUtils.ellipsize(active.first,paint,centerWidth*.43f,android.text.TextUtils.TruncateAt.END).toString(),second=android.text.TextUtils.ellipsize(active.second,paint,centerWidth*.43f,android.text.TextUtils.TruncateAt.END).toString();String names=first+" · "+second;fit(names,centerWidth,paint.getTextSize());paint.setColor(GameDesign.TEXT);canvas.drawText(names,w*.5f,h*.77f,paint);
        AllianceMarkPainter.draw(canvas,left+r*.70f,cy+r*.70f,Math.min(8*d,h*.16f),active.pair,paint);AllianceMarkPainter.draw(canvas,right-r*.70f,cy+r*.70f,Math.min(8*d,h*.16f),active.pair,paint);
        canvas.restoreToCount(save);if(animate&&(age<ENTER_MS||age>DISPLAY_MS-EXIT_MS))postInvalidateOnAnimation();
    }
}
