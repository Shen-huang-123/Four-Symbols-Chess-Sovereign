package com.dorm.fourchess;

import android.graphics.*;

/** A stable numbered treaty seal. Six pair IDs stay distinct even with several allies. */
final class AllianceMarkPainter {
    static int forOwner(int mask,int owner){int result=0;for(int other=0;other<4;other++)if(other!=owner)result|=mask&DiplomacyState.pair(owner,other);return result;}
    static int number(int pair){return pair==0?0:Integer.numberOfTrailingZeros(pair)+1;}
    static String label(int pair){return "盟印"+number(pair);}
    static void draw(Canvas canvas,float x,float y,float radius,int pair,Paint paint){
        canvas.save();canvas.translate(x,y);paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(0xff18343e);
        Path seal=new Path();seal.moveTo(-radius,-radius*.8f);seal.lineTo(radius,-radius*.8f);seal.lineTo(radius,radius*.45f);seal.quadTo(radius*.5f,radius*.9f,0,radius);seal.quadTo(-radius*.5f,radius*.9f,-radius,radius*.45f);seal.close();canvas.drawPath(seal,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(.65f,radius*.11f));paint.setColor(0xffe4c98e);canvas.drawPath(seal,paint);paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(radius*1.5f);paint.setColor(0xffffedbb);canvas.drawText(String.valueOf(number(pair)),0,-(paint.ascent()+paint.descent())/2,paint);canvas.restore();
    }
}
