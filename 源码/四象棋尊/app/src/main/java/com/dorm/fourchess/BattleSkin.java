package com.dorm.fourchess;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Original code-drawn lacquer, bronze frames and scenery. */
final class BattleSkin {
    static int blend(int a,int b,float t){return Color.rgb((int)(Color.red(a)*(1-t)+Color.red(b)*t),(int)(Color.green(a)*(1-t)+Color.green(b)*t),(int)(Color.blue(a)*(1-t)+Color.blue(b)*t));}
    static Path cut(RectF r,float corner){
        float n=Math.min(corner,Math.min(r.width(),r.height())*.24f);Path path=new Path();
        path.moveTo(r.left+n,r.top);path.lineTo(r.right-n,r.top);path.lineTo(r.right,r.top+n);path.lineTo(r.right,r.bottom-n);
        path.lineTo(r.right-n,r.bottom);path.lineTo(r.left+n,r.bottom);path.lineTo(r.left,r.bottom-n);path.lineTo(r.left,r.top+n);path.close();return path;
    }
    static final class Frame extends Drawable {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int base,edge;private final float unit;private final boolean ornament;
        private int alpha=255;private boolean enabled=true,pressed;
        Frame(int base,int edge,float density,boolean ornament){this.base=base;this.edge=edge;unit=density;this.ornament=ornament;}
        @Override public boolean isStateful(){return true;}
        @Override protected boolean onStateChange(int[] state){boolean nextEnabled=false,nextPressed=false;for(int s:state){if(s==android.R.attr.state_enabled)nextEnabled=true;if(s==android.R.attr.state_pressed||s==android.R.attr.state_focused)nextPressed=true;}boolean changed=enabled!=nextEnabled||pressed!=nextPressed;enabled=nextEnabled;pressed=nextPressed;if(changed)invalidateSelf();return changed;}
        @Override public void draw(Canvas canvas){
            Rect b=getBounds();if(b.isEmpty())return;float u=unit;
            RectF outer=new RectF(b.left+u,b.top+u,b.right-u,b.bottom-u);Path shape=cut(outer,7*u);
            int fill=!enabled?blend(base,0xff1d201e,.6f):pressed?blend(base,0xff0d1110,.25f):base;
            paint.setStyle(Paint.Style.FILL);paint.setShader(new LinearGradient(0,b.top,0,b.bottom,blend(fill,0xffc5ae77,.045f),blend(fill,0xff060908,.13f),Shader.TileMode.CLAMP));paint.setAlpha(alpha);canvas.drawPath(shape,paint);paint.setShader(null);
            canvas.save();canvas.clipPath(shape);paint.setColor(0x075e674f);paint.setAlpha(alpha*7/255);paint.setStrokeWidth(u*.5f);
            for(float y=b.top+5*u;y<b.bottom;y+=9*u)canvas.drawLine(b.left,y,b.right,y,paint);canvas.restore();
            int border=!enabled?0xff4b493b:pressed?0xffdbc081:edge;
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(u);paint.setColor(border);paint.setAlpha(alpha);canvas.drawPath(shape,paint);
            RectF inner=new RectF(outer);inner.inset(3*u,3*u);paint.setColor(blend(border,fill,.58f));paint.setAlpha(alpha);paint.setStrokeWidth(u*.6f);canvas.drawPath(cut(inner,5*u),paint);
            paint.setColor(!enabled?0xff5f5945:0xffac9360);paint.setAlpha(alpha);paint.setStrokeWidth(u);
            float length=Math.min(8*u,outer.height()*.2f);
            for(int corner=0;corner<4;corner++){float x=corner%2==0?outer.left:outer.right,y=corner<2?outer.top:outer.bottom;float dx=corner%2==0?1:-1,dy=corner<2?1:-1;canvas.drawLine(x+dx*9*u,y+dy*2*u,x+dx*(9*u+length),y+dy*2*u,paint);canvas.drawLine(x+dx*2*u,y+dy*9*u,x+dx*2*u,y+dy*(9*u+length),paint);}
            if(ornament&&outer.width()>120*u){float cx=outer.centerX(),cy=outer.top+u*2;paint.setStyle(Paint.Style.FILL);Path seal=new Path();seal.moveTo(cx,cy-2*u);seal.lineTo(cx+4*u,cy+2*u);seal.lineTo(cx,cy+6*u);seal.lineTo(cx-4*u,cy+2*u);seal.close();canvas.drawPath(seal,paint);}
            paint.setStyle(Paint.Style.FILL);
        }
        @Override public void setAlpha(int value){alpha=value;invalidateSelf();}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    static final class Backdrop extends Drawable {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path[] ridges=new Path[3];private Shader sky;private int alpha=255;
        @Override protected void onBoundsChange(Rect bounds){
            float w=bounds.width(),h=bounds.height();sky=new LinearGradient(0,0,w,h,new int[]{0xff353934,0xff242b2b,0xff1b211f},null,Shader.TileMode.CLAMP);
            for(int layer=0;layer<3;layer++){Path ridge=new Path();float base=h*(.22f+layer*.22f);ridge.moveTo(0,h);ridge.lineTo(0,base+h*.08f);
                for(int i=0;i<7;i++){float x=i*w/6f;ridge.cubicTo(x+w*.04f,base-h*(.1f+(i%2)*.05f),x+w*.11f,base+h*.07f,x+w/6f,base+h*((i+layer)%3)*.045f);}ridge.lineTo(w,h);ridge.close();ridges[layer]=ridge;}
        }
        @Override public void draw(Canvas canvas){
            Rect b=getBounds();float w=b.width(),h=b.height();if(w<=0||h<=0)return;
            paint.setStyle(Paint.Style.FILL);paint.setAlpha(alpha);paint.setShader(sky);canvas.drawRect(b,paint);paint.setShader(null);
            int[] mist={0xff41483f,0xff333d37,0xff242e2b};for(int i=0;i<3;i++){paint.setColor(mist[i]);paint.setAlpha(alpha);canvas.drawPath(ridges[i],paint);}
            fortress(canvas,w*.02f,h*.28f,h*.24f);fortress(canvas,w*.76f,h*.73f,h*.23f);
            paint.setColor(0x207c8064);paint.setAlpha(alpha*32/255);paint.setStrokeWidth(Math.max(1,h*.0014f));
            for(int i=0;i<7;i++)canvas.drawLine(0,h*(.68f+i*.05f),w,h*(.72f+i*.05f),paint);
            paint.setAlpha(alpha);
        }
        private void fortress(Canvas canvas,float x,float y,float scale){
            canvas.save();canvas.translate(x,y);canvas.scale(scale/100f,scale/100f);paint.setStyle(Paint.Style.FILL);paint.setColor(0xff28302b);paint.setAlpha(alpha);
            canvas.drawRect(-12,45,150,77,paint);canvas.drawRect(9,8,63,61,paint);canvas.drawRect(94,25,122,65,paint);
            for(int i=0;i<12;i++)canvas.drawRect(-12+i*14,39,-4+i*14,48,paint);
            roof(canvas,3,8,72);roof(canvas,89,25,39);
            paint.setColor(0xff465044);paint.setAlpha(alpha);for(int i=0;i<4;i++)canvas.drawRect(18+i*10,20,21+i*10,35,paint);
            paint.setStrokeWidth(1);canvas.drawLine(132,-35,132,56,paint);Path flag=new Path();flag.moveTo(132,-33);flag.cubicTo(147,-42,152,-26,166,-33);flag.lineTo(160,-14);flag.cubicTo(147,-9,143,-25,132,-15);flag.close();paint.setColor(0xff514737);paint.setAlpha(alpha);canvas.drawPath(flag,paint);canvas.restore();
        }
        private void roof(Canvas c,float x,float y,float width){Path roof=new Path();roof.moveTo(x-8,y+3);roof.quadTo(x+width*.25f,y-1,x+width*.5f,y-16);roof.quadTo(x+width*.75f,y-1,x+width+8,y+3);roof.lineTo(x+width,y+8);roof.lineTo(x,y+8);roof.close();paint.setColor(0xff424538);paint.setAlpha(alpha);c.drawPath(roof,paint);}
        @Override public void setAlpha(int value){alpha=value;invalidateSelf();}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.OPAQUE;}
    }
}
