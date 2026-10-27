package com.dorm.fourchess;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Real window draw cadence; can run unchanged against the previous signed APK for comparison. */
public final class FlipPerformanceProbe extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private void main(Runnable r){runOnMainSync(r);waitForIdleSync();}
    @Override public void onStart(){Bundle out=new Bundle();MainActivity activity=null;
        try{
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            for(int wait=0;wait<100&&!activity.hasWindowFocus();wait++)SystemClock.sleep(50);
            final MainActivity host=activity;final AlertDialog[] dialog={null};final SkillDrawView[] view={null};
            ArrayList<Long> draws=new ArrayList<>();final int[] complete={0};
            main(()->{try{
                java.lang.reflect.Field mf=MainActivity.class.getDeclaredField("modePage");mf.setAccessible(true);ModeSelectView mode=(ModeSelectView)mf.get(host);
                java.lang.reflect.Method sm=ModeSelectView.class.getDeclaredMethod("selectSkills",boolean.class);sm.setAccessible(true);sm.invoke(mode,true);
                java.lang.reflect.Method start=MainActivity.class.getDeclaredMethod("startMatch",String[].class);start.setAccessible(true);start.invoke(host,(Object)new String[]{"红","绿","蓝","金"});
            }catch(Exception e){throw new RuntimeException(e);}});
            for(int wait=0;wait<100;wait++){
                java.lang.reflect.Field vf=MainActivity.class.getDeclaredField("drawView");vf.setAccessible(true);view[0]=(SkillDrawView)vf.get(host);
                if(view[0]!=null&&view[0].hasWindowFocus()&&view[0].getWidth()>0)break;SystemClock.sleep(50);
            }
            main(()->{try{
                java.lang.reflect.Field df=MainActivity.class.getDeclaredField("drawDialog");df.setAccessible(true);dialog[0]=(AlertDialog)df.get(host);
                java.lang.reflect.Field hf=SkillDrawView.class.getDeclaredField("hand");hf.setAccessible(true);System.arraycopy(new int[]{6,9,12,10},0,(int[])hf.get(view[0]),0,4);
                java.lang.reflect.Field cf=SkillDrawView.class.getDeclaredField("charges");cf.setAccessible(true);Arrays.fill((int[])cf.get(view[0]),3);
                view[0].setListener(new SkillDrawView.Listener(){public void onCard(int id,boolean visible){}public void onComplete(){complete[0]++;}});view[0].invalidate();
            }catch(Exception e){throw new RuntimeException(e);}});            SystemClock.sleep(500);ViewTreeObserver.OnDrawListener listener=()->draws.add(SystemClock.elapsedRealtimeNanos());
            main(()->{view[0].getViewTreeObserver().addOnDrawListener(listener);view[0].revealAll(true);});SystemClock.sleep(1800);
            main(()->view[0].getViewTreeObserver().removeOnDrawListener(listener));
            ArrayList<Double> gaps=new ArrayList<>();for(int i=1;i<draws.size();i++)gaps.add((draws.get(i)-draws.get(i-1))/1000000d);Collections.sort(gaps);
            if(gaps.isEmpty()||!view[0].allRevealed()||complete[0]!=1)throw new AssertionError("real four-card flip did not complete exactly once: draws="+draws.size()+" complete="+complete[0]+" revealed="+view[0].allRevealed()+" shown="+view[0].isShown()+" focus="+view[0].hasWindowFocus());
            String version=getTargetContext().getPackageManager().getPackageInfo(getTargetContext().getPackageName(),0).versionName;
            String report=String.format(Locale.US,"PERF version=%s frames=%d median_gap_ms=%.2f p90_gap_ms=%.2f refresh_hz=%.1f; emulator window cadence, not Honor WIN FPS\nPASS real staggered four-legend flip completes once\nPERF COMPLETE\n",version,draws.size(),gaps.get(gaps.size()/2),gaps.get((int)((gaps.size()-1)*.9)),host.getWindowManager().getDefaultDisplay().getRefreshRate());
            main(()->dialog[0].dismiss());out.putString("stream",report);finish(Activity.RESULT_OK,out);
        }catch(Throwable e){out.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);}
        finally{if(activity!=null){MainActivity host=activity;main(host::finish);}}
    }
}
