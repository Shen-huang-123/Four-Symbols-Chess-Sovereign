package com.dorm.fourchess;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.*;
import java.lang.reflect.*;
import java.io.*;

/** Mode-page-only native geometry, touch, navigation and existing entry-flow acceptance. */
public final class ModeUiProbe extends Instrumentation {
    private Bundle args;private MainActivity activity;private ModeSelectView page;
    private final StringBuilder report=new StringBuilder();
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);args=arguments;start();}
    private Object field(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    private void invoke(Object target,String name)throws Exception{Method method=target.getClass().getDeclaredMethod(name);method.setAccessible(true);method.invoke(target);}
    private void check(boolean value,String name){if(!value)throw new AssertionError(name);report.append("PASS ").append(name).append('\n');android.util.Log.i("ModeUiProbe","PASS "+name);}
    private void main(Runnable action){java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();runOnMainSync(()->{try{action.run();}catch(Throwable error){failure.set(error);}});waitForIdleSync();if(failure.get()!=null)throw new AssertionError(failure.get());}
    private void event(int type,float x,float y){long now=SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(now,now,type,x,y,0);page.dispatchTouchEvent(event);event.recycle();}
    private RectF rect(String name,int index){try{return new RectF(index<0?(RectF)field(page,name):((RectF[])field(page,name))[index]);}catch(Exception error){throw new RuntimeException(error);}}
    private void tap(int index){RectF r=rect("cards",index);event(MotionEvent.ACTION_DOWN,r.centerX(),r.centerY());event(MotionEvent.ACTION_UP,r.centerX(),r.centerY());}
    private void settle(){SystemClock.sleep(310);waitForIdleSync();}
    private void rotate(boolean portrait){main(()->activity.setRequestedOrientation(portrait?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));for(int i=0;i<60;i++){waitForIdleSync();View decor=activity.getWindow().getDecorView();if((decor.getHeight()>=decor.getWidth())==portrait){SystemClock.sleep(180);waitForIdleSync();return;}SystemClock.sleep(80);}throw new AssertionError("rotation failed");}
    private void screenshot(String name)throws Exception{
        java.util.concurrent.CountDownLatch committed=new java.util.concurrent.CountDownLatch(1);runOnMainSync(()->{View decor=activity.getWindow().getDecorView();decor.getViewTreeObserver().registerFrameCommitCallback(committed::countDown);decor.invalidate();});if(!committed.await(3,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("frame did not commit");SystemClock.sleep(140);Bitmap bitmap=getUiAutomation().takeScreenshot();if(bitmap==null)throw new AssertionError("no screenshot");try(FileOutputStream stream=new FileOutputStream(new File(getTargetContext().getFilesDir(),name))){bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);}finally{bitmap.recycle();}
    }
    private void drawAndCheck(){
        try{
            Bitmap bitmap=Bitmap.createBitmap(page.getWidth(),page.getHeight(),Bitmap.Config.ARGB_8888);page.draw(new Canvas(bitmap));bitmap.recycle();
            RectF viewport=new RectF(0,0,page.getWidth(),page.getHeight());float d=page.getResources().getDisplayMetrics().density;
            RectF previous=null;float height=(Float)field(page,"cardHeight"),baseline=(Float)field(page,"referenceCardHeight");
            for(int i=0;i<3;i++){
                RectF card=rect("cards",i),title=rect("titleBounds",i),subtitle=rect("subtitleBounds",i);
                check(viewport.contains(card)&&card.height()+.5f>=44*d,"complete card and 44dp touch height: "+i);
                check(previous==null||previous.bottom<=card.top,"cards do not overlap: "+i);
                check(card.contains(title)&&card.contains(subtitle)&&title.bottom<=subtitle.top,"both painted labels fit without overlap: "+i);
                previous=card;
            }
            boolean portrait=(Boolean)field(page,"portrait");float minimum=portrait?72*d:44*d,maximum=portrait?140*d:132*d;
            if(baseline*.70f>=minimum&&baseline*.70f<=maximum)check(Math.abs(height/baseline-.70f)<.02f,"normal mode cards are 30 percent shorter than v2.8");
            else check(height<=maximum+1&&height+.5f>=44*d,"constrained cards obey height limits");
            RectF region=rect("heroRegion",-1),image=rect("heroImage",-1);for(int i=0;i<3;i++)check(!RectF.intersects(region,rect("cards",i)),"hero scene does not overlap cards: "+i);
            Bitmap hero=(Bitmap)field(page,"heroArt");if(hero==null){Rect source=(Rect)field(page,"portraitSource");check(image.width()<=Math.min(source.width(),source.height())+1,"existing portrait stays at native resolution");}
            else check(Math.abs(image.width()/image.height()-hero.getWidth()/(float)hero.getHeight())<.001f,"transparent hero retains source aspect ratio");
            AccessibilityNodeProvider provider=page.getAccessibilityNodeProvider();AccessibilityNodeInfo root=provider.createAccessibilityNodeInfo(View.NO_ID);check(root.getChildCount()==3,"three independent accessible entries");root.recycle();
            for(int id=1;id<=3;id++){AccessibilityNodeInfo node=provider.createAccessibilityNodeInfo(id);Rect bounds=new Rect();node.getBoundsInParent(bounds);check(node.isClickable()&&node.getText()!=null&&!bounds.isEmpty(),"accessible label and hit bounds: "+id);node.recycle();}
        }catch(Exception error){throw new RuntimeException(error);}
    }
    private boolean visibleText(String text){AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root==null)return false;return !root.findAccessibilityNodeInfosByText(text).isEmpty();}
    private void dialogAction(String text){AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root==null)throw new AssertionError("no dialog");for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(text))if(text.contentEquals(node.getText())&&node.isClickable()){check(node.performAction(AccessibilityNodeInfo.ACTION_CLICK),"dialog action "+text);waitForIdleSync();return;}throw new AssertionError("missing dialog action "+text);}
    @Override public void onStart(){
        Thread watchdog=new Thread(()->{try{Thread.sleep(60000);}catch(InterruptedException error){return;}Bundle timeout=new Bundle();timeout.putString("stream",report+"FAIL mode probe timeout\n");finish(Activity.RESULT_CANCELED,timeout);},"mode-probe-watchdog");watchdog.setDaemon(true);watchdog.start();Bundle result=new Bundle();
        try{
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));boolean portrait="portrait".equals(args.getString("orientation","portrait"));rotate(portrait);page=(ModeSelectView)field(activity,"modePage");
            main(this::drawAndCheck);screenshot("mode-v29-initial.png");
            if("true".equals(args.getString("captureOnly"))){result.putString("stream",report+"SCREENSHOT complete\n");finish(Activity.RESULT_OK,result);return;}
            ModeSelectView.Listener original=(ModeSelectView.Listener)field(page,"listener");int[] calls={0,0,0};main(()->page.setListener(new ModeSelectView.Listener(){public void onLocal(){calls[0]++;}public void onHost(){calls[1]++;}public void onJoin(){calls[2]++;}}));
            for(int i=0;i<3;i++){int index=i;main(()->tap(index));settle();check(calls[index]==1,"real card tap dispatches the original callback slot once: "+index);}
            main(()->{tap(0);tap(1);tap(2);});settle();check(calls[0]==2&&calls[1]==1&&calls[2]==1,"confirmation blocks duplicate navigation");
            RectF first=rect("cards",0);main(()->event(MotionEvent.ACTION_DOWN,first.centerX(),first.centerY()));SystemClock.sleep(110);main(()->{try{check(((float[])field(page,"pressAmounts"))[0]>.5f,"pressed animation has a real intermediate state");}catch(Exception error){throw new RuntimeException(error);}});screenshot("mode-v29-pressed.png");
            main(()->{event(MotionEvent.ACTION_MOVE,0,page.getHeight()-1);event(MotionEvent.ACTION_UP,0,page.getHeight()-1);});settle();check(calls[0]==2,"dragging out cancels navigation");
            main(()->{event(MotionEvent.ACTION_DOWN,first.centerX(),first.centerY());event(MotionEvent.ACTION_CANCEL,first.centerX(),first.centerY());event(MotionEvent.ACTION_UP,first.centerX(),first.centerY());});settle();check(calls[0]==2,"canceled touch never launches a mode");
            main(()->{tap(0);page.setVisibility(View.INVISIBLE);});settle();check(calls[0]==2,"hiding page cancels pending callback");main(()->{page.setVisibility(View.VISIBLE);page.onEnter();});
            main(()->{page.getAccessibilityNodeProvider().performAction(3,AccessibilityNodeInfo.ACTION_CLICK,null);});settle();check(calls[2]==2,"accessible join action reaches existing callback");
            main(()->{page.onEnter();page.requestFocus();page.onKeyDown(KeyEvent.KEYCODE_DPAD_DOWN,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN));page.onKeyDown(KeyEvent.KEYCODE_ENTER,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));});settle();check(calls[1]==2,"keyboard mode selection works");
            main(()->event(MotionEvent.ACTION_DOWN,first.centerX(),first.centerY()));rotate(!portrait);main(()->{try{check((Integer)field(page,"pressed")==-1,"rotation clears stale pressed state");drawAndCheck();}catch(Exception error){throw new RuntimeException(error);}});screenshot("mode-v29-rotated.png");rotate(portrait);main(this::drawAndCheck);
            main(()->page.setListener(original));main(()->tap(0));settle();FourChessView board=(FourChessView)field(activity,"board");check(!page.isShown()&&board.currentTurn()==0,"actual local mode opens existing game");
            main(()->{check(board.tryNetworkMove(0,5,15,5,14),"prepare existing game state for back check");try{invoke(activity,"showModePage");}catch(Exception error){throw new RuntimeException(error);}});String saved=board.exportState();
            main(()->activity.onBackPressed());settle();check(!page.isShown()&&saved.equals(board.exportState()),"visible back entry preserves game and returns to previous screen");
            main(()->{try{invoke(activity,"showModePage");tap(1);}catch(Exception error){throw new RuntimeException(error);}});settle();check(!page.isShown()&&field(activity,"network")!=null&&(Integer)field(board,"localPlayer")==0,"actual create card enters unchanged host flow");
            main(()->{try{invoke(activity,"closeNetwork");invoke(activity,"showModePage");tap(2);}catch(Exception error){throw new RuntimeException(error);}});settle();check(visibleText("输入房主地址"),"actual join card opens unchanged address dialog");dialogAction("取消");
            main(()->{try{invoke(activity,"showModePage");page.clearFocus();page.onEnter();drawAndCheck();}catch(Exception error){throw new RuntimeException(error);}});screenshot("mode-v29-final.png");
            report.append("SIZE ").append(page.getWidth()).append('x').append(page.getHeight()).append(" CARD ").append(field(page,"cardHeight")).append(" BASELINE ").append(field(page,"referenceCardHeight")).append(" DENSITY ").append(page.getResources().getDisplayMetrics().densityDpi).append(" FONT ").append(page.getResources().getConfiguration().fontScale).append(" HERO ").append(field(page,"heroArt")==null?"native-portrait":"transparent-art").append('\n');
            result.putString("stream",report.toString());finish(Activity.RESULT_OK,result);
        }catch(Throwable failure){result.putString("stream",report+"FAIL "+android.util.Log.getStackTraceString(failure));finish(Activity.RESULT_CANCELED,result);}
    }
}
