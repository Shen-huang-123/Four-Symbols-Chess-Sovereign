package com.dorm.fourchess;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Base64;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

class NetworkManager {
    interface Callback {
        void onNetworkStatus(String s);void onAssigned(int owner);void onState(String state);void onHostMove(int owner,int sx,int sy,int tx,int ty);
        default void onHostSkill(int owner,int skill,int sx,int sy,int tx,int ty){}
        default void onHostDiplomacy(int owner,int action,int target){}
        default void onNames(String[] names){} default void onConnectionClosed(String reason){}
    }
    private static final int PORT=27888;
    private final Callback callback;private final Context context;private final String nickname;
    private volatile boolean running=true;private volatile ServerSocket server;private volatile Socket client;private volatile PrintWriter clientOut;
    private final java.util.concurrent.ExecutorService outbound=java.util.concurrent.Executors.newSingleThreadExecutor(task->{Thread worker=new Thread(task,"fourchess-send");worker.setDaemon(true);return worker;});
    private void send(Runnable task){try{outbound.execute(task);}catch(java.util.concurrent.RejectedExecutionException ignored){}}
    private final List<Peer> peers=new ArrayList<>();private FourChessView board;
    private final String[] names={"待加入","待加入","待加入","待加入"};
    private static final class Peer {final Socket socket;final int owner;PrintWriter out;volatile boolean skills,diplomacy,ready;Peer(Socket socket,int owner){this.socket=socket;this.owner=owner;}}
    NetworkManager(Callback callback){this(callback instanceof Context?(Context)callback:null,callback,"玩家");}
    NetworkManager(Context context,Callback callback,String nickname){this.context=context==null?null:context.getApplicationContext();this.callback=callback;this.nickname=PlayerNames.clean(nickname,"玩家");}
    private static String encode(String value){return Base64.encodeToString(value.getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP|Base64.URL_SAFE);}
    private static String decode(String value){if(value.length()>256)throw new IllegalArgumentException("name too long");return PlayerNames.clean(new String(Base64.decode(value,Base64.NO_WRAP|Base64.URL_SAFE),StandardCharsets.UTF_8),"玩家");}
    void host(FourChessView board){
        this.board=board;names[0]=nickname;publishNames();
        new Thread(()->{try{
            ServerSocket opened=new ServerSocket();opened.setReuseAddress(true);synchronized(this){if(!running){opened.close();return;}server=opened;}opened.bind(new InetSocketAddress(PORT));broadcastStatus();
            while(running){Socket socket=opened.accept();Peer peer=null;synchronized(this){if(running){for(int owner=1;owner<4;owner++){boolean occupied=false;for(Peer existing:peers)if(existing.owner==owner)occupied=true;if(!occupied){peer=new Peer(socket,owner);peers.add(peer);names[owner]="玩家"+(owner+1);break;}}}}if(peer==null){socket.close();continue;}servePeer(peer);}
        }catch(Exception error){if(running)callback.onNetworkStatus("创建失败："+error.getMessage());}},"fourchess-host").start();
    }
    private void servePeer(Peer peer){new Thread(()->{try{
        synchronized(this){if(!running)return;peer.out=new PrintWriter(new OutputStreamWriter(peer.socket.getOutputStream(),StandardCharsets.UTF_8),true);peer.out.println("WELCOME|"+peer.owner);peer.out.println("RULES|DP2");}publishNames();broadcastStatus();
        BufferedReader in=new BufferedReader(new InputStreamReader(peer.socket.getInputStream(),StandardCharsets.UTF_8));String line;
        while(running&&(line=in.readLine())!=null){
            if(line.equals("CAPS|SK4")){peer.skills=true;}
            else if(line.equals("CAPS|DP2")){peer.diplomacy=true;}
            else if(line.startsWith("HELLO|")){
                if(board.isSkillMode()&&!peer.skills){peer.out.println("CLOSED|skills_require_v45");break;}if(!peer.diplomacy){peer.out.println("CLOSED|diplomacy_require_v52");break;}peer.ready=true;peer.out.println("STATE|"+board.exportState());try{String name=decode(line.substring(6));synchronized(this){names[peer.owner]=name;}publishNames();}catch(IllegalArgumentException ignored){}}
            else if(line.startsWith("MOVE|")){String[] fields=line.split("\\|");if(fields.length!=6)continue;try{int claimed=Integer.parseInt(fields[1]);if(claimed==peer.owner&&peer.ready&&(!board.isSkillMode()||peer.skills))callback.onHostMove(peer.owner,Integer.parseInt(fields[2]),Integer.parseInt(fields[3]),Integer.parseInt(fields[4]),Integer.parseInt(fields[5]));}catch(NumberFormatException ignored){}}
            else if(line.startsWith("DIPLOMACY|")){String[] f=line.split("\\|");if(f.length==4&&peer.ready)try{if(Integer.parseInt(f[1])==peer.owner)callback.onHostDiplomacy(peer.owner,Integer.parseInt(f[2]),Integer.parseInt(f[3]));}catch(NumberFormatException ignored){}}
            else if(line.startsWith("SKILL|")){
                String[] fields=line.split("\\|");if(fields.length!=7||!peer.skills||!peer.ready)continue;
                try{if(Integer.parseInt(fields[1])==peer.owner)callback.onHostSkill(peer.owner,Integer.parseInt(fields[2]),Integer.parseInt(fields[3]),Integer.parseInt(fields[4]),Integer.parseInt(fields[5]),Integer.parseInt(fields[6]));}catch(NumberFormatException ignored){}
            }
        }
    }catch(IOException ignored){}finally{
        synchronized(this){peers.remove(peer);names[peer.owner]="待加入";}try{peer.socket.close();}catch(IOException ignored){}if(running){publishNames();broadcastStatus();}
    }},"fourchess-peer").start();}
    private synchronized void broadcastStatus(){if(running)callback.onNetworkStatus("房间地址："+localIp()+" · 已入座 "+(peers.size()+1)+"/4");}
    private synchronized void publishNames(){
        if(!running)return;String[] snapshot=names.clone();StringBuilder message=new StringBuilder("NAMES");for(String name:snapshot)message.append('|').append(encode(name));for(Peer peer:peers)if(peer.out!=null)peer.out.println(message);callback.onNames(snapshot);
    }
    void join(String ip){new Thread(()->{String reason="与房主的连接已断开";try{
        Socket socket=new Socket();synchronized(this){if(!running){socket.close();return;}client=socket;}socket.connect(new InetSocketAddress(ip,PORT),5000);
        synchronized(this){if(!running)return;clientOut=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8),true);clientOut.println("CAPS|SK4");clientOut.println("CAPS|DP2");clientOut.println("HELLO|"+encode(nickname));}callback.onNetworkStatus("已连接房主 "+ip);
        BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));String line;boolean diplomacyHost=false;
        while(running&&(line=in.readLine())!=null){
            if(line.startsWith("WELCOME|"))callback.onAssigned(Integer.parseInt(line.substring(8)));
            else if(line.equals("RULES|DP2")){diplomacyHost=true;}
            else if(line.startsWith("STATE|")){if(!diplomacyHost){reason="结盟对局需要房主升级至v5.2或更新版本";break;}if((line.split("\\|",3)[1].contains(",K1")||line.split("\\|",3)[1].contains(",K2")||line.split("\\|",3)[1].contains(",K3"))){reason="周期抽牌房间需要房主升级v4.5";break;}callback.onState(line.substring(6));}
            else if(line.startsWith("NAMES|")){String[] fields=line.split("\\|",-1);if(fields.length==5){try{String[] values=new String[4];for(int i=0;i<4;i++)values[i]=decode(fields[i+1]);callback.onNames(values);}catch(IllegalArgumentException ignored){}}}
            else if(line.startsWith("CLOSED|")){reason=line.contains("diplomacy_require")?"投降与结盟需要所有玩家安装v5.2或更新版本":line.contains("skills_require")?"技能房间需要所有玩家安装v4.5或更新版本":"房主已退出，房间已关闭";break;}
        }
    }catch(Exception error){reason="连接失败或已断开，请检查地址和同一 Wi-Fi";}finally{
        boolean notify=running;try{if(client!=null)client.close();}catch(IOException ignored){}if(notify)callback.onConnectionClosed(reason);
    }},"fourchess-client").start();}
    void clientMove(int owner,int sx,int sy,int tx,int ty){send(()->{PrintWriter out=clientOut;if(running&&out!=null)out.println("MOVE|"+owner+"|"+sx+"|"+sy+"|"+tx+"|"+ty);});}
    void clientSkill(int owner,int skill,int sx,int sy,int tx,int ty){send(()->{PrintWriter out=clientOut;if(running&&out!=null)out.println("SKILL|"+owner+"|"+skill+"|"+sx+"|"+sy+"|"+tx+"|"+ty);});}
    void clientDiplomacy(int owner,int action,int target){send(()->{PrintWriter out=clientOut;if(running&&out!=null)out.println("DIPLOMACY|"+owner+"|"+action+"|"+target);});}
    void hostSkill(int owner,int skill,int sx,int sy,int tx,int ty){if(running)callback.onHostSkill(owner,skill,sx,sy,tx,ty);}
    void hostMove(int owner,int sx,int sy,int tx,int ty){if(running)callback.onHostMove(owner,sx,sy,tx,ty);}
    synchronized void broadcast(String state){Peer[] targets=peers.toArray(new Peer[0]);send(()->{if(running)for(Peer peer:targets)if(peer.out!=null&&peer.ready)peer.out.println("STATE|"+state);});}
    private String localIp(){try{if(context==null)return "127.0.0.1";WifiManager wifi=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);int ip=wifi.getConnectionInfo().getIpAddress();return(ip&255)+"."+(ip>>8&255)+"."+(ip>>16&255)+"."+(ip>>24&255);}catch(Exception error){return "请查看热点地址";}}
    synchronized void close(){
        if(!running)return;running=false;
        try{if(server!=null)server.close();}catch(IOException ignored){}
        try{if(client!=null)client.close();}catch(IOException ignored){}
        Peer[] targets=peers.toArray(new Peer[0]);send(()->{for(Peer peer:targets){if(peer.out!=null)peer.out.println("CLOSED|host_exit");try{peer.socket.close();}catch(IOException ignored){}}});outbound.shutdown();
    }
}
