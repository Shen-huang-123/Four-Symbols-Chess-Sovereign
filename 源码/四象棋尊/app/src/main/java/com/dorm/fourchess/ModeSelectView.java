package com.dorm.fourchess;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

/** Independently laid-out mode lobby. Existing Listener callbacks remain the only game entry points. */
final class ModeSelectView extends View {
    interface Listener { void onLocal(); void onHost(); void onJoin(); }
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);

    private final Bitmap reference,heroArt;
    private final Rect portraitSource=new Rect();
    private static final Rect[] HERO_PORTRAITS={new Rect(8,0,260,270),new Rect(266,0,524,270),new Rect(532,0,785,270),new Rect(792,0,1057,270)};
    private static final String[] HERO_NAMES={"红尘弈心","花间一弈","清风入弈","金陵弈梦"};
    private static final String[] HERO_CAPTIONS={"赤焰执棋 · 四方争锋","花间落子 · 弈会知音","月下邀弈 · 共赴四方","金风入局 · 弈梦山河"};
    private static final int[] HERO_ACCENTS=GameDesign.factionLights();
    private final java.util.Random heroRandom=new java.util.Random();
    private int heroIndex=2;
    private final RectF heroRegion=new RectF(),heroImage=new RectF(),header=new RectF();
    private final RectF[] ruleChoices={new RectF(),new RectF()};
    private boolean skillSelected;private int rulePressed=-1;
    private final RectF[] cards={new RectF(),new RectF(),new RectF()};
    private final RectF[] titleBounds={new RectF(),new RectF(),new RectF()},subtitleBounds={new RectF(),new RectF(),new RectF()};
    private final float[] titleSizes=new float[3],subtitleSizes=new float[3];
    private final String[] titles={"同屏四人对局","创建局域网房间","加入局域网房间"};
    private final String[] subtitles={"一机四人 · 即刻开战","邀友入局 · 自由设定","寻找对局 · 四方会战"};
    private final int[] colors={0xff8c433b,0xff426b86,0xff477d66};
    private final float[] pressAmounts=new float[3];
    private final android.animation.ValueAnimator[] pressAnimators=new android.animation.ValueAnimator[3];
    private Listener listener;
    private int pressed=-1,confirming=-1,keyboardChoice=0,accessibilityFocus=-1;
    private long confirmStarted,heroEnteredAt;
    private float touchX,touchY,referenceCardHeight,cardHeight;
    private boolean portrait;
    private Runnable pendingSelection;
    private final AccessibilityNodeProvider nodes=new ModeNodes();

    ModeSelectView(Context context){
        super(context);setFocusable(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        reference=BitmapFactory.decodeResource(getResources(),R.drawable.ui_reference);
        int id=getResources().getIdentifier("mode_hero_qingfeng","drawable",context.getPackageName());
        Bitmap candidate=id==0?null:BitmapFactory.decodeResource(getResources(),id);
        if(candidate!=null&&(!candidate.hasAlpha()||Color.alpha(candidate.getPixel(0,0))!=0||Color.alpha(candidate.getPixel(candidate.getWidth()-1,0))!=0)){candidate.recycle();candidate=null;}
        heroArt=candidate;selectHero(2);
    }
    void setListener(Listener value){listener=value;}
    boolean usesSkills(){return skillSelected;}
    void selectSkills(boolean value){skillSelected=value;invalidate();sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);}
    void onEnter(){
        cancelEffects();keyboardChoice=0;heroEnteredAt=android.os.SystemClock.uptimeMillis();
        android.content.SharedPreferences preferences=getContext().getSharedPreferences("mode_art",Context.MODE_PRIVATE);
        int previous=preferences.getInt("last_hero",-1);
        int next=previous>=0&&previous<HERO_NAMES.length?(previous+1+heroRandom.nextInt(HERO_NAMES.length-1))%HERO_NAMES.length:heroRandom.nextInt(HERO_NAMES.length);
        selectHero(next);preferences.edit().putInt("last_hero",next).apply();
        updateHeroBounds();invalidate();
    }
    private void selectHero(int index){
        heroIndex=index;Rect source=HERO_PORTRAITS[index];
        portraitSource.set(source.left+(int)(source.width()*.14f),source.top+(int)(source.height()*.02f),source.right-(int)(source.width()*.02f),source.bottom-(int)(source.height()*.12f));
        int side=Math.min(portraitSource.width(),portraitSource.height());portraitSource.left+=(portraitSource.width()-side)/2;portraitSource.top+=(portraitSource.height()-side)/2;portraitSource.right=portraitSource.left+side;portraitSource.bottom=portraitSource.top+side;
    }
    // A future blue half-body asset belongs only to the blue character.
    private Bitmap currentHeroArt(){return heroIndex==2?heroArt:null;}
    private float dp(float value){return value*getResources().getDisplayMetrics().density;}
    private float sp(float value){return value*getResources().getDisplayMetrics().scaledDensity;}
    private float clamp(float value,float low,float high){return Math.max(low,Math.min(high,value));}
    private boolean animationsEnabled(){return android.os.Build.VERSION.SDK_INT<26||android.animation.ValueAnimator.areAnimatorsEnabled();}

    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);cancelEffects();layoutPage(w,h);
    }
    private void layoutPage(float w,float h){
        portrait=h>=w;boolean lowHeight=!portrait&&h<dp(280);float pad=lowHeight?dp(4):clamp(Math.min(w,h)*.045f,dp(12),dp(24));
        float contentWidth=portrait?Math.min(w-pad*2,dp(600)):w-pad*2,contentLeft=(w-contentWidth)/2;
        float headerHeight=lowHeight?dp(24):portrait?clamp(h*.09f,dp(62),dp(82)):clamp(h*.19f,dp(46),dp(82));
        header.set(contentLeft,pad,contentLeft+contentWidth,pad+headerHeight);
        float footer=lowHeight?0:clamp(h*.047f,dp(24),dp(44));
        if(portrait){
            float oldPad=w*.045f,oldHeader=Math.min(h*.30f,w*.64f),oldGap=oldPad*.8f;
            referenceCardHeight=(h-oldPad*2.2f-oldHeader-oldPad-oldGap*2)/3;
            float gap=clamp(h*.014f,dp(8),dp(14));
            float available=h-pad-footer-header.bottom-dp(54)-gap*3;
            cardHeight=Math.min(clamp(referenceCardHeight*.62f,dp(72),dp(112)),Math.max(dp(44),available/3));
            float top=h-pad-footer-cardHeight*3-gap*2;
            heroRegion.set(contentLeft,header.bottom+dp(4),contentLeft+contentWidth,top-dp(28));
            for(int i=0;i<3;i++)cards[i].set(contentLeft,top+i*(cardHeight+gap),contentLeft+contentWidth,top+i*(cardHeight+gap)+cardHeight);
        }else{
            float oldPad=Math.max(14,h*.035f),oldTop=h*.29f,oldGap=h*.025f;
            referenceCardHeight=(h-oldTop-oldPad-oldGap*2)/3;
            float gap=lowHeight?dp(3):clamp(h*.025f,dp(5),dp(14));
            float available=h-header.bottom-dp(49)-footer-pad;
            cardHeight=Math.min(clamp(referenceCardHeight*.70f,dp(44),dp(132)),Math.max(dp(44),(available-gap*2)/3));
            float left=w*.43f,right=w-pad,top=header.bottom+dp(49)+Math.max(dp(4),(available-cardHeight*3-gap*2)/2);
            for(int i=0;i<3;i++)cards[i].set(left,top+i*(cardHeight+gap),right,top+i*(cardHeight+gap)+cardHeight);
            heroRegion.set(pad,pad,w*.40f,h-pad-footer);
        }
        if(portrait)heroRegion.top+=dp(52);
        float rulesLeft=portrait?contentLeft:cards[0].left,rulesRight=portrait?contentLeft+contentWidth:cards[0].right;
        float rulesGap=dp(8),rulesMiddle=(rulesLeft+rulesRight)/2;
        ruleChoices[0].set(rulesLeft,header.bottom+dp(3),rulesMiddle-rulesGap/2,header.bottom+dp(47));
        ruleChoices[1].set(rulesMiddle+rulesGap/2,header.bottom+dp(3),rulesRight,header.bottom+dp(47));
        updateHeroBounds();invalidate();
    }
    private void updateHeroBounds(){
        Bitmap heroArt=currentHeroArt();
        heroImage.setEmpty();if(heroRegion.width()<1||heroRegion.height()<1)return;
        if(heroArt!=null){float scale=Math.min(heroRegion.width()*.90f/heroArt.getWidth(),heroRegion.height()*.96f/heroArt.getHeight());float width=heroArt.getWidth()*scale,height=heroArt.getHeight()*scale;heroImage.set(heroRegion.centerX()-width/2,heroRegion.bottom-height,heroRegion.centerX()+width/2,heroRegion.bottom);}
        else{
            // A native-resolution portrait medallion, explicitly not an enlarged/cut-out half-body.
            float diameter=Math.min(Math.min(208,Math.min(portraitSource.width(),portraitSource.height())),Math.min(heroRegion.height()*.49f,heroRegion.width()*.48f));
            float y=heroRegion.centerY()-Math.min(dp(22),heroRegion.height()*.08f);heroImage.set(heroRegion.centerX()-diameter/2,y-diameter/2,heroRegion.centerX()+diameter/2,y+diameter/2);
        }
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);if(getWidth()==0||getHeight()==0)return;float w=getWidth(),h=getHeight();

        drawAmbient(c);drawHero(c);drawHeader(c);drawRules(c);
        float captionY=portrait?cards[0].top-dp(15):header.bottom+dp(7);
        if(portrait)label(c,"选 择 入 局 方 式",w/2,captionY,sp(12),GameDesign.SECONDARY,Paint.Align.CENTER,false);
        for(int i=0;i<3;i++)drawCard(c,cards[i],i);
        float bottom=cards[2].bottom+(h-cards[2].bottom)*.52f;
        if(h-cards[2].bottom>=dp(18))label(c,"四方落子 · 棋会天下",portrait?w/2:cards[0].centerX(),bottom,Math.min(sp(11),dp(12)),GameDesign.MUTED,Paint.Align.CENTER,false);
        if(isShown()&&hasWindowFocus()&&animationsEnabled())postInvalidateDelayed(confirming>=0||android.os.SystemClock.uptimeMillis()-heroEnteredAt<650?16:50);
    }
    private void drawRules(Canvas c){
        for(int i=0;i<2;i++){
            RectF r=ruleChoices[i];boolean chosen=skillSelected==(i==1);p.setStyle(Paint.Style.FILL);p.setColor(chosen?GameDesign.PANEL_RAISED:GameDesign.PANEL_QUIET);c.drawRoundRect(r,dp(GameDesign.RADIUS_SM),dp(GameDesign.RADIUS_SM),p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(chosen?1:.6f));p.setColor(chosen?GameDesign.GOLD:GameDesign.EDGE_QUIET);c.drawRoundRect(r,dp(GameDesign.RADIUS_SM),dp(GameDesign.RADIUS_SM),p);p.setStyle(Paint.Style.FILL);
            fit(c,i==0?"普通对局 · 原版规则":"四象秘术 · 技能对局",r.centerX(),r.centerY(),Math.min(sp(12),r.height()*.34f),r.width()-dp(12),chosen?GameDesign.GOLD_BRIGHT:GameDesign.SECONDARY,Paint.Align.CENTER,chosen,null);
        }
    }
    private void drawHeader(Canvas c){
        float x=portrait?header.centerX():cards[0].centerX();
        label(c,"四 象 棋 尊",x,header.top+header.height()*(header.height()<dp(40)?.50f:.29f),Math.min(sp(GameDesign.TYPE_DISPLAY),header.height()*(header.height()<dp(40)?.62f:.41f)),GameDesign.GOLD_BRIGHT,Paint.Align.CENTER,true);
        if(header.height()>=dp(40))fit(c,"四方争锋 · 一局定乾坤",x,header.top+header.height()*.76f,Math.min(sp(12),header.height()*.18f),portrait?header.width():getWidth()*.56f,GameDesign.SECONDARY,Paint.Align.CENTER,false,null);        if(header.height()>=dp(46)){float y=header.bottom-dp(1),half=Math.min(dp(56),header.width()*.17f);p.setColor(0x778d9b9d);p.setStrokeWidth(dp(.6f));c.drawLine(x-half,y,x-dp(8),y,p);c.drawLine(x+dp(8),y,x+half,y,p);p.setColor(0xffc1ab7e);Path seal=new Path();seal.moveTo(x,y-dp(2.5f));seal.lineTo(x+dp(2.5f),y);seal.lineTo(x,y+dp(2.5f));seal.lineTo(x-dp(2.5f),y);seal.close();c.drawPath(seal,p);}
    }
    private void drawHero(Canvas c){
        Bitmap heroArt=currentHeroArt();
        if(heroRegion.height()<1)return;float reveal=animationsEnabled()?Math.min(1,(android.os.SystemClock.uptimeMillis()-heroEnteredAt)/240f):1;int heroSave=c.saveLayerAlpha(heroRegion,Math.round(255*reveal));c.translate(0,dp(5)*(1-reveal));float cx=heroRegion.centerX(),cy=heroRegion.centerY();
        p.setShader(new RadialGradient(cx,cy,Math.max(1,heroRegion.height()*.6f),0x30000000|(HERO_ACCENTS[heroIndex]&0xffffff),HERO_ACCENTS[heroIndex]&0xffffff,Shader.TileMode.CLAMP));c.drawOval(heroRegion,p);p.setShader(null);
        if(heroArt!=null){p.setColor(Color.WHITE);p.setAlpha(255);c.drawBitmap(heroArt,null,heroImage,p);}
        else if(!heroImage.isEmpty()){
            float r=heroImage.width()/2;
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(.65f));p.setColor(0x77d3bc89);c.drawCircle(heroImage.centerX(),heroImage.centerY(),r+dp(9),p);
            p.setColor(0x445b9bb5);c.drawArc(heroImage.left-dp(16),heroImage.top-dp(16),heroImage.right+dp(16),heroImage.bottom+dp(16),205,105,false,p);p.setStyle(Paint.Style.FILL);
            if(animationsEnabled()){
                float angle=(android.os.SystemClock.uptimeMillis()%14000)/14000f*(float)(Math.PI*2),orbit=r+dp(9);
                float px=heroImage.centerX()+(float)Math.cos(angle)*orbit,py=heroImage.centerY()+(float)Math.sin(angle)*orbit;
                p.setColor(0x28efd9aa);c.drawCircle(px,py,dp(4),p);p.setColor(0xffecd8a8);c.drawCircle(px,py,dp(1.4f),p);
            }
            c.save();Path circle=new Path();circle.addCircle(heroImage.centerX(),heroImage.centerY(),r,Path.Direction.CW);c.clipPath(circle);p.setColor(Color.WHITE);p.setAlpha(255);float scale=Math.max(heroImage.width()/portraitSource.width(),heroImage.height()/portraitSource.height());RectF fitted=new RectF(heroImage.centerX()-portraitSource.width()*scale/2,heroImage.centerY()-portraitSource.height()*scale/2,heroImage.centerX()+portraitSource.width()*scale/2,heroImage.centerY()+portraitSource.height()*scale/2);c.drawBitmap(reference,portraitSource,fitted,p);c.restore();
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor(HERO_ACCENTS[heroIndex]);c.drawCircle(heroImage.centerX(),heroImage.centerY(),r,p);p.setStyle(Paint.Style.FILL);
            float textY=heroImage.bottom+Math.min(dp(29),heroRegion.height()*.14f);
            if(textY+dp(26)<heroRegion.bottom){label(c,HERO_NAMES[heroIndex],cx,textY,Math.min(sp(19),heroRegion.width()*.085f),GameDesign.TEXT,Paint.Align.CENTER,true);label(c,HERO_CAPTIONS[heroIndex],cx,textY+dp(26),Math.min(sp(11),dp(12)),GameDesign.SECONDARY,Paint.Align.CENTER,false);}
        }
        if(heroArt!=null&&heroRegion.height()>dp(130))label(c,HERO_NAMES[heroIndex],heroRegion.left+dp(12),heroRegion.bottom-dp(13),sp(15),GameDesign.TEXT,Paint.Align.LEFT,true);c.restoreToCount(heroSave);
    }
    private void drawCard(Canvas c,RectF r,int index){
        float age=android.os.SystemClock.uptimeMillis()-heroEnteredAt;
        float reveal=animationsEnabled()?clamp((age-90-index*75)/(float)GameDesign.ENTER_MS,0,1):1;
        float eased=1-(1-reveal)*(1-reveal)*(1-reveal);
        // Paint only: accessibility and touch bounds stay at the real card rectangle.
        int cardSave=reveal<1?c.saveLayerAlpha(r.left-dp(3),r.top-dp(3),r.right+dp(3),r.bottom+dp(20),Math.round(255*reveal)):c.save();
        c.translate(0,dp(12)*(1-eased));float amount=pressAmounts[index],radius=Math.min(dp(13),r.height()*.17f);c.scale(1-.018f*amount,1-.018f*amount,r.centerX(),r.centerY());c.translate(0,dp(1.2f)*amount);
        p.setStyle(Paint.Style.FILL);p.setColor(GameDesign.SHADOW);c.drawRoundRect(r.left,r.top+dp(3),r.right,r.bottom+dp(3),radius,radius,p);
        int base=BattleSkin.blend(GameDesign.PANEL,colors[index],pressed==index?.58f:.38f);
        p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,0xbf000000|(base&0xffffff),GameDesign.PANEL_QUIET,Shader.TileMode.CLAMP));p.setAlpha(255);c.drawRoundRect(r,radius,radius,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(GameDesign.STROKE));p.setColor(pressed==index?GameDesign.GOLD_BRIGHT:GameDesign.EDGE);c.drawRoundRect(r,radius,radius,p);
        p.setStrokeWidth(dp(.6f));p.setColor(0x55e4d3af);c.drawLine(r.left+radius,r.top+dp(2),r.right-radius,r.top+dp(2),p);p.setStyle(Paint.Style.FILL);
        p.setColor(0x77c5ae7e);float ornamentY=r.centerY(),ornamentX=r.right-dp(33);c.drawCircle(ornamentX,ornamentY,dp(1.2f),p);
        float iconSize=Math.min(dp(38),r.height()*.45f),cx=r.left+Math.min(dp(49),r.height()*.50f),cy=r.centerY();
        p.setColor(0x334f7787);c.drawCircle(cx,cy,iconSize*.77f,p);p.setStyle(Paint.Style.STROKE);p.setColor(0x779e947b);p.setStrokeWidth(dp(.6f));c.drawCircle(cx,cy,iconSize*.77f,p);p.setStyle(Paint.Style.FILL);drawModeIcon(c,index,cx,cy,iconSize);
        float tx=cx+iconSize*.95f,right=r.right-dp(34),width=Math.max(1,right-tx);
        float titleSize=Math.min(sp(19),r.height()*.27f),subSize=Math.min(sp(11),r.height()*.17f);
        titleSizes[index]=fit(c,titles[index],tx,cy-r.height()*.14f,titleSize,width,GameDesign.TEXT,Paint.Align.LEFT,true,titleBounds[index]);
        subtitleSizes[index]=fit(c,subtitles[index],tx,cy+r.height()*.20f,subSize,width,GameDesign.SECONDARY,Paint.Align.LEFT,false,subtitleBounds[index]);
        float arrowX=r.right-dp(18),a=Math.min(dp(5),r.height()*.075f);p.setStyle(Paint.Style.STROKE);p.setColor(GameDesign.GOLD);p.setStrokeWidth(dp(1.5f));Path arrow=new Path();arrow.moveTo(arrowX-a*.5f,cy-a);arrow.lineTo(arrowX+a*.5f,cy);arrow.lineTo(arrowX-a*.5f,cy+a);c.drawPath(arrow,p);p.setStyle(Paint.Style.FILL);
        if(index==confirming&&animationsEnabled()){
            float sweep=clamp((android.os.SystemClock.uptimeMillis()-confirmStarted)/(float)GameDesign.CONFIRM_MS,0,1);
            c.save();Path mask=new Path();mask.addRoundRect(r,radius,radius,Path.Direction.CW);c.clipPath(mask);
            float sx=r.left+r.width()*sweep*1.35f-r.width()*.18f,sw=r.width()*.16f;
            p.setShader(new LinearGradient(sx-sw,0,sx+sw,0,new int[]{0x00ead6a4,0x50ead6a4,0x00ead6a4},null,Shader.TileMode.CLAMP));c.drawRect(r,p);p.setShader(null);c.restore();
        }
        if(index==confirming){float f=Math.min(1,(android.os.SystemClock.uptimeMillis()-confirmStarted)/(float)GameDesign.CONFIRM_MS);c.save();Path clip=new Path();clip.addRoundRect(r,radius,radius,Path.Direction.CW);c.clipPath(clip);p.setShader(new RadialGradient(touchX,touchY,Math.max(1,r.width()*f),((int)(44*(1-f))<<24)|0xf1d6a2,0x00f1d6a2,Shader.TileMode.CLAMP));p.setAlpha(255);c.drawRect(r,p);p.setShader(null);c.restore();}
        if(hasFocus()&&keyboardChoice==index){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.2f));p.setColor(0xffdcc18a);c.drawRoundRect(r,radius,radius,p);p.setStyle(Paint.Style.FILL);}c.restoreToCount(cardSave);
    }
    private void drawModeIcon(Canvas c,int type,float x,float y,float size){
        c.save();c.translate(x-size/2,y-size/2);c.scale(size/24,size/24);p.setColor(0xffe4d2ab);p.setStrokeWidth(1.25f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        if(type==0){p.setStyle(Paint.Style.FILL);for(int row=0;row<2;row++)for(int col=0;col<2;col++){float cx=7+col*10,cy=5+row*10;c.drawCircle(cx,cy,2.2f,p);c.drawRoundRect(cx-3,cy+3,cx+3,cy+7,2,2,p);}}
        else if(type==1){p.setStyle(Paint.Style.STROKE);Path roof=new Path();roof.moveTo(2,10);roof.quadTo(8,9,12,3);roof.quadTo(16,9,22,10);roof.lineTo(19,12);roof.lineTo(5,12);roof.close();c.drawPath(roof,p);c.drawLine(6,13,6,21,p);c.drawLine(18,13,18,21,p);c.drawLine(12,13,12,21,p);c.drawLine(3,22,21,22,p);}
        else{p.setStyle(Paint.Style.STROKE);c.save();c.rotate(-35,12,12);c.drawRoundRect(2,8,14,16,4,4,p);c.drawRoundRect(10,8,22,16,4,4,p);c.restore();}p.setStyle(Paint.Style.FILL);c.restore();
    }
    private void drawAmbient(Canvas c){
        if(!animationsEnabled())return;float phase=(android.os.SystemClock.uptimeMillis()%24000)/24000f;
        // Petals stay outside controls and hero text; no hit testing is affected.
        for(int i=0;i<8;i++){
            float t=(phase+i*.137f)%1f,x=getWidth()*((i*.213f+.08f)%1)+(float)Math.sin(t*6.283f+i)*dp(12),y=t*(getHeight()+dp(30))-dp(15);
            boolean safe=!heroRegion.contains(x,y)&&!header.contains(x,y);for(RectF card:cards)safe&=!card.contains(x,y);if(!safe)continue;
            p.setColor(i%2==0?0x70d6a5a6:0x55e0c49b);c.save();c.rotate(t*210+i*29,x,y);c.drawOval(x-dp(3),y-dp(1.2f),x+dp(3),y+dp(1.2f),p);c.restore();
        }
        for(int i=0;i<14;i++){float x=getWidth()*((i*.173f+phase*.13f)%1),y=getHeight()*((i*.137f+phase*.21f)%1);boolean safe=true;for(RectF card:cards)safe&=!card.contains(x,y);if(!safe)continue;p.setColor(i%3==0?0x88b3cdd6:0x779c8d6e);c.drawCircle(x,y,dp(i%3==0?1.2f:.7f),p);}
    }
    private void label(Canvas c,String text,float x,float center,float size,int color,Paint.Align align,boolean bold){
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTypeface((bold?GameDesign.EMPHASIS:GameDesign.BODY));p.setTextSize(size);p.setTextAlign(align);c.drawText(text,x,center-(p.ascent()+p.descent())/2,p);
    }
    private float fit(Canvas c,String text,float x,float cy,float size,float width,int color,Paint.Align align,boolean bold,RectF bounds){
        p.setTypeface((bold?GameDesign.EMPHASIS:GameDesign.BODY));p.setTextSize(size);float measured=p.measureText(text);if(measured>width)size*=width/measured;
        label(c,text,x,cy,size,color,align,bold);float length=p.measureText(text),left=align==Paint.Align.CENTER?x-length/2:align==Paint.Align.RIGHT?x-length:x;float baseline=cy-(p.ascent()+p.descent())/2;if(bounds!=null)bounds.set(left,baseline+p.ascent(),left+length,baseline+p.descent());return size;
    }
    private void animatePress(int index,boolean down){
        if(index<0||index>2)return;if(pressAnimators[index]!=null)pressAnimators[index].cancel();if(!animationsEnabled()){pressAmounts[index]=down?1:0;invalidate();return;}
        android.animation.ValueAnimator animator=android.animation.ValueAnimator.ofFloat(pressAmounts[index],down?1:0);pressAnimators[index]=animator;animator.setDuration(down?GameDesign.PRESS_MS:GameDesign.RELEASE_MS);animator.setInterpolator(new android.view.animation.DecelerateInterpolator());animator.addUpdateListener(a->{pressAmounts[index]=(Float)a.getAnimatedValue();invalidate();});animator.start();
    }
    private void releasePress(){int old=pressed;pressed=-1;animatePress(old,false);invalidate();}
    private void choose(int index){
        if(!isShown()||!hasWindowFocus())return;
        if(index<0||index>2||confirming>=0||listener==null)return;confirming=index;confirmStarted=android.os.SystemClock.uptimeMillis();performClick();performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
        pendingSelection=()->{pendingSelection=null;int choice=confirming;confirming=-1;if(choice<0||!isShown()||!hasWindowFocus()||listener==null)return;if(choice==0)listener.onLocal();else if(choice==1)listener.onHost();else listener.onJoin();};postDelayed(pendingSelection,animationsEnabled()?GameDesign.CONFIRM_MS:0);invalidate();
    }
    private void cancelEffects(){
        if(pendingSelection!=null){removeCallbacks(pendingSelection);pendingSelection=null;}confirming=-1;pressed=-1;rulePressed=-1;
        if(pressAnimators!=null)for(int i=0;i<pressAnimators.length;i++){if(pressAnimators[i]!=null){pressAnimators[i].cancel();pressAnimators[i]=null;}pressAmounts[i]=0;}
    }
    private int hit(float x,float y){for(int i=0;i<cards.length;i++)if(cards[i].contains(x,y))return i;return -1;}
    @Override public boolean onTouchEvent(MotionEvent event){
        if(confirming>=0)return true;float x=event.getX(),y=event.getY();int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){for(int i=0;i<2;i++)if(ruleChoices[i].contains(x,y)){rulePressed=i;return true;}pressed=hit(x,y);if(pressed>=0)keyboardChoice=pressed;touchX=x;touchY=y;animatePress(pressed,true);invalidate();return true;}
        if(rulePressed>=0){if(action==MotionEvent.ACTION_UP){int rule=rulePressed;rulePressed=-1;if(ruleChoices[rule].contains(x,y)){selectSkills(rule==1);performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);}}else if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN)rulePressed=-1;return true;}
        if(action==MotionEvent.ACTION_MOVE){if(pressed>=0&&hit(x,y)!=pressed)releasePress();return true;}
        if(action==MotionEvent.ACTION_UP){int choice=pressed;releasePress();if(choice>=0&&hit(x,y)==choice){touchX=x;touchY=y;choose(choice);}return true;}
        if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_POINTER_UP){releasePress();return true;}return true;
    }
    @Override public boolean onKeyDown(int code,KeyEvent event){
        if(code==KeyEvent.KEYCODE_DPAD_DOWN||code==KeyEvent.KEYCODE_DPAD_RIGHT){keyboardChoice=(keyboardChoice+1)%3;invalidate();return true;}
        if(code==KeyEvent.KEYCODE_DPAD_UP||code==KeyEvent.KEYCODE_DPAD_LEFT){keyboardChoice=(keyboardChoice+2)%3;invalidate();return true;}
        if((code==KeyEvent.KEYCODE_ENTER||code==KeyEvent.KEYCODE_DPAD_CENTER||code==KeyEvent.KEYCODE_SPACE)&&event.getRepeatCount()==0){choose(keyboardChoice);return true;}return super.onKeyDown(code,event);
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(!focused)cancelEffects();else invalidate();}
    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);if(visibility!=VISIBLE)cancelEffects();}
    @Override protected void onDetachedFromWindow(){cancelEffects();super.onDetachedFromWindow();}
    @Override public AccessibilityNodeProvider getAccessibilityNodeProvider(){return nodes;}
    private final class ModeNodes extends AccessibilityNodeProvider {
        @Override public AccessibilityNodeInfo createAccessibilityNodeInfo(int id){
            if(id==View.NO_ID){AccessibilityNodeInfo node=AccessibilityNodeInfo.obtain(ModeSelectView.this);onInitializeAccessibilityNodeInfo(node);for(int i=1;i<=5;i++)node.addChild(ModeSelectView.this,i);return node;}
            if(id<1||id>5)return null;int index=id-1;RectF rect=id<=3?cards[index]:ruleChoices[id-4];AccessibilityNodeInfo node=AccessibilityNodeInfo.obtain();node.setSource(ModeSelectView.this,id);node.setParent(ModeSelectView.this);node.setPackageName(getContext().getPackageName());node.setClassName("android.widget.Button");node.setText(id<=3?titles[index]:id==4?"普通对局":"四象秘术");node.setContentDescription(id<=3?titles[index]+"，"+subtitles[index]:id==4?"普通对局，原版规则":"四象秘术，技能对局，每人随机四招各限一次");if(id>=4){node.setCheckable(true);node.setChecked(skillSelected==(id==5));}node.setClickable(true);node.setFocusable(true);node.setEnabled(isEnabled());node.setVisibleToUser(isShown());node.setAccessibilityFocused(accessibilityFocus==id);node.addAction(AccessibilityNodeInfo.ACTION_CLICK);node.addAction(accessibilityFocus==id?AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS:AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            Rect bounds=new Rect();rect.roundOut(bounds);node.setBoundsInParent(bounds);int[] location=new int[2];getLocationOnScreen(location);bounds.offset(location[0],location[1]);node.setBoundsInScreen(bounds);return node;
        }
        @Override public boolean performAction(int id,int action,Bundle args){
            if(id<1||id>5)return false;if(action==AccessibilityNodeInfo.ACTION_CLICK){if(id<=3)choose(id-1);else selectSkills(id==5);return true;}
            if(action==AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS||action==AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS){accessibilityFocus=action==AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS?id:-1;AccessibilityEvent event=AccessibilityEvent.obtain(action==AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS?AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);event.setSource(ModeSelectView.this,id);event.setPackageName(getContext().getPackageName());event.setClassName("android.widget.Button");if(getParent()!=null)getParent().requestSendAccessibilityEvent(ModeSelectView.this,event);invalidate();return true;}return false;
        }
    }
}

