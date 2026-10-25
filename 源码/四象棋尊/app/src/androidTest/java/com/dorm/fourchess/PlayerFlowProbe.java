package com.dorm.fourchess;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Native forms and exit checks plus real TCP nickname and room-close integration. */
public final class PlayerFlowProbe extends Instrumentation {
    private Bundle args;private MainActivity activity;private FourChessView board;private ModeSelectView mode;private final StringBuilder report=new StringBuilder();
    @Override public void onCreate(Bundle args){super.onCreate(args);this.args=args;start();}
    private Object field(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    private void call(String name)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity);}
    private void check(boolean value,String label){if(!value)throw new AssertionError(label);report.append("PASS ").append(label).append('\n');}
    private void main(Runnable task){java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();runOnMainSync(()->{try{task.run();}catch(Throwable t){error.set(t);}});waitForIdleSync();if(error.get()!=null)throw new AssertionError(error.get());}
    private void settle(){SystemClock.sleep(360);waitForIdleSync();}
    private void await(BooleanSupplier condition,String label){for(int i=0;i<140;i++){if(condition.getAsBoolean()){check(true,label);return;}SystemClock.sleep(50);}throw new AssertionError(label);}
    private AlertDialog dialog(String name){try{return (AlertDialog)field(activity,name);}catch(Exception e){throw new RuntimeException(e);}}
    private void click(String name,int which){main(()->{AlertDialog dialog=dialog(name);check(dialog!=null&&dialog.isShowing(),"dialog is visible: "+name);dialog.getButton(which).performClick();});settle();}
    private void edits(View view,List<EditText> inputs){if(view instanceof EditText)inputs.add((EditText)view);else if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)edits(((ViewGroup)view).getChildAt(i),inputs);}
    private void fill(String... values){main(()->{List<EditText> inputs=new ArrayList<>();edits(dialog("entryDialog").getWindow().getDecorView(),inputs);check(inputs.size()==values.length,"correct number of editable player fields");for(int i=0;i<values.length;i++)inputs.get(i).setText(values[i]);});}
    private Button button(View view,String text){if(view instanceof Button&&view.isShown()&&text.equals(((Button)view).getText().toString()))return (Button)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){Button found=button(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}return null;}
    private void mode(int index){main(()->{try{RectF r=((RectF[])field(mode,"cards"))[index];long now=SystemClock.uptimeMillis();for(int type:new int[]{0,1}){MotionEvent e=MotionEvent.obtain(now,now,type,r.centerX(),r.centerY(),0);mode.dispatchTouchEvent(e);e.recycle();}}catch(Exception e){throw new RuntimeException(e);}});settle();}
    private String[] names(){try{return ((String[])field(field(activity,"playerStrip"),"titles")).clone();}catch(Exception e){throw new RuntimeException(e);}}
    private void namesAre(String... expected){check(Arrays.equals(names(),expected),"four HUD nicknames match current players");}
    private void exit(boolean accept){main(()->{try{Button exit=button((View)field(activity,"gameUi"),"退出");if(exit==null)exit=button((View)field(activity,"gameUi"),"退出对局");check(exit!=null,"visible match exit entry");Rect r=new Rect();check(exit.getGlobalVisibleRect(r),"exit is within viewport");float d=exit.getResources().getDisplayMetrics().density;check(r.width()+.5>=44*d&&r.height()+.5>=44*d,"exit target is at least 44dp");exit.performClick();}catch(Exception e){throw new RuntimeException(e);}});click("exitDialog",accept?AlertDialog.BUTTON_POSITIVE:AlertDialog.BUTTON_NEGATIVE);}
    private void shot(String name)throws Exception{java.util.concurrent.CountDownLatch frame=new java.util.concurrent.CountDownLatch(1);main(()->{View root=activity.getWindow().getDecorView();root.getViewTreeObserver().registerFrameCommitCallback(frame::countDown);root.invalidate();});if(!frame.await(3,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("frame timeout");SystemClock.sleep(100);Bitmap b=getUiAutomation().takeScreenshot();try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getFilesDir(),name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    private static final class Client implements NetworkManager.Callback {
        volatile int owner=-1;volatile String[] names;volatile String closed;volatile String state;
        public void onNetworkStatus(String s){}public void onAssigned(int value){owner=value;}public void onState(String value){state=value;}public void onHostMove(int o,int sx,int sy,int tx,int ty){}public void onNames(String[] value){names=value;}public void onConnectionClosed(String value){closed=value;}
    }
    @Override public void onStart(){Bundle result=new Bundle();List<NetworkManager> resources=new ArrayList<>();try{
        getTargetContext().getSharedPreferences("player_names",0).edit().clear().commit();activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));board=(FourChessView)field(activity,"board");mode=(ModeSelectView)field(activity,"modePage");boolean portrait="portrait".equals(args.getString("orientation","portrait"));main(()->activity.setRequestedOrientation(portrait?1:0));await(()->(mode.getHeight()>=mode.getWidth())==portrait,"requested orientation is applied");settle();
        check("fallback".equals(PlayerNames.clean(" \n|\u200b ","fallback")),"empty and control-only names use fallback");check(PlayerNames.clean("一二三四五六七八九十一二三四","x").codePointCount(0,PlayerNames.clean("一二三四五六七八九十一二三四","x").length())==12,"name length capped to twelve characters");
        String initial=board.exportState();mode(0);check(mode.isShown(),"name form does not start a match early");click("entryDialog",-2);check(mode.isShown()&&initial.equals(board.exportState()),"canceling entry preserves lobby and board");
        mode(0);fill("阿红","小绿","蓝|\n朋友","金陵棋友");shot("players-name-form.png");click("entryDialog",-1);check(!mode.isShown(),"local match starts after accepting names");namesAre("阿红","小绿","蓝朋友","金陵棋友");check(activity.getSharedPreferences("player_names",0).getString("local_0","").equals("阿红"),"local names are saved");
        main(()->check(board.tryNetworkMove(0,5,15,5,14),"existing legal move remains functional"));String moved=board.exportState();exit(false);check(moved.equals(board.exportState()),"cancel exit preserves current chess position");main(()->activity.onBackPressed());click("exitDialog",-2);check(moved.equals(board.exportState()),"system back asks before leaving match");shot("players-local-match.png");main(()->{try{call("showExitConfirm");}catch(Exception e){throw new RuntimeException(e);}});shot("players-exit-confirm.png");click("exitDialog",-1);check(mode.isShown()&&field(activity,"network")==null&&!(Boolean)field(activity,"matchActive"),"confirmed exit returns to mode lobby");int stopped=(Integer)field(activity,"turnSeconds");SystemClock.sleep(1150);check((Integer)field(activity,"turnSeconds")==stopped,"lobby timer stops after exit");
        mode(0);main(()->{List<EditText> inputs=new ArrayList<>();edits(dialog("entryDialog").getWindow().getDecorView(),inputs);check(inputs.get(0).getText().toString().equals("阿红"),"saved names prefill next entry");});click("entryDialog",-2);
        mode(2);main(()->check(!dialog("entryDialog").getButton(-1).isEnabled(),"empty room address cannot submit"));fill("我的联机名","127.0.0.1");main(()->check(dialog("entryDialog").getButton(-1).isEnabled(),"room address enables join"));click("entryDialog",-2);check(mode.isShown(),"cancel join keeps lobby");
        mode(1);fill("宿舍房主");click("entryDialog",-1);await(()->{try{return ((TextView)field(activity,"networkStatus")).getText().toString().contains("房间地址");}catch(Exception e){return false;}},"host room opens after nickname confirmation");namesAre("宿舍房主","待加入","待加入","待加入");
        if("true".equals(args.getString("network","false"))){
            Client[] clients={new Client(),new Client(),new Client()};String[] labels={"绿方室友","蓝方室友","金方室友"};NetworkManager[] nets=new NetworkManager[3];
            for(int i=0;i<3;i++){nets[i]=new NetworkManager(getTargetContext(),clients[i],labels[i]);resources.add(nets[i]);nets[i].join("127.0.0.1");final int index=i;await(()->clients[index].owner>0,"TCP client assigned: "+i);}
            String[] expected={"宿舍房主","绿方室友","蓝方室友","金方室友"};await(()->Arrays.equals(names(),expected),"host receives three actual TCP nicknames");for(int i=0;i<3;i++){final int index=i;await(()->Arrays.equals(clients[index].names,expected),"nickname roster reaches TCP client: "+i);}shot("players-host-roster.png");
            NetworkManager hostNetwork=(NetworkManager)field(activity,"network");main(()->hostNetwork.hostMove(0,5,15,5,14));await(()->board.currentTurn()==1,"host move reaches existing board logic");String hostMoveState=board.exportState();await(()->hostMoveState.equals(clients[0].state),"host move broadcasts actual state in background");
            nets[0].clientMove(1,3,13,4,13);await(()->board.currentTurn()==2,"green TCP move advances turn");nets[1].clientMove(2,13,3,13,4);await(()->board.currentTurn()==3,"blue TCP move advances turn");nets[2].clientMove(3,15,5,14,5);await(()->board.currentTurn()==0,"gold TCP move completes four turns");String fourMoves=board.exportState();nets[0].clientMove(0,5,14,5,13);SystemClock.sleep(220);check(fourMoves.equals(board.exportState()),"client cannot move under another player's seat");
            nets[0].close();await(()->"待加入".equals(names()[1]),"leaving client releases its nickname and seat");Client replacement=new Client();NetworkManager newPeer=new NetworkManager(getTargetContext(),replacement,"补位棋友");resources.add(newPeer);newPeer.join("127.0.0.1");await(()->replacement.owner==1,"replacement client reuses released seat");await(()->"补位棋友".equals(names()[1]),"replacement nickname updates HUD");exit(true);await(()->clients[1].closed!=null&&clients[2].closed!=null&&replacement.closed!=null,"host exit notifies every remaining TCP client");check(clients[1].closed.contains("房主已退出"),"clients receive explicit host close reason");
        }else exit(true);
        check(mode.isShown(),"host exit returns to lobby");
        mode(1);fill("再次开房");click("entryDialog",-1);await(()->{try{return ((TextView)field(activity,"networkStatus")).getText().toString().contains("房间地址");}catch(Exception e){return false;}},"port is reusable after leaving room");exit(true);
        if("true".equals(args.getString("network","false"))){
            Client serverCallback=new Client();NetworkManager remote=new NetworkManager(getTargetContext(),serverCallback,"远端房主");resources.add(remote);remote.host(board);SystemClock.sleep(200);mode(2);fill("入房棋友","127.0.0.1");click("entryDialog",-1);await(()->"远端房主".equals(names()[0])&&"入房棋友".equals(names()[1]),"real join form sends nickname to remote host");check((Integer)field(board,"localPlayer")==1,"join retains original seat assignment");shot("players-client-match.png");remote.close();await(()->dialog("connectionDialog")!=null,"host closing shows disconnected dialog");check(!(Boolean)field(activity,"matchActive"),"disconnected game timer is paused");click("connectionDialog",-1);check(mode.isShown()&&field(activity,"network")==null,"disconnect action safely returns to lobby");
        }
        shot("players-final-lobby.png");report.append("SIZE ").append(mode.getWidth()).append('x').append(mode.getHeight()).append('\n');result.putString("stream",report.toString());finish(Activity.RESULT_OK,result);
    }catch(Throwable error){result.putString("stream",report+"FAIL "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}finally{for(NetworkManager resource:resources)resource.close();}}
}