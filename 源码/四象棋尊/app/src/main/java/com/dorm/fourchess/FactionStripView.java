package com.dorm.fourchess;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Four readable character medallions; same player data in one row or a 2-by-2 mobile HUD. */
final class FactionStripView extends View {
    private final android.text.TextPaint paint=new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Bitmap sheet;
    private final Rect[] cards={new Rect(8,0,260,270),new Rect(266,0,524,270),new Rect(532,0,785,270),new Rect(792,0,1057,270)};
    private int columns=4;
    private int active,seconds;private long turnChangedAt;
    private final boolean[] alive={true,true,true,true};
    private final int[] colors={0xffbf5549,0xff4b9774,0xff5c9cbd,0xffba9452};
    private final String[] titles={"红尘弈心","花间一弈","清风入弈","金陵弈梦"};
    private final String[] factions={"南 · 红方","西 · 绿方","北 · 蓝方","东 · 金方"};
    private final String[] directions={"南","西","北","东"};
    FactionStripView(Context context){super(context);sheet=BitmapFactory.decodeResource(getResources(),R.drawable.ui_reference);setContentDescription("四方角色与行动状态");}
    void setPlayerNames(String[] names){for(int i=0;i<4;i++)titles[i]=PlayerNames.clean(names[i],PlayerNames.DEFAULTS[i]);invalidate();}
    void setColumns(int value){if(columns!=value){columns=value;invalidate();}}
    void setActivePlayer(int value,int elapsed){if(active!=value)turnChangedAt=android.os.SystemClock.uptimeMillis();active=value;seconds=elapsed;invalidate();}
    void setAlive(int owner,boolean value){alive[owner]=value;invalidate();}
    private void fit(String label,float width,float size){paint.setTextSize(size);float measured=paint.measureText(label);if(measured>width&&measured>0)paint.setTextSize(size*width/measured);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float d=getResources().getDisplayMetrics().density;int rows=4/columns;float rowH=getHeight()/(float)rows,gap=4*d,h=rowH-(rows==2?gap:0),cw=(getWidth()-gap*(columns-1))/columns;
        for(int i=0;i<4;i++){
            c.save();c.translate((i%columns)*(cw+gap),(i/columns)*rowH);boolean current=i==active&&alive[i];RectF card=new RectF(d,d,cw-d,h-d);
            paint.setStyle(Paint.Style.FILL);paint.setShader(new LinearGradient(0,0,cw,h,current?BattleSkin.blend(0xff263442,colors[i],.32f):0xd51b2a36,0xc0182834,Shader.TileMode.CLAMP));c.drawRoundRect(card,h*.25f,h*.25f,paint);paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(current?d:d*.55f);paint.setColor(current?0xffd6b978:0x997a837e);c.drawRoundRect(card,h*.25f,h*.25f,paint);paint.setStyle(Paint.Style.FILL);
            float radius=h*.405f,cx=radius+4*d,cy=h*.5f;RectF face=new RectF(cx-radius,cy-radius,cx+radius,cy+radius);Rect source=cards[i];Rect crop=new Rect(source.left+(int)(source.width()*.14f),source.top+(int)(source.height()*.02f),source.right-(int)(source.width()*.02f),source.bottom-(int)(source.height()*.12f));
            int side=Math.min(crop.width(),crop.height());crop.left+=(crop.width()-side)/2;crop.top+=(crop.height()-side)/2;crop.right=crop.left+side;crop.bottom=crop.top+side;
            Path clip=new Path();clip.addCircle(cx,cy,radius,Path.Direction.CW);c.save();c.clipPath(clip);paint.setAlpha(alive[i]?255:95);c.drawBitmap(sheet,crop,face,paint);paint.setAlpha(255);c.restore();
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d*.8f);paint.setColor(current?0xffefd39a:colors[i]);c.drawCircle(cx,cy,radius,paint);paint.setStyle(Paint.Style.FILL);
            float flagX=face.right-d,flagY=h*.77f;paint.setColor(colors[i]);c.drawCircle(flagX,flagY,h*.145f,paint);paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(h*.19f);paint.setColor(0xfff8edda);c.drawText(directions[i],flagX,flagY-(paint.ascent()+paint.descent())/2,paint);
            float tx=face.right+8*d,right=cw-8*d,available=right-tx;
            paint.setTextAlign(Paint.Align.LEFT);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));paint.setColor(current?0xfff8e8c8:0xffdddcd2);paint.setTextSize(h*.29f);String shown=android.text.TextUtils.ellipsize(titles[i],paint,available,android.text.TextUtils.TruncateAt.END).toString();c.drawText(shown,tx,h*.42f,paint);
            paint.setTypeface(Typeface.DEFAULT);paint.setColor(current?0xffc8b89c:0xff9aafb4);fit(factions[i],available*.51f,h*.205f);c.drawText(factions[i],tx,h*.77f,paint);
            String state=!alive[i]?"退场":current?String.format(java.util.Locale.CHINA,"%02d:%02d",seconds/60,seconds%60):"候场";
            paint.setTextAlign(Paint.Align.RIGHT);paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setColor(current?0xfff2cf8e:0xff91a4a7);fit(state,available*.45f,h*.255f);c.drawText(state,right,h*.77f,paint);
            if(current){float t=Math.min(1,(android.os.SystemClock.uptimeMillis()-turnChangedAt)/280f);paint.setColor(colors[i]);c.drawRoundRect(tx,h-3*d,tx+available*(1-(1-t)*(1-t)),h-2*d,d,d,paint);if(t<1)postInvalidateOnAnimation();}c.restore();
        }
    }
}
