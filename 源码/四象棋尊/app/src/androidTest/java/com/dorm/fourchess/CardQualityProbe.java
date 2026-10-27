package com.dorm.fourchess;

import android.app.*;
import android.os.*;
import android.graphics.*;
import android.view.View;
import java.io.*;

/** Renders every quality through the same production painter, including compact/disabled states. */
public final class CardQualityProbe extends Instrumentation {
    private final StringBuilder log=new StringBuilder();
    private void check(boolean value,String label){if(!value)throw new AssertionError(label);log.append("PASS ").append(label).append('\n');}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private double luminance(int color){double[] rgb={Color.red(color)/255d,Color.green(color)/255d,Color.blue(color)/255d};for(int i=0;i<3;i++)rgb[i]=rgb[i]<=.04045?rgb[i]/12.92:Math.pow((rgb[i]+.055)/1.055,2.4);return rgb[0]*.2126+rgb[1]*.7152+rgb[2]*.0722;}
    private double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    private int dominant(Bitmap b,RectF r,int which){int count=0,total=0;for(int y=(int)r.top+6;y<r.bottom-6;y+=2)for(int x=(int)r.left+6;x<r.right-6;x+=2){int color=b.getPixel(x,y),red=Color.red(color),green=Color.green(color),blue=Color.blue(color);total++;if(which==0?red>green*1.5&&red>blue*1.4:which==1?blue>red*1.35&&blue>green:green>red*1.3&&green>blue)count++;}return count*100/Math.max(1,total);}
    private void renderSheet(int widthDp,int heightDp,boolean compact,String name)throws Exception{
        float d=getTargetContext().getResources().getDisplayMetrics().density;
        int w=Math.round(widthDp*d),h=Math.round(heightDp*d);Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(GameDesign.NIGHT);SkillCardPainter painter=new SkillCardPainter(getTargetContext());
        int[] ids={6,7,1,29},charges={3,2,1,0};RectF[] cells=new RectF[4];float gap=8*d;
        for(int i=0;i<4;i++){float cw=(w-gap*3)/2,ch=(h-gap*3)/2,left=gap+(i%2)*(cw+gap),top=gap+(i/2)*(ch+gap);cells[i]=new RectF(left,top,left+cw,top+ch);painter.draw(c,cells[i],ids[i],charges[i],false,i==3,i==3?"已用尽":"",compact,0);check(cells[i].width()>0&&cells[i].height()>0,"responsive quality card geometry "+name+"/"+i);}
        check(dominant(b,cells[0],0)>65,"legendary face is visibly red "+name);check(dominant(b,cells[1],1)>65,"rare face is visibly blue "+name);check(dominant(b,cells[2],2)>65,"common face is visibly green "+name);
        for(int i=0;i<3;i++){RectF r=cells[i];int upper=b.getPixel((int)(r.left+3*d),(int)(r.top+r.height()*.28f)),lower=b.getPixel((int)(r.right-3*d),(int)(r.top+r.height()*.88f));check(luminance(upper)-luminance(lower)>.002,"reading panel separated from emblem field "+name+"/"+i);}
        try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getFilesDir(),name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
    }
    private void run()throws Exception{
        verifyEffects();
        verifyRevealPhases();
        int[] totals=new int[3];for(int id=1;id<=30;id++){SkillCardTheme t=SkillCardTheme.forSkill(id);check(t!=null,"all skill IDs have a shared quality theme "+id);totals[t.pattern]++;}
        check(totals[0]==6&&totals[1]==10&&totals[2]==14,"mapping retains original six legendary ten rare fourteen common skills");check(SkillCardTheme.forSkill(29)==SkillCardTheme.COMMON&&SkillCardTheme.label(29).contains("整活"),"weak entertainment identity retained");
        for(SkillCardTheme t:new SkillCardTheme[]{SkillCardTheme.LEGENDARY,SkillCardTheme.RARE,SkillCardTheme.COMMON})for(int bg:new int[]{t.deep,t.middle,t.light}){check(contrast(t.title,bg)>=4.5,"title contrast >=4.5 "+t.key);check(contrast(t.body,bg)>=4.5,"description contrast >=4.5 "+t.key);}
        SkillCardPainter copyPainter=new SkillCardPainter(getTargetContext());float density=getTargetContext().getResources().getDisplayMetrics().density;
        for(int width:new int[]{146,192,320}){
            float w=width*density,h=copyPainter.minimumCardHeight(w);RectF r=new RectF(0,0,w,h),art=copyPainter.emblemBounds(r);
            check(art.width()/w>=.45f&&art.width()/w<=.55f,"readable manuscript reserves 45-55 percent width for totem "+width);
            for(int id=1;id<=30;id++){android.text.StaticLayout lines=copyPainter.summary(SkillCardCopy.summary(id),w-Math.min(10*density,w*.055f)*2,h*.26f-12*density,11*getTargetContext().getResources().getDisplayMetrics().scaledDensity,SkillCardTheme.forSkill(id).body);check(lines.getLineCount()<=2&&lines.getPaint().getTextSize()>=9*density,"actual card summary remains complete and legible "+id+"/"+width);}
        }
        check(SkillDrawView.REVEAL_MS>=500&&SkillDrawView.REVEAL_MS<=800,"three-stage reveal duration within requested range");
        check(360+SkillCardEffects.duration(6,false)<=800,"highest quality reveal settles within 800ms");
        for(int id=1;id<=30;id++)for(int width:new int[]{146,192,320}){
            String copy=SkillRules.DETAILS[id-1];android.text.StaticLayout lines=copyPainter.description(copy,width*density,36*density,11*density,SkillCardTheme.forSkill(id).body);
            check(lines.getText().toString().equals(copy)&&lines.getLineEnd(lines.getLineCount()-1)==copy.length(),"complete unabridged description "+id+"/"+width);
            check(lines.getHeight()<=36*density+1,"wrapped description fits allotted height "+id+"/"+width);
        }
        java.util.HashSet<Integer> glyphHashes=new java.util.HashSet<>();
        Bitmap atlas=Bitmap.createBitmap(Math.round(600*density),Math.round(700*density),Bitmap.Config.ARGB_8888);Canvas gallery=new Canvas(atlas);gallery.drawColor(0xff152630);
        for(int id=1;id<=30;id++){
            String copy=SkillCardCopy.summary(id);check(!copy.contains("…")&&!copy.isEmpty(),"semantic summary without truncation "+id);
            for(int width:new int[]{146,192,320}){android.text.StaticLayout lines=copyPainter.summary(copy,width*density,32*density,11*density,SkillCardTheme.forSkill(id).body);check(lines.getLineCount()<=2&&lines.getText().toString().equals(copy),"summary fully visible in at most two lines "+id+"/"+width);check(lines.getPaint().getTextSize()>=7.5*density,"summary readable at real narrow-card width "+id+"/"+width);}
            Bitmap glyph=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);new SkillEmblemPainter().draw(new Canvas(glyph),new RectF(8,8,184,184),id,SkillCardTheme.COMMON,false);int[] pixels=new int[192*192];glyph.getPixels(pixels,0,192,0,0,192,192);check(glyphHashes.add(java.util.Arrays.hashCode(pixels)),"unique semantic seal even with identical quality frame "+id);check(glyph.getPixel(0,0)==0&&glyph.getPixel(191,191)==0,"emblem stays inside drawing bounds "+id);glyph.recycle();
            int col=(id-1)%5,row=(id-1)/5;RectF cell=new RectF((col*120+16)*density,(row*112+9)*density,(col*120+104)*density,(row*112+97)*density);new SkillEmblemPainter().draw(gallery,cell,id,SkillCardTheme.forSkill(id),false);
            android.graphics.Paint label=new android.graphics.Paint(3);label.setColor(0xffede3cc);label.setTextSize(11*density);label.setTextAlign(android.graphics.Paint.Align.CENTER);gallery.drawText(SkillRules.name(id),cell.centerX(),(row*112+108)*density,label);
        }
        try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getFilesDir(),"quality-seals.png"))){atlas.compress(Bitmap.CompressFormat.PNG,100,out);}atlas.recycle();
        renderSheet(420,580,false,"quality-full.png");renderSheet(360,130,true,"quality-compact.png");
        SkillDrawView draw=new SkillDrawView(getTargetContext(),new int[]{6,7,1,29},new int[]{3,2,1,0});float d=getTargetContext().getResources().getDisplayMetrics().density;int w=Math.round(420*d),h=Math.round(390*d);draw.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));draw.layout(0,0,w,h);draw.revealAll(false);
        check(draw.allRevealed(),"skip preserves fully revealed card presentation");String node=draw.getAccessibilityNodeProvider().createAccessibilityNodeInfo(1).getContentDescription().toString();check(node.contains("传说")&&node.contains("剩余3次")&&node.contains("天遁"),"revealed quality name and actual charges accessible");check(draw.getAccessibilityNodeProvider().createAccessibilityNodeInfo(2).getContentDescription().toString().contains("稀有"),"rare presentation uses shared label");
        Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);draw.draw(new Canvas(b));try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getFilesDir(),"quality-reveal.png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
    }
    private void verifyRevealPhases()throws Exception{
        float d=getTargetContext().getResources().getDisplayMetrics().density;int w=Math.round(360*d),h=Math.round(490*d);
        SkillDrawView v=new SkillDrawView(getTargetContext(),new int[]{9,30,16,2},new int[]{3,2,1,2});
        v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h);
        java.lang.reflect.Field flipField=SkillDrawView.class.getDeclaredField("flips");flipField.setAccessible(true);long[] flips=(long[])flipField.get(v);
        final int[] completed={0};v.setListener(new SkillDrawView.Listener(){public void onCard(int id,boolean visible){}public void onComplete(){completed[0]++;}});
        java.util.HashSet<Integer> hashes=new java.util.HashSet<>();Bitmap atlas=Bitmap.createBitmap(w*3,h,Bitmap.Config.ARGB_8888);Canvas out=new Canvas(atlas);
        for(int i=0;i<3;i++){
            flips[0]=SystemClock.uptimeMillis()-new int[]{90,400,650}[i];Bitmap frame=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);v.draw(new Canvas(frame));
            int[] pixels=new int[w*h];frame.getPixels(pixels,0,w,0,0,w,h);check(hashes.add(java.util.Arrays.hashCode(pixels)),"opening flip and quality-settle frames differ "+i);
            String accessible=v.getAccessibilityNodeProvider().createAccessibilityNodeInfo(1).getContentDescription().toString();
            check(i==0?accessible.contains("点击揭晓")&&!accessible.contains("传说"):accessible.contains("穿云")&&accessible.contains("剩余3次"),"sealed identity concealed until face turns "+i);
            out.drawBitmap(frame,i*w,0,null);frame.recycle();
        }
        v.revealAll(false);check(v.allRevealed()&&completed[0]==1,"skip during active flip immediately completes exactly once");v.revealAll(false);check(completed[0]==1,"repeated skip never duplicates completion callback");
        try(FileOutputStream file=new FileOutputStream(new File(getTargetContext().getFilesDir(),"quality-phases.png"))){atlas.compress(Bitmap.CompressFormat.PNG,100,file);}atlas.recycle();
    }
    private void verifyEffects()throws Exception{
        SkillCardEffects fx=new SkillCardEffects();SkillCardPainter painter=new SkillCardPainter(getTargetContext());
        check(SkillCardEffects.duration(6,false)>SkillCardEffects.duration(7,false)&&SkillCardEffects.duration(7,false)>SkillCardEffects.duration(1,false),"reveal quality has progressively richer finite duration");
        check(SkillCardEffects.duration(6,true)>SkillCardEffects.duration(7,true)&&SkillCardEffects.duration(7,true)>SkillCardEffects.duration(1,true),"selection quality has progressively richer finite duration");
        float d=getTargetContext().getResources().getDisplayMetrics().density;
        Bitmap sheet=Bitmap.createBitmap(Math.round(600*d),Math.round(540*d),Bitmap.Config.ARGB_8888);Canvas gallery=new Canvas(sheet);gallery.drawColor(GameDesign.NIGHT);
        for(int row=0;row<3;row++){
            int id=new int[]{1,7,6}[row];java.util.HashSet<Integer> hashes=new java.util.HashSet<>();
            for(int frame=0;frame<3;frame++){
                int w=192,h=240;RectF r=new RectF(2,2,w-2,h-2),field=painter.emblemBounds(r);
                Bitmap overlay=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas layer=new Canvas(overlay);
                long age=Math.round(SkillCardEffects.duration(id,false)*new float[]{.2f,.5f,.8f}[frame]);fx.draw(layer,field,id,age,false);
                int[] pixels=new int[w*h];overlay.getPixels(pixels,0,w,0,0,w,h);check(hashes.add(java.util.Arrays.hashCode(pixels)),"effect changes between reveal frames "+id+"/"+frame);
                boolean clean=true;int lit=0;for(int y=0;y<h;y++)for(int x=0;x<w;x++){if(Color.alpha(pixels[y*w+x])>0){lit++;if(x<Math.floor(field.left)||x>=Math.ceil(field.right)||y<Math.floor(field.top)||y>=Math.ceil(field.bottom))clean=false;}}
                check(clean&&lit>0,"effect stays inside illustration and never covers copy "+id+"/"+frame);overlay.recycle();
                RectF cell=new RectF((frame*200+5)*d,(row*180+5)*d,(frame*200+195)*d,(row*180+175)*d);painter.draw(gallery,cell,id,2,true,false,"",false,0);fx.draw(gallery,painter.emblemBounds(cell),id,age,false);
            }
            for(boolean select:new boolean[]{false,true}){
                check(!SkillCardEffects.active(id,-1,select)&&!SkillCardEffects.active(id,SkillCardEffects.duration(id,select),select),"no default or expired animation "+id+"/"+select);
                Bitmap empty=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);fx.draw(new Canvas(empty),new RectF(10,10,90,90),id,SkillCardEffects.duration(id,select),select);int[] pixels=new int[10000];empty.getPixels(pixels,0,100,0,0,100,100);boolean clear=true;for(int pixel:pixels)if(pixel!=0)clear=false;check(clear,"expired effect draws nothing "+id+"/"+select);empty.recycle();
            }
        }
        try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getFilesDir(),"quality-effects.png"))){sheet.compress(Bitmap.CompressFormat.PNG,100,out);}sheet.recycle();
    }
    @Override public void onStart(){Bundle result=new Bundle();try{final Throwable[] error={null};runOnMainSync(()->{try{run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError(error[0]);log.append("QUALITY COMPLETE\n");result.putString("stream",log.toString());finish(Activity.RESULT_OK,result);}catch(Throwable e){result.putString("stream",log+"FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
