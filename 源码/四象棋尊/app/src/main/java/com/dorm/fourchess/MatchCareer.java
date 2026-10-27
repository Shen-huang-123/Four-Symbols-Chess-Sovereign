package com.dorm.fourchess;

import android.content.Context;
import android.os.*;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Settlement journal and per-seat bindings. Reads the board; never decides the winner itself. */
final class MatchCareer {
    private final PlayerArchive store;private final AtomicFile journal;private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler ui=new Handler(Looper.getMainLooper());
    private String matchId="";private long started;private String[] bindings={"","","",""};private boolean settled;private JSONObject metadata;interface Notice { void show(String message); } private final Notice notice;
    MatchCareer(Context c,PlayerArchive a,Notice message){store=a;notice=message;journal=new AtomicFile(new File(c.getFilesDir(),"settlement-journal.json"));worker.execute(()->{try{drain(readJobs());}catch(Exception e){notify("待恢复结算尚未完成："+e.getMessage());}});}

    void begin(String[] ids){matchId=UUID.randomUUID().toString();started=System.currentTimeMillis();bindings=ids.clone();settled=false;metadata=null;}
    void network(JSONObject value,int seat,String localId){metadata=value;String id=value.optString("id");if(!id.matches("[a-f0-9-]{36}"))return;if(!id.equals(matchId)){matchId=id;started=value.optLong("started");settled=false;}bindings=new String[]{"","","",""};if(seat>=0&&seat<4)bindings[seat]=localId;}
    void observe(FourChessView board,boolean networkGame){if(matchId.isEmpty())return;int winner=board.careerWinner();if(winner<0){if(settled){settled=false;JSONObject job=new JSONObject();CareerRules.put(job,"action","retract");CareerRules.put(job,"match",matchId);CareerRules.put(job,"bindings",new JSONArray(Arrays.asList(bindings)));enqueue(job);}return;}if(settled)return;
        if(networkGame&&(metadata==null||!metadata.optString("fingerprint").equals(fingerprint(board.exportState()))))return;
        int[][] totals=board.careerTotals();JSONArray participants=new JSONArray();if(networkGame){JSONArray peers=CareerRules.array(metadata,"players");for(int i=0;i<4;i++){JSONObject card=peers.optJSONObject(i);participants.put(card==null?"":card.optString("id"));}JSONArray incoming=CareerRules.array(metadata,"totals");for(int i=0;i<4;i++){JSONArray row=incoming.optJSONArray(i);if(row!=null){totals[i][0]=Math.max(0,row.optInt(0));totals[i][1]=Math.max(0,row.optInt(1));}}}else for(String id:bindings)participants.put(id);
        long ended=System.currentTimeMillis();JSONObject job=new JSONObject(),records=new JSONObject();CareerRules.put(job,"action","settle");CareerRules.put(job,"bindings",new JSONArray(Arrays.asList(bindings)));for(int seat=0;seat<4;seat++){if(bindings[seat].isEmpty())continue;JSONObject record=new JSONObject();CareerRules.put(record,"id",matchId);CareerRules.put(record,"mode",board.isSkillMode()?"skills":"classic");CareerRules.put(record,"started",started);CareerRules.put(record,"ended",ended);CareerRules.put(record,"players",participants);CareerRules.put(record,"side",seat);CareerRules.put(record,"result",seat==winner?"win":"loss");if(seat==winner)CareerRules.put(record,"rank",1);CareerRules.put(record,"captures",totals[seat][0]);CareerRules.put(record,"skills",totals[seat][1]);CareerRules.put(records,bindings[seat],record);}CareerRules.put(job,"records",records);settled=true;enqueue(job);
    }
    private JSONArray readJobs()throws Exception{try(InputStream in=journal.openRead();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))>=0){out.write(b,0,n);if(out.size()>3_000_000)throw new IOException("结算日志过大");}return new JSONArray(new String(out.toByteArray(),StandardCharsets.UTF_8));}catch(FileNotFoundException e){return new JSONArray();}}
    private void writeJobs(JSONArray jobs)throws Exception{if(jobs.length()==0){journal.delete();return;}FileOutputStream out=journal.startWrite();try{out.write(jobs.toString().getBytes(StandardCharsets.UTF_8));journal.finishWrite(out);}catch(Exception e){journal.failWrite(out);throw e;}}
    private void drain(JSONArray jobs)throws Exception{while(jobs.length()>0){apply(jobs.getJSONObject(0));JSONArray remaining=new JSONArray();for(int i=1;i<jobs.length();i++)remaining.put(jobs.get(i));writeJobs(remaining);jobs=remaining;}}
    private void enqueue(JSONObject job){worker.execute(()->{try{JSONArray jobs=readJobs();jobs.put(job);writeJobs(jobs);drain(jobs);}catch(Exception e){notify("结算待恢复，本地日志已保留："+e.getMessage());}});}
    private void apply(JSONObject job)throws Exception{JSONArray ids=job.getJSONArray("bindings");int count=0;for(int i=0;i<ids.length();i++){String id=ids.optString(i);if(id.isEmpty())continue;if("retract".equals(job.optString("action"))){store.retract(id,job.getString("match"));continue;}JSONObject before=store.read(id);if(store.settle(id,job.getJSONObject("records").getJSONObject(id))){count++;JSONObject after=store.read(id);int levels=after.optInt("level")-before.optInt("level");StringBuilder text=new StringBuilder(CareerRules.object(after,"profile").optString("nickname")+" · 对局已入档");if(levels>0)text.append(" · 晋升LV.").append(after.optInt("level"));int unlocked=0;for(CareerRules.Achievement a:CareerRules.ACHIEVEMENTS)if(CareerRules.object(CareerRules.object(after,"achievements"),a.id).optBoolean("unlocked")&&!CareerRules.object(CareerRules.object(before,"achievements"),a.id).optBoolean("unlocked"))unlocked++;if(unlocked>0)text.append(" · 解锁").append(unlocked).append("项荣誉");notify(text.toString());}}if(count==0&&"settle".equals(job.optString("action")))notify("对局结束 · 游客不计正式战绩 / 已入档不重复结算");}
    static String fingerprint(String state){try{byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(state.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(java.util.Locale.US,"%02x",b&255));return s.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    private void notify(String s){ui.post(()->notice.show(s));}
    void stop(){matchId="";metadata=null;}
    void close(){worker.shutdown();}
}
