package com.dorm.fourchess;

import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.*;

/** Presentation only: accepts a fixed four-card hand, never generates or applies skills. */
final class SkillDrawView extends View {
    interface Listener {void onCard(int id,boolean visible);void onComplete();}
    private final int[] hand,charges;private final SkillCardPainter cardPainter;
    private final long[] flips={-1,-1,-1,-1};
    private final SkillCardEffects effects=new SkillCardEffects();
    private final SecretScrollBackPainter backPainter=new SecretScrollBackPainter();
    static final int REVEAL_MS=720;
    private long selectedAt=-1;
    private final RectF[] cards={new RectF(),new RectF(),new RectF(),new RectF()};
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private Listener listener;private int selected=-1,down=-1,focused=-1;private boolean completed;
    SkillDrawView(Context c,int[] fixedHand,int[] remaining){super(c);if(fixedHand.length!=4||remaining.length!=4)throw new IllegalArgumentException("four cards required");hand=fixedHand.clone();charges=remaining.clone();for(int i=0;i<4;i++)if(hand[i]<1||hand[i]>SkillRules.COUNT||charges[i]<0||charges[i]>3)throw new IllegalArgumentException("invalid skill card");cardPainter=new SkillCardPainter(c);setFocusable(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setContentDescription("四张随机技能牌，点击逐张揭晓");}
    void setListener(Listener value){listener=value;}
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    private float sp(float v){return v*getResources().getDisplayMetrics().scaledDensity;}
    private boolean motion(){return Build.VERSION.SDK_INT<26||android.animation.ValueAnimator.areAnimatorsEnabled();}
    @Override protected void onMeasure(int ws,int hs){int width=resolveSize((int)dp(600),ws);float cardWidth=Math.max(dp(44),(width-dp(16))/2);int required=(int)Math.ceil(cardPainter.minimumCardHeight(cardWidth)*2+dp(16));setMeasuredDimension(width,resolveSize(required,hs));}
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){
        float gap=dp(8),pad=dp(4);int cols=2,rows=2;
        float cw=(w-pad*2-gap*(cols-1))/cols,ch=(h-pad*2-gap*(rows-1))/rows;
        for(int i=0;i<4;i++){float x=pad+(i%cols)*(cw+gap),y=pad+(i/cols)*(ch+gap);cards[i].set(x,y,x+cw,y+ch);}
    }
    private float progress(int i){return flips[i]<0?0:!motion()?1:Math.max(0,Math.min(1,(SystemClock.uptimeMillis()-flips[i])/(float)REVEAL_MS));}
    boolean allRevealed(){for(int i=0;i<4;i++)if(flips[i]<0||progress(i)<1)return false;return true;}
    void revealAll(boolean animated){long now=SystemClock.uptimeMillis();for(int i=0;i<4;i++)if(flips[i]<0||!animated)flips[i]=animated&&motion()?now+i*90:now-10000;selected=0;selectedAt=-1;announceForAccessibility(animated?"正在揭晓四张技能":"已揭晓四张技能");finishIfReady();invalidate();}
    void reveal(int index){if(index<0||index>=4)return;selected=index;boolean hidden=flips[index]<0;selectedAt=!hidden&&motion()?SystemClock.uptimeMillis():-1;if(hidden){flips[index]=motion()?SystemClock.uptimeMillis():SystemClock.uptimeMillis()-10000;performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);}if(listener!=null)listener.onCard(hand[index],progress(index)>=.5f);finishIfReady();invalidate();}
    private void finishIfReady(){if(!completed&&allRevealed()){completed=true;if(listener!=null)listener.onComplete();announceForAccessibility("四张技能已揭晓，可以进入对局");}if(selected>=0&&listener!=null&&progress(selected)>=1)listener.onCard(hand[selected],true);}
    private void label(Canvas c,String text,float x,float y,float size,int color){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTypeface(GameDesign.EMPHASIS);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);c.drawText(text,x,y-(p.ascent()+p.descent())/2,p);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);boolean moving=false;
        for(int i=0;i<4;i++){
            RectF r=cards[i];float t=progress(i);long now=SystemClock.uptimeMillis(),age=flips[i]<0?-10000:now-flips[i],revealAge=age-360,selectAge=selectedAt>=0&&selected==i?now-selectedAt:-1;boolean face=flips[i]>=0&&t>=.5f;
            boolean revealFx=motion()&&effects.active(hand[i],revealAge,false),selectFx=motion()&&effects.active(hand[i],selectAge,true);
            moving|=motion()&&flips[i]>=0&&(t<1||revealFx||selectFx);
            c.save();float turn=Math.max(0,Math.min(1,(t-.15f)/.7f));float sx=flips[i]<0?1:Math.max(.035f,Math.abs((float)Math.cos(turn*Math.PI)));float lift=flips[i]<0?0:(float)Math.sin(Math.PI*t)*dp(3);c.translate(0,-lift);c.scale(sx,1,r.centerX(),r.centerY());
            if(face){cardPainter.draw(c,r,hand[i],charges[i],selected==i,charges[i]==0,charges[i]==0?"已用尽":"",false,0);
                if(charges[i]>0&&(selectFx||revealFx))effects.draw(c,cardPainter.emblemBounds(r),hand[i],selectFx?selectAge:revealAge,selectFx);
            }
            else{
                backPainter.draw(c,r,getResources().getDisplayMetrics().density,getResources().getDisplayMetrics().scaledDensity,flips[i]>=0?(float)Math.sin(Math.PI*Math.min(1,t/.5f)):0);
            }c.restore();
        }
        finishIfReady();if(moving&&isShown()&&hasWindowFocus())postInvalidateOnAnimation();
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN){down=-1;for(int i=0;i<4;i++)if(cards[i].contains(e.getX(),e.getY()))down=i;return down>=0;}if(e.getActionMasked()==MotionEvent.ACTION_UP){if(down>=0&&cards[down].contains(e.getX(),e.getY())){performClick();reveal(down);}down=-1;return true;}if(e.getActionMasked()==MotionEvent.ACTION_CANCEL||e.getActionMasked()==MotionEvent.ACTION_POINTER_DOWN){down=-1;return true;}return true;}
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public boolean onKeyDown(int key,android.view.KeyEvent e){if(key==android.view.KeyEvent.KEYCODE_DPAD_RIGHT||key==android.view.KeyEvent.KEYCODE_DPAD_DOWN){selected=(selected+1)%4;invalidate();return true;}if(key==android.view.KeyEvent.KEYCODE_DPAD_LEFT||key==android.view.KeyEvent.KEYCODE_DPAD_UP){selected=(selected+3)%4;invalidate();return true;}if(key==android.view.KeyEvent.KEYCODE_ENTER||key==android.view.KeyEvent.KEYCODE_DPAD_CENTER){reveal(Math.max(0,selected));return true;}return super.onKeyDown(key,e);}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)invalidate();}
    @Override public AccessibilityNodeProvider getAccessibilityNodeProvider(){return new AccessibilityNodeProvider(){
        @Override public AccessibilityNodeInfo createAccessibilityNodeInfo(int id){if(id==View.NO_ID){AccessibilityNodeInfo n=AccessibilityNodeInfo.obtain(SkillDrawView.this);onInitializeAccessibilityNodeInfo(n);for(int i=1;i<=4;i++)n.addChild(SkillDrawView.this,i);return n;}if(id<1||id>4)return null;int i=id-1;boolean shown=flips[i]>=0&&progress(i)>=.5f;AccessibilityNodeInfo n=AccessibilityNodeInfo.obtain();n.setSource(SkillDrawView.this,id);n.setParent(SkillDrawView.this);n.setPackageName(getContext().getPackageName());n.setClassName("android.widget.Button");n.setContentDescription(shown?SkillCardTheme.label(hand[i])+"，"+SkillRules.name(hand[i])+"，剩余"+charges[i]+"次，"+SkillRules.DETAILS[hand[i]-1]:"第"+id+"张技能，点击揭晓");n.setEnabled(true);n.setClickable(true);n.setFocusable(true);n.setVisibleToUser(isShown());n.setAccessibilityFocused(focused==id);n.addAction(AccessibilityNodeInfo.ACTION_CLICK);n.addAction(focused==id?AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS:AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);Rect bounds=new Rect();cards[i].roundOut(bounds);n.setBoundsInParent(bounds);int[] location=new int[2];getLocationOnScreen(location);bounds.offset(location[0],location[1]);n.setBoundsInScreen(bounds);return n;}
        @Override public boolean performAction(int id,int action,Bundle args){if(id<1||id>4)return false;if(action==AccessibilityNodeInfo.ACTION_CLICK){reveal(id-1);sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);return true;}if(action==AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS||action==AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS){focused=action==AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS?id:-1;AccessibilityEvent event=AccessibilityEvent.obtain(action==AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS?AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);event.setSource(SkillDrawView.this,id);event.setPackageName(getContext().getPackageName());if(getParent()!=null)getParent().requestSendAccessibilityEvent(SkillDrawView.this,event);return true;}return false;}
    };}
}
