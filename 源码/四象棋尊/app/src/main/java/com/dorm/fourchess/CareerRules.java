package com.dorm.fourchess;

import org.json.*;
import java.util.*;

/** Pure progression rules. Never grants competitive abilities or changes board state. */
final class CareerRules {
    static final String[] HEROES={"红尘弈心","花间一弈","清风入弈","金陵弈梦"};
    static final String[] TITLES={"初入棋道","青铜棋士","白银棋士","黄金棋士","弈道高手","四方名将","棋道宗师","纵横棋圣","天命棋尊","四象至尊","万象棋神"};
    static int need(int level){return 80+level*20;}
    static int level(long xp){int level=1;while(level<100&&xp>=need(level)){xp-=need(level++);}return level;}
    static long progress(long xp){for(int n=1;n<100&&xp>=need(n);n++)xp-=need(n);return xp;}
    static int bondLevel(long xp){int l=1;while(l<20&&xp>=40+l*15){xp-=40+l++*15;}return l;}
    static String title(int level){return TITLES[level==100?10:level/10];}
    static String friendship(int xp){int[] floors={50,150,400,800,1500,3000};String[] names={"初识棋友","棋逢对手","以棋会友","莫逆之交","生死棋交","四象知己","纵横双绝"};int i=0;while(i<floors.length&&xp>=floors[i])i++;return names[i];}
    static final class Achievement {
        final String id,name,category,quality,metric;final int target,bonus;
        Achievement(String id,String n,String c,String q,String m,int t,int b){this.id=id;name=n;category=c;quality=q;metric=m;target=t;bonus=b;}
    }
    static final Achievement[] ACHIEVEMENTS={
        a("first","初入棋局","对局","普通","games",1,10),a("ten","十局历练","对局","普通","games",10,20),a("hundred","百战棋士","对局","稀有","games",100,50),a("thousand","千局问道","对局","传说","games",1000,100),
        a("win","首战告捷","棋艺","普通","wins",1,20),a("win10","十胜初成","棋艺","稀有","wins",10,30),a("win50","常胜将军","棋艺","史诗","wins",50,60),a("streak3","三连破阵","棋艺","稀有","best",3,30),a("streak10","十连绝胜","棋艺","传说","best",10,100),
        a("capture","锋芒初现","棋艺","普通","captures",1,10),a("capture50","破阵五十","棋艺","稀有","captures",50,30),a("capture500","横扫四方","棋艺","史诗","captures",500,60),
        a("skill","初识秘术","秘术","普通","skills",1,10),a("skill20","符箓学徒","秘术","稀有","skills",20,30),a("skill100","秘术大师","秘术","史诗","skills",100,60),a("secret10","秘卷常客","秘术","稀有","secret",10,30),
        a("friend","以棋会友","社交","普通","friendGames",1,20),a("friend10","十局知音","社交","稀有","friendGames",10,30),a("close","莫逆之交","社交","史诗","intimacy",400,50),a("friends4","四海棋友","社交","稀有","friends",4,20),
        a("sides","四象皆通","隐藏","史诗","sides",4,50),a("classic","古法十胜","对局","稀有","classicWins",10,30),a("hours","弈路漫漫","对局","稀有","minutes",600,30),a("bond","心意相通","隐藏","传说","bondMax",20,100)
    };
    private static Achievement a(String id,String n,String c,String q,String m,int t,int b){return new Achievement(id,n,c,q,m,t,b);}
    static JSONObject object(JSONObject o,String k){JSONObject v=o.optJSONObject(k);return v==null?new JSONObject():v;}
    static JSONArray array(JSONObject o,String k){JSONArray a=o.optJSONArray(k);return a==null?new JSONArray():a;}
    static void put(JSONObject o,String k,Object v){try{o.put(k,v);}catch(JSONException e){throw new IllegalArgumentException(e);}}
    static JSONObject copy(JSONObject o){try{return new JSONObject(o.toString());}catch(JSONException e){throw new IllegalArgumentException(e);}}
    static ArrayList<JSONObject> orderedMatches(JSONObject doc){ArrayList<JSONObject> list=new ArrayList<>();JSONObject rows=object(doc,"matches");Iterator<String> keys=rows.keys();while(keys.hasNext()){JSONObject m=rows.optJSONObject(keys.next());if(m!=null)list.add(m);}Collections.sort(list,(a,b)->{int c=Long.compare(a.optLong("ended"),b.optLong("ended"));return c!=0?c:a.optString("id").compareTo(b.optString("id"));});return list;}
    private static JSONArray numbers(int[] values){JSONArray a=new JSONArray();for(int v:values)a.put(v);return a;}
    static JSONObject derive(JSONObject doc){
        JSONObject s=new JSONObject(),heroes=new JSONObject(),friends=object(doc,"friends"),friendStats=new JSONObject();long xp=0;int streak=0,best=0,wins=0,captures=0,skills=0,secret=0,friendGames=0,classicWins=0,minutes=0;int[] gamesSide=new int[4],winsSide=new int[4];int[] modes=new int[4];long[] bond=new long[4];ArrayList<JSONObject> matches=orderedMatches(doc);
        for(JSONObject m:matches){int side=m.optInt("side",-1);if(side<0||side>3)continue;boolean win="win".equals(m.optString("result"));xp+=win?100:30;gamesSide[side]++;bond[side]+=win?30:20;if(win){wins++;winsSide[side]++;best=Math.max(best,++streak);}else streak=0;captures+=m.optInt("captures");skills+=m.optInt("skills");boolean magic="skills".equals(m.optString("mode"));if(magic)secret++;if(win&&!magic)classicWins++;modes[magic?2:0]++;if(win)modes[magic?3:1]++;minutes+=Math.max(0,(m.optLong("ended")-m.optLong("started"))/60000);
            boolean friendMatch=false;JSONArray players=array(m,"players");for(int i=0;i<players.length();i++){String id=players.optString(i);JSONObject f=friends.optJSONObject(id);if(f==null||m.optLong("ended")<f.optLong("since"))continue;JSONObject fs=object(friendStats,id);int n=fs.optInt("games")+1;put(fs,"games",n);put(fs,"last",m.optLong("ended"));put(fs,"intimacy",n*10+20+(n>=10?30:0));put(friendStats,id,fs);friendMatch=true;}if(friendMatch)friendGames++;
        }
        int sides=0,maxBond=1,maxIntimacy=0;for(int i=0;i<4;i++){if(gamesSide[i]>0)sides++;JSONObject h=new JSONObject();put(h,"xp",bond[i]);put(h,"level",bondLevel(bond[i]));put(heroes,""+i,h);maxBond=Math.max(maxBond,bondLevel(bond[i]));}
        Iterator<String> fk=friendStats.keys();while(fk.hasNext())maxIntimacy=Math.max(maxIntimacy,object(friendStats,fk.next()).optInt("intimacy"));
        String[] metrics={"games","wins","best","captures","skills","secret","friendGames","intimacy","friends","sides","classicWins","minutes","bondMax"};int[] values={matches.size(),wins,best,captures,skills,secret,friendGames,maxIntimacy,friends.length(),sides,classicWins,minutes,maxBond};for(int i=0;i<metrics.length;i++)put(s,metrics[i],values[i]);put(s,"streak",streak);put(s,"losses",matches.size()-wins);put(s,"draws",0);put(s,"sideGames",numbers(gamesSide));put(s,"sideWins",numbers(winsSide));put(s,"modeTotals",numbers(modes));
        JSONObject ach=new JSONObject();JSONArray unlocked=new JSONArray();for(String item:new String[]{"avatar:0","avatar:1","avatar:2","avatar:3","frame:plain","wall:night","card:night","board:ivory","piece:ivory","effect:quiet","title:auto"})unlocked.put(item);
        JSONObject previous=object(doc,"achievements");for(Achievement a:ACHIEVEMENTS){int progress=Math.min(a.target,s.optInt(a.metric));JSONObject v=new JSONObject();boolean done=progress>=a.target;put(v,"progress",progress);put(v,"unlocked",done);if(done){long when=object(previous,a.id).optLong("at",System.currentTimeMillis());put(v,"at",when);xp+=a.bonus;unlocked.put("badge:"+a.id);if("传说".equals(a.quality))unlocked.put("frame:legend");}put(ach,a.id,v);}
        int level=level(xp);if(level>=10){unlocked.put("frame:gold");unlocked.put("board:jade");}if(level>=20){unlocked.put("piece:jade");unlocked.put("effect:ink");}if(maxIntimacy>=400){unlocked.put("card:jade");unlocked.put("frame:jade");}for(int i=0;i<4;i++)if(bondLevel(bond[i])>=2){unlocked.put("wall:hero"+i);unlocked.put("title:hero"+i);}put(doc,"statistics",s);put(doc,"characters",heroes);put(doc,"friendStats",friendStats);put(doc,"achievements",ach);put(doc,"unlocked",unlocked);put(doc,"xp",xp);put(doc,"level",level);
        JSONObject milestones=new JSONObject(),old=object(doc,"milestones");for(int l=10;l<=100;l+=10)if(level>=l)milestone(milestones,old,"level"+l,"晋升LV."+l+" · "+title(l));for(int i=0;i<4;i++)if(bondLevel(bond[i])==20)milestone(milestones,old,"bond"+i,HEROES[i]+" · 羁绊圆满");if(maxIntimacy>=400)milestone(milestones,old,"closeFriend","棋友羁绊 · 莫逆之交");put(doc,"milestones",milestones);return doc;
    }
    private static void milestone(JSONObject out,JSONObject previous,String id,String label){JSONObject v=new JSONObject();put(v,"at",object(previous,id).optLong("at",System.currentTimeMillis()));put(v,"label",label);put(out,id,v);}
    static boolean owns(JSONObject doc,String item){JSONArray a=array(doc,"unlocked");for(int i=0;i<a.length();i++)if(item.equals(a.optString(i)))return true;return false;}
}
