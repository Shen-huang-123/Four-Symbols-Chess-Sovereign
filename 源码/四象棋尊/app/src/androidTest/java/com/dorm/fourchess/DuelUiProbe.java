package com.dorm.fourchess;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Device checks exercise real measured views, touch events, history and undo together. */
public final class DuelUiProbe extends Instrumentation {
    private final StringBuilder report=new StringBuilder();
    private Bundle arguments;
    private Activity testedActivity;
    @Override public void onCreate(Bundle args){super.onCreate(args);arguments=args;start();}
    private Object field(Object object,String name)throws Exception{
        Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);
    }
    private void invoke(Object object,String name,Class<?>[] types,Object... args)throws Exception{
        Method m=object.getClass().getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(object,args);
    }
    private void check(boolean ok,String label){android.util.Log.i("DuelUiProbe",(ok?"PASS ":"FAIL ")+label);if(!ok)throw new AssertionError(label);report.append("PASS ").append(label).append('\n');}
    private void main(Runnable task){java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();runOnMainSync(()->{try{task.run();}catch(Throwable error){failure.set(error);}});waitForIdleSync();if(failure.get()!=null)throw new AssertionError("Main-thread probe failed",failure.get());}
    private void tap(View view,float x,float y){
        long now=SystemClock.uptimeMillis();
        MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0);
        MotionEvent up=MotionEvent.obtain(now,now+40,MotionEvent.ACTION_UP,x,y,0);
        view.dispatchTouchEvent(down);view.dispatchTouchEvent(up);down.recycle();up.recycle();
    }
    private void event(View view,int action,float x,float y){long now=SystemClock.uptimeMillis();MotionEvent e=MotionEvent.obtain(now,now,action,x,y,0);view.dispatchTouchEvent(e);e.recycle();}
    private Button findButton(View view,String label){
        if(!view.isShown())return null;
        if(view instanceof Button&&((Button)view).getText().toString().equals(label))return (Button)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){Button found=findButton(((ViewGroup)view).getChildAt(i),label);if(found!=null)return found;}
        return null;
    }
    private void screenshot(String name)throws Exception{
        java.util.concurrent.CountDownLatch committed=new java.util.concurrent.CountDownLatch(1);runOnMainSync(()->{View decor=testedActivity.getWindow().getDecorView();decor.getViewTreeObserver().registerFrameCommitCallback(committed::countDown);decor.invalidate();});
        if(!committed.await(3,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("Frame did not commit for "+name);SystemClock.sleep(150);Bitmap bitmap=getUiAutomation().takeScreenshot();
        if(bitmap==null)throw new AssertionError("Screenshot unavailable");
        File output=new File(getTargetContext().getFilesDir(),name);
        try(FileOutputStream stream=new FileOutputStream(output)){bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);}finally{bitmap.recycle();}
    }
    private void assertButtons(View view,ViewGroup root){
        if(view instanceof Button){int[] a=new int[2],b=new int[2];view.getLocationOnScreen(a);root.getLocationOnScreen(b);
            check(view.getWidth()>0&&view.getHeight()>0&&a[0]>=b[0]&&a[1]>=b[1]&&a[0]+view.getWidth()<=b[0]+root.getWidth()&&a[1]+view.getHeight()<=b[1]+root.getHeight(),"button visible: "+((Button)view).getText());
        }
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)assertButtons(((ViewGroup)view).getChildAt(i),root);
    }
    private void textFits(TextView text,String label){
        android.text.Layout layout=text.getLayout();
        int width=text.getWidth()-text.getCompoundPaddingLeft()-text.getCompoundPaddingRight();
        int height=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom();
        boolean fits=layout!=null&&layout.getHeight()<=height+1;
        if(layout!=null)for(int line=0;line<layout.getLineCount();line++)fits&=layout.getLineWidth(line)<=width+1&&layout.getEllipsisCount(line)==0;
        check(fits,label+" text fits ("+width+"x"+height+", layout "+(layout==null?"null":layout.getHeight())+")");
    }
    private void testInsets(Activity activity)throws Exception{
        if(android.os.Build.VERSION.SDK_INT<30)return;
        View screen=(View)field(activity,"screen");android.view.WindowInsets original=screen.getRootWindowInsets();
        int reserve=Math.round(48*screen.getResources().getDisplayMetrics().density);
        android.view.WindowInsets hidden=new android.view.WindowInsets.Builder()
            .setInsetsIgnoringVisibility(android.view.WindowInsets.Type.navigationBars(),android.graphics.Insets.of(0,0,0,reserve))
            .setInsets(android.view.WindowInsets.Type.systemBars(),android.graphics.Insets.NONE)
            .setVisible(android.view.WindowInsets.Type.systemBars(),false).build();
        invoke(activity,"applySafeInsets",new Class<?>[]{android.view.WindowInsets.class},hidden);
        check(screen.getPaddingBottom()==0,"hidden navigation bar does not reserve stable inset");
        android.view.WindowInsets visible=new android.view.WindowInsets.Builder(hidden).setInsets(android.view.WindowInsets.Type.navigationBars(),android.graphics.Insets.of(0,0,reserve,0)).setVisible(android.view.WindowInsets.Type.navigationBars(),true).build();
        invoke(activity,"applySafeInsets",new Class<?>[]{android.view.WindowInsets.class},visible);
        check(screen.getPaddingRight()==reserve,"visible navigation bar remains protected");
        android.view.DisplayCutout cutout=new android.view.DisplayCutout(android.graphics.Insets.of(reserve,0,0,0),new android.graphics.Rect(0,100,reserve,180),null,null,null);
        android.view.WindowInsets camera=new android.view.WindowInsets.Builder(hidden).setDisplayCutout(cutout).build();
        invoke(activity,"applySafeInsets",new Class<?>[]{android.view.WindowInsets.class},camera);
        check(screen.getPaddingLeft()==reserve,"landscape camera cutout remains protected");
        if(original!=null)invoke(activity,"applySafeInsets",new Class<?>[]{android.view.WindowInsets.class},original);else screen.setPadding(0,0,0,0);
    }    private void rotate(Activity activity,boolean portrait){
        main(()->activity.setRequestedOrientation(portrait?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
        for(int i=0;i<60;i++){waitForIdleSync();View screen=activity.getWindow().getDecorView();if((screen.getHeight()>screen.getWidth())==portrait){SystemClock.sleep(180);waitForIdleSync();return;}SystemClock.sleep(100);}
        throw new AssertionError("Orientation did not update");
    }
    private float number(Object object,String name){try{return (Float)field(object,name);}catch(Exception e){throw new RuntimeException(e);}}
    private void tapCell(FourChessView board,int x,int y){tap(board,number(board,"ox")+x*number(board,"cellX"),number(board,"oy")+y*number(board,"cellY"));}
    private void geometry(FourChessView board){
        float spacing=number(board,"cell"),cx=number(board,"cellX"),cy=number(board,"cellY"),ox=number(board,"ox"),oy=number(board,"oy");
        check(Math.abs(cx-cy)<.001f,"square grid has no axis distortion");
        float available=Math.min(board.getWidth(),board.getHeight())-4*board.getResources().getDisplayMetrics().density;
        check(Math.abs(spacing*18.96f-available*.95f)<1,"board scale is 95 percent of the maximum fit");
        check(Math.abs(board.pieceDiameter()/spacing-.70f)<.001f,"rendered piece diameter is 70 percent of spacing");
        int count=0;float radius=board.pieceDiameter()/2;
        for(int x=0;x<19;x++)for(int y=0;y<19;y++)if(x>=5&&x<=13||y>=5&&y<=13){float px=ox+x*cx,py=oy+y*cy;
            if(px-radius<0||py-radius<0||px+radius>board.getWidth()||py+radius>board.getHeight()||Math.round((px-ox)/cx)!=x||Math.round((py-oy)/cy)!=y)throw new AssertionError("point "+x+","+y);count++;
        }check(count==261,"all valid points and complete piece circles fit and round-trip");
        Bitmap layer=Bitmap.createBitmap(board.getWidth(),board.getHeight(),Bitmap.Config.ARGB_8888);
        try{
            board.draw(new android.graphics.Canvas(layer));boolean opaque=true;float[][] samples={{7.3f,7.3f},{7.3f,1.3f},{1.3f,7.3f},{16.7f,7.3f},{7.3f,16.7f}};
            for(float[] sample:samples)opaque&=android.graphics.Color.alpha(layer.getPixel(Math.round(ox+sample[0]*cx),Math.round(oy+sample[1]*cy)))==255;
            check(opaque,"all five board regions remain opaque over scenery");
            if((Boolean)field(board,"compactUi"))check(android.graphics.Color.alpha(layer.getPixel(Math.round(spacing),Math.round(spacing)))==0,"unused cross corners reveal the independent scene");
        }catch(Exception error){throw new RuntimeException(error);}finally{layer.recycle();}
    }
    private void layout(MainActivity activity,FourChessView board,int expected)throws Exception{
        DuelLayout duel=(DuelLayout)board.getParent();ViewGroup root=(ViewGroup)field(activity,"gameUi");
        check(duel.layoutMode()==expected,"responsive breakpoint "+expected);
        View hud=duel.getChildAt(0),desktop=duel.getChildAt(2),mobile=duel.getChildAt(3),controls=expected==DuelLayout.WIDE?desktop:mobile;
        check(hud.getBottom()<=board.getTop()&&board.getLeft()>=0&&board.getRight()<=duel.getWidth()&&board.getBottom()<=duel.getHeight(),"board is below HUD and inside safe viewport");
        check(expected==DuelLayout.PHONE_PORTRAIT?board.getBottom()<=controls.getTop():board.getRight()<=controls.getLeft(),"controls never overlap board");
        check(expected==DuelLayout.WIDE?desktop.isShown()&&!mobile.isShown():mobile.isShown()&&!desktop.isShown(),"only the appropriate control panel is visible");
        if(expected!=DuelLayout.WIDE){check(((android.graphics.RectF)field(board,"sizeToggle")).isEmpty(),"mobile decorative toggle and instructions removed");check(((android.widget.ScrollView)field(activity,"historyScroll")).isShown()==false,"history does not occupy phone board space");textFits((TextView)field(activity,"compactState"),"compact state");}
        else{check(((View)field(activity,"historyScroll")).isShown(),"wide layout retains permanent scrollable history");textFits((TextView)field(activity,"status"),"wide status");textFits((TextView)field(activity,"timer"),"wide timer");}
        for(String label:expected==DuelLayout.WIDE?new String[]{"模式","规则","悔棋","声音 开"}:new String[]{"模式","规则","悔棋","声音","棋谱"}){Button button=findButton(root,label);check(button!=null,"visible action "+label);check(button.getHeight()/root.getResources().getDisplayMetrics().density>=36,"touch target at least 36dp: "+label);textFits(button,label);}
        if(expected==DuelLayout.PHONE_PORTRAIT)check((Integer)field(field(activity,"playerStrip"),"columns")==2,"portrait HUD uses two rows");
        check(duel.getBackground() instanceof CourtyardScene,"scene is an independent drawable below the live board");
        if(expected==DuelLayout.PHONE_PORTRAIT){
            float density=root.getResources().getDisplayMetrics().density;
            check(board.getTop()-hud.getBottom()<=8*density,"portrait HUD and board form a continuous stack");
            check(controls.getTop()-board.getBottom()<=8*density,"portrait dock stays adjacent to board");
        }
        if(expected!=DuelLayout.WIDE)for(String name:new String[]{"模式","规则","悔棋","声音","棋谱"}){
            Button action=findButton(root,name);float density=root.getResources().getDisplayMetrics().density;
            check(action instanceof ActionDockButton&&action.getWidth()>=44*density&&action.getHeight()>=44*density,"icon dock and 44dp touch surface: "+name);
            float label=((ActionDockButton)action).labelSize();android.graphics.Paint glyph=new android.graphics.Paint();glyph.setTextSize(label);
            check(glyph.measureText(name)<action.getWidth(),"painted dock label fits: "+name);
        }
        geometry(board);
    }
    private void dialogAction(String text){
        android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root==null)throw new AssertionError("Dialog missing");
        for(android.view.accessibility.AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(text))if(text.contentEquals(node.getText())&&node.isClickable()){check(node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK),"dialog action "+text);waitForIdleSync();return;}
        throw new AssertionError("Dialog action missing: "+text);
    }
    @Override public void onStart(){
        Thread watchdog=new Thread(()->{try{Thread.sleep(45000);}catch(InterruptedException e){return;}StringBuilder dump=new StringBuilder("FAIL probe timeout\n");for(java.util.Map.Entry<Thread,StackTraceElement[]> entry:Thread.getAllStackTraces().entrySet()){dump.append(entry.getKey().getName()).append("\n");for(StackTraceElement frame:entry.getValue())dump.append(frame).append("\n");}Bundle timed=new Bundle();timed.putString("stream",report.toString()+dump);finish(Activity.RESULT_CANCELED,timed);},"probe-watchdog");watchdog.setDaemon(true);watchdog.start();
        Bundle result=new Bundle();
        try{
            getTargetContext().getSharedPreferences("duel_ui",android.content.Context.MODE_PRIVATE).edit().putBoolean("responsive_v27",true).putBoolean("large_pieces",false).commit();
            MainActivity activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            testedActivity=activity;
            boolean portrait="portrait".equals(arguments.getString("orientation","landscape"));rotate(activity,portrait);
            int expected=Integer.parseInt(arguments.getString("expectedMode",portrait?"1":"0"));
            screenshot("mode-initial.png");
            main(()->{try{testInsets(activity);ModeSelectView page=(ModeSelectView)field(activity,"modePage");((ModeSelectView.Listener)field(page,"listener")).onLocal();}catch(Exception e){throw new RuntimeException(e);}});
            FourChessView board=(FourChessView)field(activity,"board");ViewGroup root=(ViewGroup)field(activity,"gameUi");
            main(()->{try{layout(activity,board,expected);}catch(Exception e){throw new RuntimeException(e);}});
            screenshot("duel-initial.png");if("true".equals(arguments.getString("captureOnly"))){result.putString("stream",report.toString()+"SCREENSHOT complete\n");finish(Activity.RESULT_OK,result);return;}String initial=board.exportState();
            main(()->event(board,MotionEvent.ACTION_DOWN,number(board,"ox")+5*number(board,"cellX"),number(board,"oy")+15*number(board,"cellY")));SystemClock.sleep(550);waitForIdleSync();
            main(()->{try{check((Boolean)field(board,"inspecting"),"long press opens live loupe");check(initial.equals(board.exportState()),"loupe does not change game state");}catch(Exception e){throw new RuntimeException(e);}});screenshot("duel-loupe.png");
            main(()->{event(board,MotionEvent.ACTION_UP,number(board,"ox")+5*number(board,"cellX"),number(board,"oy")+15*number(board,"cellY"));check(initial.equals(board.exportState()),"releasing loupe does not move");
                Button sound=findButton(root,expected==2?"声音 开":"声音");sound.performClick();try{check(!(Boolean)field(board,"soundEnabled"),"visible sound action disables sound");sound.performClick();check((Boolean)field(board,"soundEnabled"),"visible sound action restores sound");}catch(Exception e){throw new RuntimeException(e);}
            });
            int[][] steps={{5,15,5,14},{3,13,4,13},{13,3,13,4},{15,5,14,5}};
            for(int owner=0;owner<4;owner++){int index=owner;int[] step=steps[owner];
                main(()->{tapCell(board,step[0],step[1]);try{check(field(board,"selected")!=null&&!((List<?>)field(board,"moves")).isEmpty(),"real touch selection and legal targets: "+index);}catch(Exception e){throw new RuntimeException(e);}});
                if(owner==0)screenshot("duel-selected.png");
                main(()->{tapCell(board,step[2],step[3]);check(board.currentTurn()==(index+1)%4,"real touch move and turn: "+index);});
            }
            String fourMoves=board.exportState();screenshot("duel-history.png");int clockBefore=(Integer)field(activity,"turnSeconds");SystemClock.sleep(1100);waitForIdleSync();check((Integer)field(activity,"turnSeconds")>clockBefore,"turn clock advances during play");
            main(()->tapCell(board,5,14));int beforeSeconds=(Integer)field(activity,"turnSeconds");
            rotate(activity,!portrait);
            main(()->{try{check(field(activity,"board")==board&&fourMoves.equals(board.exportState()),"rotation preserves the same board and game state");check(((List<?>)field(activity,"displayedMoves")).size()==4,"rotation preserves move history");check((Integer)field(activity,"turnSeconds")>=beforeSeconds,"rotation does not reset clock");check(field(board,"selected")!=null&&!((List<?>)field(board,"moves")).isEmpty(),"selection and legal targets survive rotation");geometry(board);tapCell(board,5,13);check(board.currentTurn()==1,"touch mapping remains correct after rotation");}catch(Exception e){throw new RuntimeException(e);}});
            screenshot("duel-rotated.png");String fifth=board.exportState();rotate(activity,portrait);
            main(()->{try{check(fifth.equals(board.exportState()),"rotation back preserves position");layout(activity,board,expected);if(expected==DuelLayout.WIDE)invoke(activity,"showHistory",new Class<?>[0]);else findButton(root,"棋谱").performClick();}catch(Exception e){throw new RuntimeException(e);}});
            main(()->{try{AlertDialog dialog=(AlertDialog)field(activity,"historyDialog");TextView log=(TextView)field(activity,"dialogMoves");check(dialog.isShowing()&&log.getHeight()>0,"history is available in a measured popup");check(log.getText().toString().equals(((TextView)field(activity,"moveLog")).getText().toString()),"popup history matches live history");Button size=dialog.getButton(AlertDialog.BUTTON_NEUTRAL);float spacing=number(board,"cell");size.performClick();check(Math.abs(board.pieceDiameter()/spacing-.84f)<.001f,"optional large pieces remain available");size.performClick();check(Math.abs(board.pieceDiameter()/spacing-.70f)<.001f,"standard 70 percent pieces restored");}catch(Exception e){throw new RuntimeException(e);}});
            screenshot("duel-popup.png");main(()->{try{((AlertDialog)field(activity,"historyDialog")).dismiss();findButton(root,"悔棋").performClick();}catch(Exception e){throw new RuntimeException(e);}});
            dialogAction("撤回上一步");
            main(()->{try{check(fourMoves.equals(board.exportState()),"visible undo action restores previous move");invoke(activity,"performUndo",new Class<?>[]{int.class},4);check(initial.equals(board.exportState()),"four-step undo restores exact opening");check(((List<?>)field(activity,"displayedMoves")).isEmpty(),"undo synchronizes history");check(!findButton(root,"悔棋").isEnabled(),"empty undo is disabled in active layout");findButton(root,"规则").performClick();}catch(Exception e){throw new RuntimeException(e);}});
            dialogAction("开始下棋");
            main(()->findButton(root,"模式").performClick());check(((View)field(activity,"modePage")).isShown(),"mode action opens independent page");main(activity::onBackPressed);check(initial.equals(board.exportState()),"returning from mode page preserves game");
            main(()->{board.importState("0,-1,-1,-1,-1|0,J,9,18;");try{check(((TextView)field(activity,"status")).getText().toString().contains("红方获胜"),"existing victory state reaches UI");check(board.isPlayerAlive(0)&&!board.isPlayerAlive(1)&&!board.isPlayerAlive(2)&&!board.isPlayerAlive(3),"victory styling uses existing alive data");}catch(Exception e){throw new RuntimeException(e);}});
            screenshot("duel-victory.png");main(()->board.importState(initial));check(initial.equals(board.exportState()),"victory preview restores game state");
            report.append("DENSITY ").append(root.getResources().getDisplayMetrics().densityDpi).append(" FONT_SCALE ").append(root.getResources().getConfiguration().fontScale).append("\nSIZE ").append(root.getWidth()).append('x').append(root.getHeight()).append(" MODE ").append(expected).append(" GRID ").append(number(board,"cell")).append(" PIECE ").append(board.pieceDiameter()).append('\n');
            result.putString("stream",report.toString());finish(Activity.RESULT_OK,result);
        }catch(Throwable failure){result.putString("stream",report+"FAIL "+android.util.Log.getStackTraceString(failure));finish(Activity.RESULT_CANCELED,result);}
    }
}





