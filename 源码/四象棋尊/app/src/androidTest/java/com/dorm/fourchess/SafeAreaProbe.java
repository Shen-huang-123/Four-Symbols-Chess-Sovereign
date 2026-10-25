package com.dorm.fourchess;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;

/** Actual-window and injected-cutout regression for the formerly blank scene margins. */
public final class SafeAreaProbe extends Instrumentation {
    private Bundle args;private MainActivity activity;private FrameLayout root;private ModeSelectView mode;
    private final StringBuilder log=new StringBuilder();
    @Override public void onCreate(Bundle args){super.onCreate(args);this.args=args;start();}
    private Object field(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    private void call(Object target,String name)throws Exception{Method m=target.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(target);}
    private void check(boolean value,String label){if(!value)throw new AssertionError(label);log.append("PASS ").append(label).append('\n');}
    private void main(Runnable task){java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();runOnMainSync(()->{try{task.run();}catch(Throwable e){error.set(e);}});waitForIdleSync();if(error.get()!=null)throw new AssertionError(error.get());}
    private void settle(){SystemClock.sleep(360);waitForIdleSync();}
    private void rotate(boolean portrait){main(()->activity.setRequestedOrientation(portrait?1:0));for(int i=0;i<60;i++){settle();if((root.getHeight()>=root.getWidth())==portrait)return;}throw new AssertionError("rotation timeout");}
    private void shot(String name)throws Exception{java.util.concurrent.CountDownLatch latch=new java.util.concurrent.CountDownLatch(1);main(()->{root.getViewTreeObserver().registerFrameCommitCallback(latch::countDown);root.invalidate();});if(!latch.await(3,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("no frame");SystemClock.sleep(120);Bitmap b=getUiAutomation().takeScreenshot();try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getFilesDir(),name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    private void tap(int index){try{RectF r=((RectF[])field(mode,"cards"))[index];long now=SystemClock.uptimeMillis();for(int type:new int[]{0,1}){MotionEvent e=MotionEvent.obtain(now,now,type,r.centerX(),r.centerY(),0);mode.dispatchTouchEvent(e);e.recycle();}}catch(Exception e){throw new RuntimeException(e);}}
    private void geometry(boolean lobby){
        try{
            check(root.getBackground() instanceof CourtyardScene,"scenery belongs to full window");
            int[] location=new int[2];root.getLocationOnScreen(location);check(location[0]==0&&location[1]==0,"window begins at physical screen origin");
            View safe=lobby?mode:root.getChildAt(0);check(safe.getLeft()>=root.getPaddingLeft()&&safe.getTop()>=root.getPaddingTop()&&safe.getRight()<=root.getWidth()-root.getPaddingRight()&&safe.getBottom()<=root.getHeight()-root.getPaddingBottom(),"interactive page stays inside safe area");
            Bitmap b=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);root.draw(new Canvas(b));
            int[][] strips={{0,0,root.getWidth(),root.getPaddingTop()},{0,0,root.getPaddingLeft(),root.getHeight()},{root.getWidth()-root.getPaddingRight(),0,root.getWidth(),root.getHeight()},{0,root.getHeight()-root.getPaddingBottom(),root.getWidth(),root.getHeight()}};
            for(int[] r:strips){if(r[2]<=r[0]||r[3]<=r[1])continue;int samples=0,painted=0;for(int y=r[1];y<r[3];y+=Math.max(1,(r[3]-r[1])/12))for(int x=r[0];x<r[2];x+=Math.max(1,(r[2]-r[0])/12)){samples++;if(b.getPixel(x,y)!=0xff191f1c&&Color.alpha(b.getPixel(x,y))==255)painted++;}check(painted==samples,"safe margin fully painted, no former dark strip");}b.recycle();
            if(lobby){for(RectF card:(RectF[])field(mode,"cards"))check(card.left>=0&&card.top>=0&&card.right<=mode.getWidth()&&card.bottom<=mode.getHeight(),"mode card remains visible");check(mode.getAccessibilityNodeProvider().createAccessibilityNodeInfo(View.NO_ID).getChildCount()==3,"only three mode entries remain");}
        }catch(Exception e){throw new RuntimeException(e);}
    }
    private void inject(int left,int top,int right,int bottom){
        main(()->{WindowInsets current=root.getRootWindowInsets();Rect safe=new Rect(left,top,right,bottom);java.util.List<Rect> bounds=new java.util.ArrayList<>();if(top>0)bounds.add(new Rect(root.getWidth()/2-40,0,root.getWidth()/2+40,top));else if(left>0)bounds.add(new Rect(0,root.getHeight()/2-40,left,root.getHeight()/2+40));WindowInsets insets=new WindowInsets.Builder(current).setInsets(WindowInsets.Type.systemBars(),Insets.NONE).setInsets(WindowInsets.Type.displayCutout(),Insets.of(left,top,right,bottom)).setDisplayCutout(new DisplayCutout(safe,bounds)).build();root.dispatchApplyWindowInsets(insets);root.requestLayout();});settle();
        main(()->check(root.getPaddingLeft()>=left&&root.getPaddingTop()>=top&&root.getPaddingRight()>=right&&root.getPaddingBottom()>=bottom,"injected cutout is reserved for controls"));
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));root=(FrameLayout)field(activity,"screen");mode=(ModeSelectView)field(activity,"modePage");boolean portrait="portrait".equals(args.getString("orientation","portrait"));rotate(portrait);main(()->geometry(true));shot("safe-initial-mode.png");
        for(int side=0;side<3;side++){
            final int index=side;inject(side==1?110:0,side==0?150:0,side==2?110:0,24);
            main(()->geometry(true));if(side==0)shot("safe-cutout-mode.png");main(()->tap(0));settle();main(()->{check(!mode.isShown(),"local entry opens game");geometry(false);});if(side==0)shot("safe-cutout-game.png");
            FourChessView board=(FourChessView)field(activity,"board");String before=board.exportState();main(()->check(board.tryNetworkMove(0,5,15,5,14),"existing legal pawn move succeeds"));check(board.currentTurn()==1,"turn advances");main(()->{try{Method undo=MainActivity.class.getDeclaredMethod("performUndo",int.class);undo.setAccessible(true);undo.invoke(activity,1);}catch(Exception e){throw new RuntimeException(e);}});check(before.equals(board.exportState()),"undo restores game state");
            main(()->{try{call(activity,"showModePage");activity.onBackPressed();}catch(Exception e){throw new RuntimeException(e);}});settle();check(!mode.isShown()&&before.equals(board.exportState()),"back preserves existing board");main(()->{try{call(activity,"showModePage");}catch(Exception e){throw new RuntimeException(e);}});
        }
        main(()->root.requestApplyInsets());settle();rotate(!portrait);main(()->geometry(true));shot("safe-rotated-mode.png");rotate(portrait);main(()->geometry(true));shot("safe-final-mode.png");main(()->tap(0));settle();main(()->geometry(false));shot("safe-final-game.png");
        log.append("SIZE ").append(root.getWidth()).append('x').append(root.getHeight()).append(" INSETS ").append(root.getPaddingLeft()).append(',').append(root.getPaddingTop()).append(',').append(root.getPaddingRight()).append(',').append(root.getPaddingBottom()).append('\n');result.putString("stream",log.toString());finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream",log+"FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}