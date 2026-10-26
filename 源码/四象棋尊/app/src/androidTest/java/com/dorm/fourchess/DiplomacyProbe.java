package com.dorm.fourchess;

import android.app.*;
import android.os.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.*;

/** Actual rule, local dialog and three-client TCP checks for match diplomacy. */
public final class DiplomacyProbe extends Instrumentation {
    private final StringBuilder log=new StringBuilder();private MainActivity activity;private String orientation;private boolean runNetwork;
    private void check(boolean yes,String label){if(!yes)throw new AssertionError(label);log.append("PASS ").append(label).append('\n');}
    private Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private Object call(Object o,String method,Class<?>[] types,Object... args)throws Exception{Method m=o.getClass().getDeclaredMethod(method,types);m.setAccessible(true);return m.invoke(o,args);}
    @SuppressWarnings("unchecked") private List<FourChessView.Piece> pieces(FourChessView b)throws Exception{return (List<FourChessView.Piece>)field(b,"pieces");}
    private FourChessView.Piece at(FourChessView b,int x,int y)throws Exception{for(FourChessView.Piece p:pieces(b))if(p.x==x&&p.y==y)return p;return null;}
    private void main(Runnable r){runOnMainSync(r);waitForIdleSync();}
    private final String position="0,-1,-1,-1,-1|0,J,9,18;1,J,0,9;2,J,9,0;3,J,18,9;0,C,9,9;1,C,9,10;2,C,10,10;3,B,10,9;";
    private void pact(FourChessView b,int a,int other){check(b.tryDiplomacy(a,0,other),"offer valid "+a+"/"+other);check(!b.allied(a,other),"offer alone gives no protection");check(b.tryDiplomacy(other,1,a)&&b.allied(a,other)&&b.allied(other,a),"accept creates mutual treaty");}
    private void rules()throws Exception{
        FourChessView b=new FourChessView(getTargetContext());b.setSoundEnabled(false);b.importState(position);
        for(int a=0;a<4;a++)for(int other=a+1;other<4;other++){b.newGame();pact(b,a,other);check(b.tryDiplomacy(a,3,other),"either pair may break");check(b.treatyBroken(a,other)&&!b.allied(a,other),"breaking removes protection");check(!b.tryDiplomacy(a,0,other)&&!b.tryDiplomacy(other,0,a),"neither party can re-ally in this match");}
        b.importState(position);pact(b,0,1);String treaty=b.exportState();
        check(!b.tryNetworkMove(0,9,9,9,10)&&treaty.equals(b.exportState()),"rook cannot capture allied piece and no turn consumed");
        check(!b.tryDiplomacy(0,0,0)&&!b.tryDiplomacy(0,1,2)&&!b.tryDiplomacy(9,4,-1)&&!b.tryDiplomacy(0,4,1),"self alliance invalid accept spoofed seat and malformed surrender rejected");
        FourChessView copy=new FourChessView(getTargetContext());copy.setSoundEnabled(false);copy.importState(treaty);check(copy.exportState().equals(treaty)&&copy.allied(0,1),"treaty and offers synchronize atomically");
        check(b.tryDiplomacy(0,3,1)&&b.tryNetworkMove(0,9,9,9,10),"capture is enabled after explicit treaty break");
        check(b.undoSteps(2)&&b.treatyBroken(0,1)&&!b.allied(0,1)&&at(b,9,10)!=null,"undo restores pieces but cannot restore broken treaty");
        check(b.undoSteps(b.undoCount())&&!b.tryDiplomacy(1,0,0),"undo past original invitation cannot reset broken pair");
        copy.importState(b.exportState());check(copy.exportState().equals(b.exportState()),"sticky broken pair survives export even before original invitation");
        b.newGame();check(!b.treatyBroken(0,1)&&b.tryDiplomacy(0,0,1),"new match resets treaty eligibility");
        b.importState(position);pact(b,0,1);pact(b,1,2);check(!b.allied(0,2),"alliances do not transitively protect allies of allies");pieces(b).add(new FourChessView.Piece(2,'C',9,12));check(!b.tryNetworkMove(0,9,9,9,12),"allied rook still blocks the ray to an enemy behind it");
        b.importState(position.replace("0,C,9,9;1,C,9,10;","0,P,9,9;3,B,9,10;").replace("3,B,10,9;","1,C,9,12;"));pact(b,0,1);check(!b.tryNetworkMove(0,9,9,9,12),"cannon also cannot capture ally beyond a screen");check(b.tryDiplomacy(1,3,0)&&b.tryNetworkMove(0,9,9,9,12),"cannon capture resumes after break");
        b.importState(position);pact(b,0,1);String valid=b.exportState();String base=valid.substring(0,valid.lastIndexOf('|')+1);
        for(String invalid:new String[]{"D1,1,1,0,1,1,0,1","D1,0,0,1,1,0,0,1","D1,64,0,0,1,1,0,1","D1,1,0,0,0,-1,-1,-1","D1,1,0,0,1,1,0,0"}){b.importState(base+invalid);check(valid.equals(b.exportState()),"malformed diplomacy rejected without mutation "+invalid);}
        for(int id:new int[]{4,7,8,9,11,12,18,19,25,30}){
            Method fixture=DraftSkillProbe.class.getDeclaredMethod("fixture",int.class,int.class);fixture.setAccessible(true);b.importState((String)fixture.invoke(new DraftSkillProbe(),0,id));pact(b,0,1);
            int sx=9,sy=9,tx=10,ty=10;if(id==4||id==25){sx=tx;sy=ty;}if(id==9||id==19){tx=9;ty=11;}if(id==30){sx=10;sy=10;tx=10;ty=11;}
            String before=b.exportState();check(!b.trySkill(0,id,sx,sy,tx,ty)&&before.equals(b.exportState()),"hostile skill cannot bypass treaty or consume charge "+id);
        }
        Method fixture=DraftSkillProbe.class.getDeclaredMethod("fixture",int.class,int.class);fixture.setAccessible(true);b.importState((String)fixture.invoke(new DraftSkillProbe(),0,25));pieces(b).add(new FourChessView.Piece(2,'C',10,11));pact(b,0,1);
        check(b.trySkill(0,25,10,11,10,11)&&at(b,10,11).sealed&&!at(b,10,10).sealed,"area seal hits enemy while protecting neighbouring ally");
        b.importState(position);pact(b,0,1);check(b.tryDiplomacy(1,4,-1)&&b.currentTurn()==0&&!b.isPlayerAlive(1)&&!b.allied(0,1),"out of turn surrender removes own side and treaty without advancing actor");
        check(b.undoSteps(1)&&b.isPlayerAlive(1)&&b.allied(0,1),"local undo can restore surrender as explained in confirmation");
        check(b.tryDiplomacy(0,4,-1)&&b.currentTurn()==1,"current player surrender advances to next survivor");
        check(!b.tryDiplomacy(0,0,2)&&!b.tryNetworkMove(0,9,9,9,10),"eliminated player can neither negotiate nor move");
        b.newGame();for(int i=0;i<3;i++)check(b.tryDiplomacy(i,4,-1),"sequential surrender "+i);
        check(b.liveCount()==1&&b.currentTurn()==3&&!b.canAct()&&!b.tryDiplomacy(3,4,-1)&&!b.tryNetworkMove(3,18,5,17,5),"last survivor wins and finished game rejects further actions");
    }
    private AlertDialog dialog()throws Exception{return (AlertDialog)field(activity,"diplomacyDialog");}
    private void invoke(String name,Class<?>[] types,Object... values)throws Exception{call(activity,name,types,values);}
    private void pick(int index)throws Exception{AlertDialog d=dialog();check(d!=null&&d.isShowing(),"actual diplomacy dialog visible");d.getListView().performItemClick(null,index,index);}
    private void button(int which)throws Exception{AlertDialog d=dialog();check(d!=null&&d.getButton(which)!=null,"actual diplomacy action exists");d.getButton(which).performClick();}
    private interface UiStep{void run()throws Exception;}
    private void step(UiStep task)throws Exception{final Throwable[] error={null};main(()->{try{task.run();}catch(Throwable t){error[0]=t;}});if(error[0]!=null)throw new AssertionError(error[0]);}
    private void ui()throws Exception{
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
        main(()->activity.setRequestedOrientation("landscape".equals(orientation)?0:1));SystemClock.sleep(600);
        step(()->{
            invoke("startMatch",new Class<?>[]{String[].class},(Object)new String[]{"红盟主","绿盟主","蓝盟主","金盟主"});
            invoke("showMatchMenu",new Class<?>[0]);pick(1);pick(0);pick(0);button(-1);
        });
        step(()->{FourChessView b=(FourChessView)field(activity,"board");
            check(b.allianceOffered(0,1)&&!b.allied(0,1),"real UI sends invitation and waits for consent");
            invoke("chooseDiplomacyActor",new Class<?>[]{boolean.class},false);pick(1);pick(0);button(-1);
        });
        step(()->{FourChessView b=(FourChessView)field(activity,"board");
            check(b.allied(0,1),"other local player accepts using real UI");
            invoke("showTreaties",new Class<?>[]{int.class},0);
        });SystemClock.sleep(200);shot("treaties");
        step(()->{pick(0);button(-2);});
        step(()->{FourChessView b=(FourChessView)field(activity,"board");check(b.allied(0,1),"cancel break preserves treaty");invoke("showTreaties",new Class<?>[]{int.class},0);pick(0);button(-1);});
        step(()->{FourChessView b=(FourChessView)field(activity,"board");check(b.treatyBroken(0,1),"confirmed break uses actual action");invoke("confirmSurrender",new Class<?>[]{int.class},0);});shot("surrender");
        step(()->button(-2));
        step(()->{FourChessView b=(FourChessView)field(activity,"board");check(b.isPlayerAlive(0),"cancel surrender preserves side");invoke("confirmSurrender",new Class<?>[]{int.class},0);button(-1);});
        step(()->{FourChessView b=(FourChessView)field(activity,"board");check(!b.isPlayerAlive(0)&&b.currentTurn()==1,"confirmed surrender eliminates only own side");check(((TextView)field(activity,"moveLog")).getText().toString().contains("投降"),"surrender appears in actual history");});shot("match");main(()->activity.finish());activity=null;
    }
    private void shot(String name)throws Exception{android.graphics.Bitmap image=getUiAutomation().takeScreenshot();try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getFilesDir(),"diplomacy-"+name+".png"))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}image.recycle();}
    private static final class Client implements NetworkManager.Callback {
        volatile int owner=-1;volatile String state,closed;
        public void onNetworkStatus(String s){}public void onAssigned(int o){owner=o;}public void onState(String s){state=s;}public void onHostMove(int o,int sx,int sy,int tx,int ty){}public void onConnectionClosed(String s){closed=s;}
    }
    private interface Condition{boolean ready()throws Exception;}
    private void await(Condition condition,String label)throws Exception{for(int i=0;i<80;i++){if(condition.ready()){check(true,label);return;}SystemClock.sleep(75);}throw new AssertionError("timeout "+label);}
    private boolean readMain(Condition condition)throws Exception{final boolean[] out={false};final Exception[] error={null};main(()->{try{out[0]=condition.ready();}catch(Exception e){error[0]=e;}});if(error[0]!=null)throw error[0];return out[0];}
    private void network()throws Exception{
        final FourChessView[] board={null};main(()->{board[0]=new FourChessView(getTargetContext());board[0].setSoundEnabled(false);board[0].importState(position.replaceFirst("^0,","1,"));});
        final NetworkManager[] host={null};ArrayList<NetworkManager> resources=new ArrayList<>();
        NetworkManager.Callback callback=new NetworkManager.Callback(){
            public void onNetworkStatus(String s){}public void onAssigned(int o){}public void onState(String s){}
            public void onHostMove(int o,int sx,int sy,int tx,int ty){main(()->{if(board[0].tryNetworkMove(o,sx,sy,tx,ty))host[0].broadcast(board[0].exportState());});}
            public void onHostDiplomacy(int o,int a,int t){main(()->{if(board[0].tryDiplomacy(o,a,t))host[0].broadcast(board[0].exportState());});}
        };
        try{
            host[0]=new NetworkManager(getTargetContext(),callback,"盟约房主");resources.add(host[0]);host[0].host(board[0]);SystemClock.sleep(200);
            Client[] clients={new Client(),new Client(),new Client()};NetworkManager[] nets=new NetworkManager[3];
            for(int i=0;i<3;i++){nets[i]=new NetworkManager(getTargetContext(),clients[i],"盟友"+i);resources.add(nets[i]);nets[i].join("127.0.0.1");final int n=i;await(()->clients[n].owner==n+1&&clients[n].state!=null,"real TCP player joins with diplomacy capability "+i);}
            nets[0].clientDiplomacy(1,0,0);await(()->readMain(()->board[0].allianceOffered(1,0)),"remote invitation reaches authoritative host");
            main(()->{check(board[0].tryDiplomacy(0,1,1),"host independently accepts remote invitation");host[0].broadcast(board[0].exportState());});
            await(()->readMain(()->board[0].exportState().equals(clients[0].state)&&clients[0].state.equals(clients[1].state)&&clients[1].state.equals(clients[2].state)),"three clients receive identical treaty state");
            String before=clients[0].state;nets[0].clientMove(1,9,10,9,9);nets[1].clientDiplomacy(1,4,-1);SystemClock.sleep(400);
            check(readMain(()->before.equals(board[0].exportState())),"host rejects allied capture and forged diplomacy actor");
            nets[0].clientDiplomacy(1,3,0);await(()->readMain(()->board[0].treatyBroken(1,0)),"remote break applied by host");nets[0].clientDiplomacy(1,0,0);SystemClock.sleep(250);check(readMain(()->!board[0].allianceOffered(1,0)),"network re-alliance remains forbidden");
            nets[0].clientMove(1,9,10,9,9);await(()->readMain(()->board[0].currentTurn()==2),"remote capture works after treaty broken");
            nets[1].clientDiplomacy(2,4,-1);await(()->readMain(()->!board[0].isPlayerAlive(2)&&board[0].currentTurn()==3),"remote surrender retires side and advances turn");
            await(()->readMain(()->board[0].exportState().equals(clients[0].state)&&clients[0].state.equals(clients[1].state)&&clients[1].state.equals(clients[2].state)),"surrender synchronized to every client including surrendering seat");
            check(clients[0].closed==null&&clients[1].closed==null&&clients[2].closed==null,"surrender keeps room and spectator connection open");
            nets[1].close();SystemClock.sleep(250);
            try(Socket raw=new Socket("127.0.0.1",27888)){raw.setSoTimeout(3000);PrintWriter out=new PrintWriter(raw.getOutputStream(),true);out.println("CAPS|SK4");out.println("HELLO|b2xk");BufferedReader in=new BufferedReader(new InputStreamReader(raw.getInputStream()));boolean rejected=false;for(int i=0;i<12;i++){String line=in.readLine();if(line==null)break;if(line.contains("diplomacy_require_v51")){rejected=true;break;}}check(rejected,"ordinary room explicitly rejects clients missing diplomacy support");}
        }finally{for(NetworkManager net:resources)net.close();SystemClock.sleep(300);}
    }
    @Override public void onCreate(Bundle args){super.onCreate(args);orientation=args==null?"portrait":args.getString("orientation","portrait");runNetwork=args==null||!"false".equals(args.getString("network","true"));start();}
    @Override public void onStart(){Bundle result=new Bundle();try{final Throwable[] error={null};main(()->{try{rules();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError(error[0]);ui();if(runNetwork)network();log.append("DIPLOMACY COMPLETE\n");result.putString("stream",log.toString());finish(Activity.RESULT_OK,result);}catch(Throwable error){if(activity!=null)main(()->activity.finish());result.putString("stream",log+"FAIL "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}}
}
