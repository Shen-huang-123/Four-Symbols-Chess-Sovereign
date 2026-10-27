package com.dorm.fourchess;

import android.content.Context;
import android.os.*;
import android.security.keystore.*;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.concurrent.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** HTTPS own-profile API only; GitHub credentials and passwords never enter local storage. */
final class ArchiveCloud {
    interface Result {void done(JSONObject value,String error);}
    private final Context context;private final PlayerArchive archive;private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler ui=new Handler(Looper.getMainLooper());
    private final android.content.SharedPreferences prefs;private boolean syncing,conflict;private int failures;private volatile boolean closed;
    String status="本地档案 · 未配置云端";JSONObject conflictRemote;private String conflictOwner="";
    ArchiveCloud(Context c,PlayerArchive a){context=c;archive=a;prefs=c.getSharedPreferences("cloud_endpoint",0);status=endpoint().isEmpty()?status:"离线缓存 · 等待同步";ui.postDelayed(tick,15000);}
    String endpoint(){return prefs.getString("url","");}
    void endpoint(String s)throws Exception{URI u=new URI(s.trim());if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null)throw new IOException("只接受不含凭据的HTTPS接口地址");android.content.SharedPreferences.Editor update=prefs.edit();if(!endpoint().equals(s.trim().replaceAll("/+$","")))for(String k:prefs.getAll().keySet())if(k.startsWith("session_"))update.remove(k);update.putString("url",s.trim().replaceAll("/+$","")).apply();status="云端地址已设置";}
    private javax.crypto.SecretKey key()throws Exception{KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);if(!store.containsAlias("chess-session-v1")){KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("chess-session-v1",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();}return (javax.crypto.SecretKey)store.getKey("chess-session-v1",null);}
    private void token(String id,String token)throws Exception{if(token.isEmpty()){prefs.edit().remove("session_"+id).apply();return;}Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());String encrypted=Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(c.doFinal(token.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);prefs.edit().putString("session_"+id,encrypted).apply();}
    private String token(String id)throws Exception{String value=prefs.getString("session_"+id,"");if(value.isEmpty())return "";try{String[] s=value.split(":");Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(s[0],Base64.NO_WRAP)));return new String(c.doFinal(Base64.decode(s[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);}catch(Exception e){prefs.edit().remove("session_"+id).apply();throw new IOException("本机会话已失效，请重新登录");}}
    boolean hasSession(String id){return prefs.contains("session_"+id);}
    private static final class ApiError extends IOException {final int code;final JSONObject body;ApiError(int c,JSONObject b){super(b.optString("error","云端请求失败"));code=c;body=b;}}
    private JSONObject request(String method,String path,JSONObject body,String session)throws Exception{
        if(endpoint().isEmpty())throw new IOException("尚未配置云函数，请先设置HTTPS服务地址");
        HttpURLConnection c=(HttpURLConnection)new URL(endpoint()+path).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setRequestMethod(method);c.setRequestProperty("Content-Type","application/json");if(!session.isEmpty())c.setRequestProperty("Authorization","Bearer "+session);
        try{if(body!=null){c.setDoOutput(true);try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}}int code=c.getResponseCode();InputStream input=code>=200&&code<300?c.getInputStream():c.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream();if(input!=null)try(InputStream in=input){byte[] buf=new byte[8192];int n;while((n=in.read(buf))>=0){out.write(buf,0,n);if(out.size()>2_000_000)throw new IOException("云端响应过大");}}JSONObject result=new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));if(code<200||code>=300)throw new ApiError(code,result);return result;}finally{c.disconnect();}
    }
    void authenticate(boolean register,String account,String password,Result callback){
        if(!account.matches("[A-Za-z0-9_]{4,32}")){callback.done(null,"账号为4～32位英文字母、数字或下划线");return;}if(password.length()<10||password.length()>128){callback.done(null,"密码需要10～128个字符");return;}
        submit(()->{JSONObject b=new JSONObject();b.put("account",account);b.put("password",password);JSONObject response=request("POST",register?"/v1/register":"/v1/login",b,"");JSONObject doc=response.getJSONObject("document");PlayerArchive.validate(doc);String id=doc.getString("id");
            try{JSONObject local=archive.read(id);if(local.optBoolean("dirty")){response.put("pendingLocal",local);token(id,response.getString("session"));return response;}}catch(FileNotFoundException ignored){}
            CareerRules.put(doc,"cloudRevision",response.getLong("revision"));CareerRules.put(doc,"dirty",false);archive.write(doc);archive.select(id);token(id,response.getString("session"));return response;},(r,e)->{status=e==null?"已登录 · 本地优先":e;callback.done(r,e);});
    }
    void restoreWithLocal(JSONObject response,boolean localDetails,Result cb){submit(()->{JSONObject remote=response.getJSONObject("document");remote.put("cloudRevision",response.getLong("revision"));JSONObject merged=PlayerArchive.merge(response.getJSONObject("pendingLocal"),remote,localDetails);archive.save(merged);archive.select(merged.getString("id"));return merged;},cb);}
    void sync(Result cb){String id=archive.selected();if(syncing){cb.done(null,"正在同步，请稍候");return;}if(conflict){cb.done(null,"存在修订冲突，请先选择合并方式");return;}syncing=true;
        submit(()->{JSONObject local=archive.read(id);if(local.optBoolean("guest"))throw new IOException("游客默认不进行云备份");String session=token(id);if(session.isEmpty())throw new IOException("此档案未登录云端账号");long revision=local.optLong("localRevision");JSONObject response;
            try{if(local.optBoolean("dirty")){JSONObject b=new JSONObject();b.put("revision",local.optLong("cloudRevision"));b.put("document",local);response=request("PUT","/v1/profile",b,session);}else response=request("GET","/v1/profile",null,session);}
            catch(ApiError e){if(e.code==409){conflictRemote=e.body.getJSONObject("document");conflictRemote.put("cloudRevision",e.body.getLong("revision"));conflictOwner=id;conflict=true;}if(e.code==401)token(id,"");throw e;}
            synchronized(archive){JSONObject latest=archive.read(id);if(latest.optLong("localRevision")!=revision){JSONObject incoming=response.getJSONObject("document");PlayerArchive.validate(incoming);if(!id.equals(incoming.getString("id")))throw new IOException("云端身份不匹配");incoming.put("cloudRevision",response.getLong("revision"));JSONObject merged=PlayerArchive.merge(latest,incoming,true);archive.save(merged);return merged;}JSONObject remote=response.getJSONObject("document");PlayerArchive.validate(remote);if(!id.equals(remote.getString("id")))throw new IOException("云端身份不匹配");remote.put("cloudRevision",response.getLong("revision"));remote.put("localRevision",revision);remote.put("dirty",false);archive.write(remote);return remote;}
        },(r,e)->{syncing=false;if(e==null){failures=0;status="已同步 · "+new java.text.SimpleDateFormat("HH:mm",java.util.Locale.CHINA).format(new java.util.Date());}else{failures=Math.min(5,failures+1);status=(conflict?"修订冲突 · ":"待同步 · ")+e;}cb.done(r,e);});
    }
    void resolve(boolean localDetails,Result cb){final JSONObject remote=conflictRemote;final String id=conflictOwner;if(remote==null){cb.done(null,"没有待处理冲突");return;}submit(()->{JSONObject merged=PlayerArchive.merge(archive.read(id),remote,localDetails);archive.save(merged);return merged;},(r,e)->{if(e==null){conflict=false;conflictRemote=null;status="已合并，等待重新同步";}cb.done(r,e);});}
    void password(String old,String next,Result cb){String id=archive.selected();if(next.length()<10||next.length()>128){cb.done(null,"新密码需要10～128个字符");return;}submit(()->{JSONObject b=new JSONObject();b.put("oldPassword",old);b.put("newPassword",next);JSONObject r=request("POST","/v1/password",b,token(id));token(id,r.getString("session"));return r;},cb);}
    void logout(Result cb){String id=archive.selected();submit(()->{String t=token(id);token(id,"");if(!t.isEmpty())request("POST","/v1/logout",new JSONObject(),t);return new JSONObject();},(r,e)->{status=e==null?"已退出云端会话":"本机会话已移除；离线时服务端会话将在14天内过期";cb.done(r,e);});}
    private interface Task{JSONObject run()throws Exception;}
    private void submit(Task action,Result callback){worker.execute(()->{JSONObject value=null;String error=null;try{value=action.run();}catch(Exception e){error=e.getMessage()==null?"网络或存档操作失败":e.getMessage();}JSONObject v=value;String message=error;ui.post(()->{if(!closed)callback.done(v,message);});});}
    private final Runnable tick=new Runnable(){public void run(){if(closed)return;try{JSONObject d=archive.current();if(d!=null&&d.optBoolean("dirty")&&hasSession(d.optString("id"))&&!conflict&&!syncing)sync((r,e)->{});}catch(Exception ignored){}ui.postDelayed(this,Math.min(300000,15000L*(1L<<failures)));}};
    void close(){closed=true;ui.removeCallbacks(tick);worker.shutdown();}
}
