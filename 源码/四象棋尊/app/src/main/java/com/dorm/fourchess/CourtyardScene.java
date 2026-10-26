package com.dorm.fourchess;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Independent, resolution-aware courtyard art. No board, text or controls baked in. */
final class CourtyardScene extends Drawable {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private Bitmap scene;
    private int opacity=255;
    @Override protected void onBoundsChange(Rect bounds){
        if(scene!=null){scene.recycle();scene=null;}
        if(bounds.width()<1||bounds.height()<1)return;
        // Bound the scene cache; UI and all chess glyphs remain full-resolution vectors.
        float ratio=Math.min(1f,1800f/Math.max(bounds.width(),bounds.height()));
        scene=Bitmap.createBitmap(Math.max(1,Math.round(bounds.width()*ratio)),Math.max(1,Math.round(bounds.height()*ratio)),Bitmap.Config.ARGB_8888);
        paintScene(new Canvas(scene),scene.getWidth(),scene.getHeight());
    }
    private void fill(Canvas c,int color,float l,float t,float r,float b){p.setShader(null);p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRect(l,t,r,b,p);}
    private void glow(Canvas c,float x,float y,float radius,int color){p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(x,y,radius,color,0x00000000,Shader.TileMode.CLAMP));c.drawCircle(x,y,radius,p);p.setShader(null);}
    private void paintScene(Canvas c,float w,float h){
        p.setShader(new LinearGradient(0,0,w*.45f,h,new int[]{GameDesign.NIGHT,0xff243b49,GameDesign.INK_TEAL,0xff192c37},new float[]{0,.42f,.77f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
        glow(c,w*.74f,h*.13f,w*.46f,0x306b91b2);p.setColor(0xffc7d5d6);c.drawCircle(w*.74f,h*.13f,w*.055f,p);p.setColor(0x1887a3b2);c.drawCircle(w*.725f,h*.122f,w*.013f,p);
        java.util.Random random=new java.util.Random(827);
        for(int i=0;i<46;i++){p.setColor(0x779fb8ca);c.drawCircle(random.nextFloat()*w,random.nextFloat()*h*.50f,Math.max(.5f,w*.001f),p);}
        for(int layer=0;layer<4;layer++){
            Path mountain=new Path();float y=h*(.24f+layer*.115f);mountain.moveTo(-w*.1f,h);mountain.lineTo(-w*.1f,y);
            for(int i=0;i<8;i++){float x=w*(i-1)/6;mountain.cubicTo(x+w*.05f,y-h*(.06f+random.nextFloat()*.10f),x+w*.13f,y+h*.02f,x+w*.17f,y+h*.035f);}
            mountain.lineTo(w*1.2f,h);mountain.close();p.setColor(new int[]{0xff304959,0xff2b424f,0xff253641,0xff202e37}[layer]);c.drawPath(mountain,p);
        }
        pavilion(c,-w*.08f,h*.32f,w*.37f,h*.16f,0xff25323d);pavilion(c,w*.77f,h*.28f,w*.30f,h*.14f,0xff283744);
        pavilion(c,w*.67f,h*.52f,w*.31f,h*.18f,0xff1b2732);
        // Lake and reflected window lights.
        fill(c,0xff24343e,0,h*.73f,w,h);
        for(int i=0;i<35;i++){float y=h*(.74f+i*.007f),x=w*(.5f+(float)Math.sin(i*1.7f)*.42f);p.setColor(i%4==0?0x447b7971:0x203d6374);p.setStrokeWidth(Math.max(1,h*.001f));c.drawLine(x-w*.10f,y,x+w*.13f,y,p);}
        for(int i=0;i<10;i++){p.setColor((35-i*2)<<24|0x00edb67a);float yy=h*(.79f+i*.009f);c.drawOval(w*(.12f-i*.002f),yy,w*(.17f+i*.004f),yy+h*.002f,p);}
        // Stone terrace in perspective, kept at the foot of the composition.
        Path terrace=new Path();terrace.moveTo(0,h*.88f);terrace.quadTo(w*.38f,h*.81f,w*.67f,h*.87f);terrace.lineTo(w,h*.81f);terrace.lineTo(w,h);terrace.lineTo(0,h);terrace.close();p.setColor(0xff30363c);c.drawPath(terrace,p);
        p.setColor(0x445d6263);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,w*.0012f));
        for(int i=0;i<7;i++)c.drawLine(w*.48f,h*.85f,w*(i-1)/4,h,p);
        for(int i=0;i<4;i++){float y=h*(.89f+i*.03f);c.drawLine(0,y,w,y-h*.025f,p);}p.setStyle(Paint.Style.FILL);
        balustrade(c,w*.02f,h*.70f,w*.23f,h*.13f);balustrade(c,w*.79f,h*.68f,w*.25f,h*.14f);
        lantern(c,w*.10f,h*.72f,w*.048f);lantern(c,w*.90f,h*.69f,w*.048f);
        lantern(c,w*.025f,h*.32f,w*.035f);lantern(c,w*.97f,h*.29f,w*.032f);
        branch(c,w,h,false);branch(c,w,h,true);
        // Low-contrast central wash is a separate scene layer, beneath the real board.
        glow(c,w*.50f,h*.47f,Math.max(w,h)*.62f,0x4a13232c);
        p.setShader(new LinearGradient(0,0,0,h,new int[]{0x00111b24,0x00111b24,0x59121c27},new float[]{0,.72f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
    }
    private void pavilion(Canvas c,float x,float y,float w,float h,int color){
        c.save();c.translate(x,y);fill(c,color,w*.13f,h*.27f,w*.88f,h);
        for(int tier=0;tier<2;tier++){float yy=h*(.15f+tier*.4f),span=w*(1-tier*.12f),cx=w*.5f;Path roof=new Path();roof.moveTo(cx-span*.56f,yy+h*.10f);roof.quadTo(cx-span*.20f,yy+h*.10f,cx,yy-h*.15f);roof.quadTo(cx+span*.20f,yy+h*.10f,cx+span*.56f,yy+h*.10f);roof.lineTo(cx+span*.42f,yy+h*.18f);roof.lineTo(cx-span*.42f,yy+h*.18f);roof.close();p.setColor(color);c.drawPath(roof,p);p.setColor(0xff65717a);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,w*.004f));c.drawPath(roof,p);p.setStyle(Paint.Style.FILL);}
        for(int i=0;i<5;i++){float xx=w*(.23f+i*.13f);fill(c,0x66bf9368,xx,h*.46f,xx+w*.035f,h*.58f);fill(c,0x447b7564,xx,h*.78f,xx+w*.035f,h*.89f);}c.restore();
    }
    private void balustrade(Canvas c,float x,float y,float w,float h){
        fill(c,0xff28303b,x,y+h*.35f,x+w,y+h*.44f);fill(c,0xff202934,x,y+h*.84f,x+w,y+h);
        for(int i=0;i<4;i++){float xx=x+i*w/3;p.setColor(0xff35404a);c.drawRoundRect(xx-w*.025f,y+h*.2f,xx+w*.025f,y+h*.90f,w*.01f,w*.01f,p);p.setColor(0xff424a52);c.drawCircle(xx,y+h*.17f,w*.042f,p);}
    }
    private void lantern(Canvas c,float x,float y,float size){
        glow(c,x,y,size*3.2f,0x65d99552);fill(c,0xff28262a,x-size*.10f,y-size*.5f,x+size*.10f,y+size*1.65f);
        p.setColor(0xff4e3d32);c.drawRoundRect(x-size*.57f,y-size*.91f,x+size*.57f,y+size*.70f,size*.1f,size*.1f,p);
        p.setShader(new LinearGradient(x-size*.45f,y,x+size*.45f,y,0xffa16d42,0xffe5bd7a,Shader.TileMode.CLAMP));c.drawRoundRect(x-size*.42f,y-size*.69f,x+size*.42f,y+size*.5f,size*.15f,size*.15f,p);p.setShader(null);
        p.setColor(0xff624936);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,size*.045f));for(int i=-1;i<=1;i++)c.drawLine(x+i*size*.22f,y-size*.65f,x+i*size*.22f,y+size*.49f,p);p.setStyle(Paint.Style.FILL);
        Path roof=new Path();roof.moveTo(x-size*.82f,y-size*.77f);roof.quadTo(x,y-size*1.40f,x+size*.82f,y-size*.77f);roof.close();p.setColor(0xff35323a);c.drawPath(roof,p);p.setColor(0xff8d7351);c.drawRect(x-size*.7f,y-size*.78f,x+size*.7f,y-size*.71f,p);
    }
    private void branch(Canvas c,float w,float h,boolean right){
        c.save();if(right){c.translate(w,h*.08f);c.scale(-1,1);}
        p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(0xff30222b);p.setStrokeWidth(w*.027f);
        Path trunk=new Path();trunk.moveTo(-w*.03f,h*.5f);trunk.cubicTo(w*.09f,h*.30f,-w*.02f,h*.1f,w*.20f,-h*.02f);c.drawPath(trunk,p);p.setStrokeWidth(w*.009f);
        Path twig=new Path();twig.moveTo(w*.03f,h*.22f);twig.quadTo(w*.15f,h*.12f,w*.29f,h*.095f);twig.moveTo(w*.05f,h*.3f);twig.quadTo(w*.12f,h*.24f,w*.19f,h*.22f);c.drawPath(twig,p);p.setStyle(Paint.Style.FILL);
        java.util.Random random=new java.util.Random(right?62:23);
        for(int i=0;i<70;i++){float x=random.nextFloat()*w*.25f,y=random.nextFloat()*h*.33f;if(x>w*.14f&&y>h*.18f)continue;float size=w*(.003f+random.nextFloat()*.006f);p.setColor(i%3==0?0x998c4f57:i%3==1?0x77664350:0x887d5659);c.save();c.rotate(i*37,x,y);for(int petal=0;petal<4;petal++){c.rotate(90,x,y);c.drawOval(x-size*.4f,y-size*1.5f,x+size*.4f,y,p);}c.restore();}c.restore();
    }
    @Override public void draw(Canvas c){if(scene!=null){p.setColor(Color.WHITE);p.setAlpha(opacity);p.setShader(null);p.setStyle(Paint.Style.FILL);c.drawBitmap(scene,null,getBounds(),p);}}
    @Override public void setAlpha(int alpha){opacity=alpha;invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
