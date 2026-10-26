package com.dorm.fourchess;

import android.content.Context;
import android.graphics.*;
import java.lang.ref.WeakReference;

/** Reuses original character artwork and the same square portrait crop across HUD and notices. */
final class CharacterPortraits {
    private static WeakReference<Bitmap> cache=new WeakReference<>(null);
    private final Bitmap sheet;
    private static final Rect[] CARDS={new Rect(8,0,260,270),new Rect(266,0,524,270),new Rect(532,0,785,270),new Rect(792,0,1057,270)};
    CharacterPortraits(Context context){Bitmap value=cache.get();if(value==null||value.isRecycled()){value=BitmapFactory.decodeResource(context.getResources(),R.drawable.ui_reference);cache=new WeakReference<>(value);}sheet=value;}
    void draw(Canvas canvas,int owner,RectF face,int alpha,Paint paint){
        Rect source=CARDS[owner];Rect crop=new Rect(source.left+(int)(source.width()*.14f),source.top+(int)(source.height()*.02f),source.right-(int)(source.width()*.02f),source.bottom-(int)(source.height()*.12f));int side=Math.min(crop.width(),crop.height());crop.left+=(crop.width()-side)/2;crop.top+=(crop.height()-side)/2;crop.right=crop.left+side;crop.bottom=crop.top+side;
        Path clip=new Path();clip.addOval(face,Path.Direction.CW);canvas.save();canvas.clipPath(clip);paint.setShader(null);paint.setAlpha(alpha);canvas.drawBitmap(sheet,crop,face,paint);paint.setAlpha(255);canvas.restore();
    }
}
