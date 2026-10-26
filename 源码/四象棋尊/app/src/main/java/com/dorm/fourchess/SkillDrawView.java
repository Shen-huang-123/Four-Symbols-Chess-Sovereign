package com.dorm.fourchess;

import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.*;

/** Presentation only: accepts a fixed five-card hand, never generates or applies skills. */
final class SkillDrawView extends View {
    interface Listener {void onCard(int id,boolean visible);void onComplete();}
    private final int[] hand;
    private final long[] flips={-1,-1,-1,-1,-1};
    private final RectF[] cards={new RectF(),new RectF(),new RectF(),new RectF(),new RectF()};
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private Listener listener;private int selected=-1,down=-1,focused=-1;private boolean completed;
    SkillDrawView(Context c,int[] fixedHand){super(c);if(fixedHand.length!=5)throw new IllegalArgumentException("five cards required");hand=fixedHand.clone();setFocusable(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setContentDescription("五张随机技能牌，点击逐张揭晓");}
    void setListener(Listener value){listener=value;}
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    private float sp(float v){return v*getResources().getDisplayMetrics().scaledDensity;}
    private boolean motion(){return Build.VERSION.SDK_INT<26||android.animation.ValueAnimator.areAnimatorsEnabled();}
    @Override protected void onMeasure(int ws,int hs){setMeasuredDimension(resolveSize((int)dp(600),ws),resolveSize((int)dp(getResources().getConfiguration().orientation==1?440:170),hs));}
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){
        float gap=dp(8),pad=dp(4);boolean portrait=h>w*.70f;int cols=portrait?2:5,rows=portrait?3:1;
        float cw=(w-pad*2-gap*(cols-1))/cols,ch=(h-pad*2-gap*(rows-1))/rows;
        for(int i=0;i<5;i++){float x=pad+(i%cols)*(cw+gap),y=pad+(i/cols)*(ch+gap);if(portrait&&i==4)x=(w-cw)/2;cards[i].set(x,y,x+cw,y+ch);}
    }
    private float progress(int i){return flips[i]<0?0:!motion()?1:Math.max(0,Math.min(1,(SystemClock.uptimeMillis()-flips[i])/320f));}
    boolean allRevealed(){for(int i=0;i<5;i++)if(flips[i]<0||progress(i)<1)return false;return true;}
    void revealAll(boolean animated){long now=SystemClock.uptimeMillis();for(int i=0;i<5;i++)if(flips[i]<0||!animated)flips[i]=animated&&motion()?now+i*90:now-1000;selected=0;announceForAccessibility(animated?"正在揭晓五张技能":"已揭晓五张技能");finishIfReady();invalidate();}
    void reveal(int index){if(index<0||index>=5)return;selected=index;if(flips[index]<0){flips[index]=motion()?SystemClock.uptimeMillis():SystemClock.uptimeMillis()-1000;performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);}if(listener!=null)listener.onCard(hand[index],progress(index)>=.5f);finishIfReady();invalidate();}
    private void finishIfReady(){if(!completed&&allRevealed()){completed=true;if(listener!=null)listener.onComplete();announceForAccessibility("五张技能已揭晓，可以进入对局");}if(selected>=0&&listener!=null&&progress(selected)>=1)listener.onCard(hand[selected],true);}
    private void label(Canvas c,String text,float x,float y,float size,int color){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTypeface(GameDesign.EMPHASIS);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);c.drawText(text,x,y-(p.ascent()+p.descent())/2,p);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);boolean moving=false;
        for(int i=0;i<5;i++){
            RectF r=cards[i];float t=progress(i),age=flips[i]<0?9999:SystemClock.uptimeMillis()-flips[i];boolean face=flips[i]>=0&&t>=.5f;
            boolean active=flips[i]>=0&&age<820&&motion();moving|=active;
            int ink=face?SkillRules.tierColor(hand[i]):GameDesign.GOLD;
            if(active&&age>=160){float a=Math.max(0,1-(age-160)/660f);p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(r.centerX(),r.centerY(),Math.max(1,r.width()*.9f),GameDesign.alpha(ink,(int)(60*a)),GameDesign.alpha(ink,0),Shader.TileMode.CLAMP));c.drawRoundRect(r,dp(8),dp(8),p);p.setShader(null);}
            c.save();float sx=flips[i]<0?1:Math.max(.035f,Math.abs((float)Math.cos(t*Math.PI)));c.scale(sx,1,r.centerX(),r.centerY());float radius=Math.min(dp(8),r.height()*.08f);
            p.setStyle(Paint.Style.FILL);p.setColor(GameDesign.SHADOW);c.drawRoundRect(r.left,r.top+dp(2),r.right,r.bottom+dp(2),radius,radius,p);
            p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,face?BattleSkin.blend(GameDesign.PANEL_RAISED,ink,.13f):GameDesign.PANEL_RAISED,GameDesign.NIGHT,Shader.TileMode.CLAMP));c.drawRoundRect(r,radius,radius,p);p.setShader(null);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(selected==i?1.2f:.7f));p.setColor(face?ink:GameDesign.EDGE);c.drawRoundRect(r,radius,radius,p);
            RectF inner=new RectF(r);inner.inset(dp(4),dp(4));p.setStrokeWidth(dp(.45f));p.setColor(GameDesign.alpha(ink,70));c.drawRoundRect(inner,radius,radius,p);
            float cx=r.centerX(),cy=r.centerY();float seal=Math.min(r.width()*.22f,r.height()*.22f);p.setColor(GameDesign.alpha(ink,face?105:85));c.drawCircle(cx,cy,seal,p);Path diamond=new Path();diamond.moveTo(cx,cy-seal*1.2f);diamond.lineTo(cx+seal*1.2f,cy);diamond.lineTo(cx,cy+seal*1.2f);diamond.lineTo(cx-seal*1.2f,cy);diamond.close();c.drawPath(diamond,p);p.setStyle(Paint.Style.FILL);
            float small=Math.min(sp(11),Math.min(r.height()*.13f,r.width()*.16f));
            label(c,face?SkillRules.tier(hand[i]):"四 象 秘 卷",cx,r.top+r.height()*.14f,small,face?ink:GameDesign.SECONDARY);
            label(c,face?SkillRules.name(hand[i]):"秘",cx,cy,Math.min(sp(face?23:32),Math.min(r.width()*.29f,r.height()*.26f)),face?GameDesign.TEXT:GameDesign.GOLD);
            label(c,face?"一局一次":"点击揭晓",cx,r.bottom-r.height()*.15f,small,GameDesign.SECONDARY);c.restore();
            if(active&&face&&SkillRules.tier(hand[i]).equals("传说")){float f=Math.min(1,(age-160)/660f);p.setColor(GameDesign.alpha(ink,(int)(150*(1-f))));for(int k=0;k<8;k++){double angle=k*Math.PI/4;float rad=Math.min(r.width(),r.height())*(.25f+f*.3f),x=r.centerX()+(float)Math.cos(angle)*rad,y=r.centerY()+(float)Math.sin(angle)*rad;c.drawCircle(x,y,dp(1.1f),p);}}
        }
        finishIfReady();if(moving&&isShown()&&hasWindowFocus())postInvalidateOnAnimation();
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN){down=-1;for(int i=0;i<5;i++)if(cards[i].contains(e.getX(),e.getY()))down=i;return down>=0;}if(e.getActionMasked()==MotionEvent.ACTION_UP){if(down>=0&&cards[down].contains(e.getX(),e.getY())){performClick();reveal(down);}down=-1;return true;}if(e.getActionMasked()==MotionEvent.ACTION_CANCEL||e.getActionMasked()==MotionEvent.ACTION_POINTER_DOWN){down=-1;return true;}return true;}
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public boolean onKeyDown(int key,android.view.KeyEvent e){if(key==android.view.KeyEvent.KEYCODE_DPAD_RIGHT||key==android.view.KeyEvent.KEYCODE_DPAD_DOWN){selected=(selected+1)%5;invalidate();return true;}if(key==android.view.KeyEvent.KEYCODE_DPAD_LEFT||key==android.view.KeyEvent.KEYCODE_DPAD_UP){selected=(selected+4)%5;invalidate();return true;}if(key==android.view.KeyEvent.KEYCODE_ENTER||key==android.view.KeyEvent.KEYCODE_DPAD_CENTER){reveal(Math.max(0,selected));return true;}return super.onKeyDown(key,e);}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)invalidate();}
    @Override public AccessibilityNodeProvider getAccessibilityNodeProvider(){return new AccessibilityNodeProvider(){
        @Override public AccessibilityNodeInfo createAccessibilityNodeInfo(int id){if(id==View.NO_ID){AccessibilityNodeInfo n=AccessibilityNodeInfo.obtain(SkillDrawView.this);onInitializeAccessibilityNodeInfo(n);for(int i=1;i<=5;i++)n.addChild(SkillDrawView.this,i);return n;}if(id<1||id>5)return null;int i=id-1;boolean shown=flips[i]>=0&&progress(i)>=.5f;AccessibilityNodeInfo n=AccessibilityNodeInfo.obtain();n.setSource(SkillDrawView.this,id);n.setParent(SkillDrawView.this);n.setPackageName(getContext().getPackageName());n.setClassName("android.widget.Button");n.setContentDescription(shown?SkillRules.tier(hand[i])+"，"+SkillRules.name(hand[i])+"，"+SkillRules.DETAILS[hand[i]-1]:"第"+id+"张技能，点击揭晓");n.setEnabled(true);n.setClickable(true);n.setFocusable(true);n.setVisibleToUser(isShown());n.setAccessibilityFocused(focused==id);n.addAction(AccessibilityNodeInfo.ACTION_CLICK);n.addAction(focused==id?AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS:AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);Rect bounds=new Rect();cards[i].roundOut(bounds);n.setBoundsInParent(bounds);int[] location=new int[2];getLocationOnScreen(location);bounds.offset(location[0],location[1]);n.setBoundsInScreen(bounds);return n;}
        @Override public boolean performAction(int id,int action,Bundle args){if(id<1||id>5)return false;if(action==AccessibilityNodeInfo.ACTION_CLICK){reveal(id-1);sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);return true;}if(action==AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS||action==AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS){focused=action==AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS?id:-1;AccessibilityEvent event=AccessibilityEvent.obtain(action==AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS?AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);event.setSource(SkillDrawView.this,id);event.setPackageName(getContext().getPackageName());if(getParent()!=null)getParent().requestSendAccessibilityEvent(SkillDrawView.this,event);return true;}return false;}
    };}
}
