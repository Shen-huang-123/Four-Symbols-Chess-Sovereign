package com.dorm.fourchess;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Four readable character medallions; same player data in one row or a 2-by-2 mobile HUD. */
final class FactionStripView extends View {
    private final android.text.TextPaint paint=new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final CharacterPortraits portraits;
    private int allianceMask;
    private int columns=4;
    private int active,seconds;private long turnChangedAt;
    private final boolean[] alive={true,true,true,true};
    private final String[] treaties={"","","",""};
    private final int[] colors=GameDesign.factionInks();
    private final String[] titles={"红尘弈心","花间一弈","清风入弈","金陵弈梦"};
    private final String[] factions={"南 · 红方","西 · 绿方","北 · 蓝方","东 · 金方"};
    private final String[] directions={"南","西","北","东"};
    FactionStripView(Context context){super(context);portraits=new CharacterPortraits(context);setContentDescription("四方角色与行动状态");}
    void setPlayerNames(String[] names){for(int i=0;i<4;i++)titles[i]=PlayerNames.clean(names[i],PlayerNames.DEFAULTS[i]);updateDescription();invalidate();}
    void setColumns(int value){if(columns!=value){columns=value;invalidate();}}
    void setActivePlayer(int value,int elapsed){if(active!=value)turnChangedAt=android.os.SystemClock.uptimeMillis();active=value;seconds=elapsed;invalidate();}
    void setAlive(int owner,boolean value){alive[owner]=value;invalidate();}
    String playerName(int owner){return titles[owner];}
    void setTreaty(int owner,String value){if(!treaties[owner].equals(value)){treaties[owner]=value;invalidate();}updateDescription();}
    void setAllianceMask(int mask){if(allianceMask!=mask){allianceMask=mask;invalidate();}updateDescription();}
    int marksForOwner(int owner){return alive[owner]?AllianceMarkPainter.forOwner(allianceMask,owner):0;}
    private void updateDescription(){StringBuilder info=new StringBuilder("四方角色与行动状态");for(int i=0;i<4;i++){info.append('，').append(titles[i]).append(' ').append(alive[i]?treaties[i]:"退场");for(int pair=1;pair<=32;pair<<=1)if((marksForOwner(i)&pair)!=0)info.append(' ').append(AllianceMarkPainter.label(pair));}setContentDescription(info);}
    private void fit(String label,float width,float size){paint.setTextSize(size);float measured=paint.measureText(label);if(measured>width&&measured>0)paint.setTextSize(size*width/measured);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float d=getResources().getDisplayMetrics().density;int rows=4/columns;float rowH=getHeight()/(float)rows,gap=4*d,h=rowH-(rows==2?gap:0),cw=(getWidth()-gap*(columns-1))/columns;
        for(int i=0;i<4;i++){
            c.save();c.translate((i%columns)*(cw+gap),(i/columns)*rowH);boolean current=i==active&&alive[i];RectF card=new RectF(d,d,cw-d,h-d);float corner=Math.min(12*d,h*.24f);
            paint.setStyle(Paint.Style.FILL);paint.setShader(new LinearGradient(0,0,cw,h,current?BattleSkin.blend(GameDesign.PANEL_RAISED,colors[i],.26f):GameDesign.PANEL,GameDesign.PANEL_QUIET,Shader.TileMode.CLAMP));c.drawRoundRect(card,corner,corner,paint);paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(current?d:d*.55f);paint.setColor(current?GameDesign.GOLD:GameDesign.EDGE_QUIET);c.drawRoundRect(card,corner,corner,paint);paint.setStyle(Paint.Style.FILL);
            float radius=h*.385f,cx=radius+6*d,cy=h*.5f;RectF face=new RectF(cx-radius,cy-radius,cx+radius,cy+radius);portraits.draw(c,i,face,alive[i]?255:95,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d*.8f);paint.setColor(current?GameDesign.GOLD_BRIGHT:colors[i]);c.drawCircle(cx,cy,radius,paint);paint.setStyle(Paint.Style.FILL);
            if(current){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d*.8f);paint.setColor(0x99efd39a);c.drawArc(face.left-2*d,face.top-2*d,face.right+2*d,face.bottom+2*d,-95,280,false,paint);paint.setStyle(Paint.Style.FILL);}
            float flagX=face.right-d,flagY=h*.77f;paint.setColor(colors[i]);c.drawCircle(flagX,flagY,h*.145f,paint);paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(h*.19f);paint.setColor(GameDesign.TEXT);c.drawText(directions[i],flagX,flagY-(paint.ascent()+paint.descent())/2,paint);
            int seals=marksForOwner(i),sealIndex=0;float sealRadius=Math.min(6*d,radius*.25f);for(int pair=1;pair<=32;pair<<=1)if((seals&pair)!=0){AllianceMarkPainter.draw(c,face.left+sealRadius+sealIndex*(2*sealRadius+d),face.top+sealRadius,sealRadius,pair,paint);sealIndex++;}
            float tx=face.right+8*d,right=cw-8*d,available=right-tx;
            paint.setTextAlign(Paint.Align.LEFT);paint.setTypeface(GameDesign.EMPHASIS);paint.setColor(current?GameDesign.TEXT:GameDesign.SECONDARY);paint.setTextSize(h*.29f);String shown=android.text.TextUtils.ellipsize(titles[i],paint,Math.max(1,available-(current?10*d:0)),android.text.TextUtils.TruncateAt.END).toString();c.drawText(shown,tx,h*.42f,paint);
            if(current){paint.setColor(0xffe6c58b);float markX=right-3*d,markY=h*.30f;Path mark=new Path();mark.moveTo(markX,markY-2.2f*d);mark.lineTo(markX+2.2f*d,markY);mark.lineTo(markX,markY+2.2f*d);mark.lineTo(markX-2.2f*d,markY);mark.close();c.drawPath(mark,paint);}
            paint.setTypeface(Typeface.DEFAULT);paint.setColor(current?GameDesign.GOLD:GameDesign.MUTED);fit(factions[i],available*.51f,h*.205f);c.drawText(factions[i],tx,h*.77f,paint);
            String state=!alive[i]?"退场":current?String.format(java.util.Locale.CHINA,"%02d:%02d",seconds/60,seconds%60):treaties[i].isEmpty()?"候场":treaties[i];
            paint.setTextAlign(Paint.Align.RIGHT);paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setColor(current?GameDesign.GOLD_BRIGHT:GameDesign.MUTED);fit(state,available*.45f,h*.255f);c.drawText(state,right,h*.77f,paint);
            if(current){float t=Math.min(1,(android.os.SystemClock.uptimeMillis()-turnChangedAt)/280f);paint.setColor(colors[i]);c.drawRoundRect(tx,h-3*d,tx+available*(1-(1-t)*(1-t)),h-2*d,d,d,paint);boolean motion=android.os.Build.VERSION.SDK_INT<26||android.animation.ValueAnimator.areAnimatorsEnabled();
                long age=android.os.SystemClock.uptimeMillis()-turnChangedAt;
                if(motion&&age>=0&&age<GameDesign.TURN_MS){float wave=age/(float)GameDesign.TURN_MS;paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d*(.8f+.5f*(float)Math.sin(wave*Math.PI)));paint.setColor(((int)(125*(1-wave))<<24)|0xf3d493);c.drawRoundRect(card,corner,corner,paint);paint.setStyle(Paint.Style.FILL);if(isShown()&&hasWindowFocus())postInvalidateDelayed(32);}
                else if(t<1&&motion&&isShown()&&hasWindowFocus())postInvalidateOnAnimation();}c.restore();
        }
    }
}
