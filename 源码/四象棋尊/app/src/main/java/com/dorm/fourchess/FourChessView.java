package com.dorm.fourchess;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Rect;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class FourChessView extends View {
    interface StatusListener { void onStatus(String text); }
    interface MoveRequester { void request(int owner,int sx,int sy,int tx,int ty); }
    interface SkillRequester { void request(int owner,int skill,int sx,int sy,int tx,int ty); }
    interface MoveObserver { void onMove(String text); }
    static class Piece {
        int owner, x, y; char type; boolean sealed;
        Piece(int o, char t, int x, int y) { owner=o; type=t; this.x=x; this.y=y; }
        Piece copy() { Piece q=new Piece(owner,type,x,y);q.sealed=sealed;return q; }
    }
    static class Snapshot {
        final List<Piece> pieces;final int turn,fx,fy,tx,ty;int[] skillUse;int[][] hands,remaining;int rounds,roundMask;long seed;int skill,actor;DiplomacyState diplomacy;
        Snapshot(List<Piece> p,int t,int fx,int fy,int tx,int ty){pieces=p;turn=t;this.fx=fx;this.fy=fy;this.tx=tx;this.ty=ty;}
    }

    private final Paint p = new Paint(3); private final RectF boardRect = new RectF(); private final List<Piece> pieces = new ArrayList<>();
    private final List<Snapshot> history = new ArrayList<>(); private final boolean[] alive = new boolean[4];
    private DiplomacyState diplomacy=new DiplomacyState();
    private StatusListener listener; private MoveRequester requester; private MoveObserver moveObserver; private Piece selected; private List<int[]> moves = new ArrayList<>();
    private final int[][] skillHands=new int[4][SkillRules.HAND_SIZE];
    private final int[][] skillCharges=new int[4][SkillRules.COUNT+1];private int completedRounds,roundMask;private long dealSeed;
    private boolean skillMode;private final int[] skillUse=new int[4];private int pendingSkill,lastSkill,lastActor;private Piece skillSource;private SkillRequester skillRequester;
    private int turn=0, localPlayer=-1,lastFx=-1,lastFy=-1,lastTx=-1,lastTy=-1; private float cell,cellX,cellY,ox,oy;private final GameSound sounds=new GameSound();private boolean soundEnabled=true;
    private float rejectX=-1,rejectY=-1;private long rejectUntil;
    private final int[] colors=GameDesign.factionInks();
    private final String[] names={"红方","绿方","蓝方","金方"};
    private boolean largePieces=false,compactUi;
    private final RectF sizeToggle=new RectF();
    private float animationPhase;
    private final Rect glyphBounds=new Rect();
    private final BoardFeedback feedback=new BoardFeedback();
    private final BattleSkin.Backdrop battlefield=new BattleSkin.Backdrop();
    private float touchDownX,touchDownY,loupeX,loupeY;
    private boolean touchDown,tapEligible,inspecting;
    private long moveAnimationStart;
    private int animationFx,animationFy,animationTx,animationTy;
    private final Runnable inspectHold=()->{
        if(touchDown&&tapEligible){inspecting=true;loupeX=touchDownX;loupeY=touchDownY;performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);invalidate();}
    };

    public FourChessView(Context c) { super(c);android.content.SharedPreferences prefs=c.getSharedPreferences("duel_ui",Context.MODE_PRIVATE);if(!prefs.getBoolean("responsive_v27",false))prefs.edit().putBoolean("responsive_v27",true).putBoolean("large_pieces",false).apply();largePieces=prefs.getBoolean("large_pieces",false);p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);newGame(); }
    void setStatusListener(StatusListener l){listener=l; announce();}
    void setMoveRequester(MoveRequester r){requester=r;}
    void setMoveObserver(MoveObserver o){moveObserver=o;}
    void setSoundEnabled(boolean on){soundEnabled=on;sounds.setEnabled(on);if(on)sounds.play(GameSound.SELECT);}
    int currentTurn(){return turn;}
    boolean isPlayerAlive(int owner){return alive[owner];}
    void setLocalPlayer(int player){localPlayer=player;selected=null;moves.clear();invalidate();announce();}
    String ownerName(int owner){return names[owner];}
    void newGame(){ diplomacy=new DiplomacyState();completedRounds=roundMask=0;dealSeed=new java.security.SecureRandom().nextLong();dealSkills();java.util.Arrays.fill(skillUse,0);pendingSkill=lastSkill=lastActor=0;skillSource=null;pieces.clear(); history.clear(); selected=null; turn=0;lastFx=lastFy=lastTx=lastTy=-1; for(int i=0;i<4;i++)alive[i]=true;
        addSide(0); addSide(1); addSide(2); addSide(3); invalidate(); announce(); }
    private void addSide(int o) {
        char[] back={'C','M','X','S','J','S','X','M','C'};
        for(int i=0;i<9;i++){ int[] q=toBoard(o,i,0); pieces.add(new Piece(o,back[i],q[0],q[1])); }
        int[] cannonA=toBoard(o,1,2), cannonB=toBoard(o,7,2);
        pieces.add(new Piece(o,'P',cannonA[0],cannonA[1])); pieces.add(new Piece(o,'P',cannonB[0],cannonB[1]));
        for(int i=0;i<5;i++){int[] q=toBoard(o,i*2,3);pieces.add(new Piece(o,'B',q[0],q[1]));}
    }
    // Local coordinates: file 0..8 left-to-right from player's view, depth 0..18 forward.
    private int[] toBoard(int o,int file,int depth){
        if(o==0)return new int[]{5+file,18-depth}; // bottom
        if(o==1)return new int[]{depth,13-file};   // left
        if(o==2)return new int[]{13-file,depth};   // top
        return new int[]{18-depth,5+file};         // right
    }
    private int[] toLocal(int o,int x,int y){
        if(o==0)return new int[]{x-5,18-y};
        if(o==1)return new int[]{13-y,x};
        if(o==2)return new int[]{13-x,y};
        return new int[]{y-5,18-x};
    }
    void setCompactUi(boolean value){if(compactUi!=value){compactUi=value;sizeToggle.setEmpty();invalidate();}}
    boolean usesLargePieces(){return largePieces;}
    void togglePieceSize(){largePieces=!largePieces;getContext().getSharedPreferences("duel_ui",Context.MODE_PRIVATE).edit().putBoolean("large_pieces",largePieces).apply();invalidate();}
    float pieceDiameter(){return cell*(largePieces?.84f:.70f);}
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){
        // The unscaled cross is 18 intervals plus 0.48 of edge on each side.
        float padding=getResources().getDisplayMetrics().density*2f;
        cell=Math.max(1,Math.min(w-padding*2,h-padding*2)/18.96f*.95f);
        cellX=cellY=cell;ox=(w-cell*18)/2f;oy=(h-cell*18)/2f;
        battlefield.setBounds(0,0,w,h);
    }    private boolean validCell(int x,int y){ return x>=0&&x<19&&y>=0&&y<19&&(x>=5&&x<=13||y>=5&&y<=13); }
    private boolean motionEnabled(){return android.os.Build.VERSION.SDK_INT<26||android.animation.ValueAnimator.areAnimatorsEnabled();}
    @Override protected void onDraw(Canvas c){ super.onDraw(c);
        animationPhase=motionEnabled()?(System.currentTimeMillis()%8000L)/8000f:0;
        p.setShader(null); // Transparent unused corners reveal the independent courtyard scene.
        // Quiet gold frame around the actual square board area.
        RectF frame=new RectF(ox-cellX*.48f,oy-cellY*.48f,ox+18*cellX+cellX*.48f,oy+18*cellY+cellY*.48f);
        if(!compactUi){p.setColor(0x4018140f);c.drawRoundRect(frame,cell*.08f,cell*.08f,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.035f);p.setColor(0xff806b47);c.drawRoundRect(frame,cell*.08f,cell*.08f,p);p.setStyle(Paint.Style.FILL);}
        if(!compactUi)drawBoardScenery(c);
        drawPosition(c);
        if(!compactUi){drawSizeToggle(c);drawPetals(c);}
        drawVictorySeal(c);if(inspecting)drawLoupe(c);
        long now=System.currentTimeMillis();boolean moving=motionEnabled()&&now-moveAnimationStart<560;
        if(isShown()&&hasWindowFocus()&&(moving||now<rejectUntil||(!compactUi&&motionEnabled())))postInvalidateDelayed(moving?16:now<rejectUntil?32:100);
    }
    private void drawPosition(Canvas c){
        drawBoardSurface(c);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1f,cell*.027f));p.setColor(GameDesign.GRID);
        for(int y=0;y<19;y++)for(int x=0;x<19;x++)if(validCell(x,y)){
            float cx=ox+x*cellX,cy=oy+y*cellY;
            if(x<18&&validCell(x+1,y))c.drawLine(cx,cy,cx+cellX,cy,p);
            if(y<18&&validCell(x,y+1))c.drawLine(cx,cy,cx,cy+cellY,p);
        }
        // Palace diagonals are visual guides only; original legal cells stay unchanged.
        for(int owner=0;owner<4;owner++){int[] a=toBoard(owner,3,0),b=toBoard(owner,5,2),d=toBoard(owner,5,0),e=toBoard(owner,3,2);c.drawLine(ox+a[0]*cellX,oy+a[1]*cellY,ox+b[0]*cellX,oy+b[1]*cellY,p);c.drawLine(ox+d[0]*cellX,oy+d[1]*cellY,ox+e[0]*cellX,oy+e[1]*cellY,p);}
        // The four boards meet at one restrained boundary mark instead of a large medallion.
        float centerX=ox+9*cellX,centerY=oy+9*cellY;
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.03f);p.setColor(0x508f691f);RectF boundary=new RectF(centerX-cellX*.65f,centerY-cellY*.4f,centerX+cellX*.65f,centerY+cellY*.4f);c.drawRoundRect(boundary,cell*.12f,cell*.12f,p);p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);p.setTextSize(cell*.38f);p.setTypeface(GameDesign.PIECE);p.setColor(0x708f691f);c.drawText("四方",centerX,centerY-(p.ascent()+p.descent())/2,p);
        if(lastFx>=0)feedback.recent(c,ox+lastFx*cellX,oy+lastFy*cellY,ox+lastTx*cellX,oy+lastTy*cellY,cell,pieceDiameter()/2);
        for(int[] m:moves)if(pieceAt(m[0],m[1])==null)feedback.destination(c,ox+m[0]*cellX,oy+m[1]*cellY,cell);
        if(motionEnabled()&&lastTx==animationTx&&lastTy==animationTy&&lastFx==animationFx&&lastFy==animationFy){
            float arrival=(System.currentTimeMillis()-moveAnimationStart-160)/400f;
            feedback.arrival(c,ox+lastTx*cellX,oy+lastTy*cellY,cell,arrival);
        }
        if(pendingSkill>0)for(int[] target:skillTargets())feedback.skillTarget(c,ox+target[0]*cellX,oy+target[1]*cellY,cell);
        for(Piece q:pieces){drawPiece(c,q);if(skillMode&&q.sealed)feedback.seal(c,ox+q.x*cellX,oy+q.y*cellY,cell);}

        for(int[] m:moves)if(pieceAt(m[0],m[1])!=null)feedback.capture(c,ox+m[0]*cellX,oy+m[1]*cellY,cell,pieceDiameter()/2);
        if(System.currentTimeMillis()<rejectUntil){float life=(rejectUntil-System.currentTimeMillis())/320f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.055f);p.setColor(((int)(190*life)<<24)|0x00d94a3b);c.drawCircle(rejectX,rejectY,cell*(.20f+(1-life)*.20f),p);p.setStyle(Paint.Style.FILL);}
    }
    private void startMoveAnimation(){
        animationFx=lastFx;animationFy=lastFy;animationTx=lastTx;animationTy=lastTy;moveAnimationStart=System.currentTimeMillis();
    }
    private void drawLoupe(Canvas c){
        float density=getResources().getDisplayMetrics().density;
        float radius=Math.min(getHeight()*.22f,Math.max(52*density,Math.min(76*density,cell*2.6f)));
        float cx=Math.max(radius+4,Math.min(getWidth()-radius-4,loupeX));
        float cy=loupeY-radius*1.55f;
        if(cy<radius+4)cy=loupeY+radius*1.55f;
        cy=Math.max(radius+4,Math.min(getHeight()-radius-28*density,cy));
        p.setStyle(Paint.Style.FILL);p.setColor(0x50302013);c.drawCircle(cx+2,cy+4,radius+2,p);
        p.setColor(0xffeadcc1);c.drawCircle(cx,cy,radius,p);
        c.save();Path clip=new Path();clip.addCircle(cx,cy,radius,Path.Direction.CW);c.clipPath(clip);
        c.translate(cx,cy);c.scale(2.4f,2.4f);c.translate(-loupeX,-loupeY);drawPosition(c);c.restore();
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*density);p.setColor(0xffbea16a);c.drawCircle(cx,cy,radius,p);
        p.setStrokeWidth(density);p.setColor(0xb0a5792d);c.drawLine(cx-7*density,cy,cx+7*density,cy,p);c.drawLine(cx,cy-7*density,cx,cy+7*density,p);
        p.setStyle(Paint.Style.FILL);int gx=Math.round((loupeX-ox)/cellX),gy=Math.round((loupeY-oy)/cellY);Piece at=pieceAt(gx,gy);
        String caption=validCell(gx,gy)?point(gx,gy)+(at==null?" · 空位":" · "+names[at.owner]+" "+pieceGlyph(at)):"移到棋盘查看";
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);p.setTextSize(12*getResources().getDisplayMetrics().scaledDensity);p.setTextAlign(Paint.Align.CENTER);
        float half=p.measureText(caption)/2+12*density;RectF label=new RectF(cx-half,cy+radius+5*density,cx+half,cy+radius+27*density);
        p.setColor(0xf029251e);c.drawRoundRect(label,11*density,11*density,p);p.setColor(0xffead7ae);c.drawText(caption,cx,label.centerY()-(p.ascent()+p.descent())/2,p);
    }
    private void drawVictorySeal(Canvas c){
        int count=0,winner=-1;for(int i=0;i<4;i++)if(alive[i]){count++;winner=i;}if(count!=1)return;
        float cx=ox+9*cellX,cy=oy+9*cellY;RectF ribbon=new RectF(cx-cell*4.3f,cy-cell*1.3f,cx+cell*4.3f,cy+cell*1.3f);
        p.setColor(0xf022303d);c.drawRoundRect(ribbon,cell*.35f,cell*.35f,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.035f);p.setColor(0xffd9ba79);c.drawRoundRect(ribbon,cell*.35f,cell*.35f,p);p.setStyle(Paint.Style.FILL);
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(cell*.70f);p.setColor(0xfff0d9a5);c.drawText(names[winner]+"获胜",cx,cy,p);p.setTextSize(cell*.37f);p.setColor(0xffc4c3b9);String[] heroes={"红尘弈心","花间一弈","清风入弈","金陵弈梦"};c.drawText(heroes[winner]+" · 一局定乾坤",cx,cy+cell*.72f,p);
    }
    private void drawBoardScenery(Canvas c){
        // Restrained line art in the four unused corners, covered by the real board surface.
        float[][] corners={{ox,oy},{ox+14*cellX,oy},{ox,oy+14*cellY},{ox+14*cellX,oy+14*cellY}};
        for(int k=0;k<corners.length;k++){float x=corners[k][0],y=corners[k][1];p.setStyle(Paint.Style.STROKE);p.setColor(0x287c715a);p.setStrokeWidth(Math.max(1,cell*.025f));Path mountain=new Path();mountain.moveTo(x,y+3.8f*cellY);mountain.cubicTo(x+cellX,y+2.2f*cellY,x+1.4f*cellX,y+4*cellY,x+2.5f*cellX,y+2.6f*cellY);mountain.quadTo(x+3*cellX,y+2*cellY,x+4*cellX,y+3.6f*cellY);c.drawPath(mountain,p);p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(android.graphics.Typeface.create("serif",android.graphics.Typeface.NORMAL));p.setTextSize(cell*.38f);p.setColor(0x657f7156);String[] words={"","听 风","落 子","无 悔"};c.drawText(words[k],x+2*cellX,y+2.0f*cellY,p);}
    }
    private void drawSizeToggle(Canvas c){
        float h=Math.min(cellY*1.5f,Math.max(getResources().getDisplayMetrics().density*28,cell*1.2f)),w=cellX*4.1f;sizeToggle.set(ox,oy+cellY*.25f,ox+w,oy+cellY*.25f+h);
        p.setColor(0xff40362a);c.drawRoundRect(sizeToggle,h*.45f,h*.45f,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,cell*.025f));p.setColor(0xff8c7954);c.drawRoundRect(sizeToggle,h*.45f,h*.45f,p);p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(cell*.43f);p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);p.setColor(0xffdcccaa);c.drawText(largePieces?"大字棋子  ⇄":"标准棋子  ⇄",sizeToggle.centerX(),sizeToggle.centerY()-(p.ascent()+p.descent())/2,p);
        p.setTextSize(Math.min(cell*.45f,11*getResources().getDisplayMetrics().scaledDensity));p.setColor(0xffb1a080);
        String hint=selected==null?"长按棋盘 · 局部放大":pieceGlyph(selected)+" · "+moves.size()+" 个合法落点";
        c.drawText(hint,sizeToggle.centerX(),sizeToggle.bottom+cellY*.7f,p);
    }
    private void drawBoardSurface(Canvas c){
        float l=ox-cellX*.45f,r=ox+18.45f*cellX,t=oy-cellY*.45f,b=oy+18.45f*cellY;
        float x5=ox+4.55f*cellX,x14=ox+13.45f*cellX,y5=oy+4.55f*cellY,y14=oy+13.45f*cellY;
        Path cross=new Path();cross.moveTo(x5,t);cross.lineTo(x14,t);cross.lineTo(x14,y5);cross.lineTo(r,y5);cross.lineTo(r,y14);cross.lineTo(x14,y14);cross.lineTo(x14,b);cross.lineTo(x5,b);cross.lineTo(x5,y14);cross.lineTo(l,y14);cross.lineTo(l,y5);cross.lineTo(x5,y5);cross.close();
        c.save();c.translate(0,cell*.08f);p.setColor(0x88202a2e);c.drawPath(cross,p);c.restore();p.setShader(new LinearGradient(0,t,0,b,GameDesign.IVORY,GameDesign.IVORY_SHADE,Shader.TileMode.CLAMP));p.setAlpha(255);c.drawPath(cross,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.07f);p.setColor(0xff66503b);c.drawPath(cross,p);p.setStrokeWidth(cell*.022f);p.setColor(0xffac9463);c.drawPath(cross,p);p.setStyle(Paint.Style.FILL);
        float band=Math.max(2,cell*.12f);p.setColor(0xff365f78);c.drawRect(x5,t,x14,t+band,p);p.setColor(0xff376c50);c.drawRect(l,y5,l+band,y14,p);p.setColor(0xffa87527);c.drawRect(r-band,y5,r,y14,p);p.setColor(0xffb22f2a);c.drawRect(x5,b-band,x14,b,p);
        c.save();c.clipPath(cross);p.setStrokeWidth(Math.max(1,cell*.016f));p.setColor(0x0b5f3d28);for(int i=0;i<10;i++){float yy=oy+(i*61%180)*cellY/10f;c.drawLine(l,yy,r,yy+cellY*.10f,p);}c.restore();drawBoardOrnaments(c);
    }
    private void drawBoardOrnaments(Canvas c){float[][] pts={{4.65f,4.65f},{13.35f,4.65f},{4.65f,13.35f},{13.35f,13.35f}};for(float[] q:pts){float x=ox+q[0]*cellX,y=oy+q[1]*cellY,s=cell*.14f;Path d=new Path();d.moveTo(x,y-s);d.lineTo(x+s,y);d.lineTo(x,y+s);d.lineTo(x-s,y);d.close();p.setColor(0x99c28c38);c.drawPath(d,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.025f);p.setColor(0xffffe0a1);c.drawPath(d,p);p.setStyle(Paint.Style.FILL);}}
    private void drawPiece(Canvas c,Piece q){float x=ox+q.x*cellX,y=oy+q.y*cellY;
        long elapsed=System.currentTimeMillis()-moveAnimationStart;
        if(motionEnabled()&&elapsed>=0&&elapsed<160&&q.x==animationTx&&q.y==animationTy&&lastFx==animationFx&&lastFy==animationFy&&lastTx==animationTx&&lastTy==animationTy){float t=elapsed/160f;float remain=(1-t)*(1-t)*(1-t);x+=(animationFx-animationTx)*cellX*remain;y+=(animationFy-animationTy)*cellY*remain;}
        // Standard remains 70%; the optional large-text preset uses 84%, without moving centres.
        float radius=pieceDiameter()/2f;
        p.setStyle(Paint.Style.FILL);p.setColor(0x38221810);c.drawCircle(x+cell*.025f,y+cell*.045f,radius,p);
        p.setColor(0xffb8a385);c.drawCircle(x,y+cell*.025f,radius,p);
        p.setShader(new LinearGradient(x,y-radius,x,y+radius,0xfff6eedc,0xffe8dbc1,Shader.TileMode.CLAMP));c.drawCircle(x,y,radius,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.03f);p.setColor(colors[q.owner]);c.drawCircle(x,y,radius*.90f,p);
        if(q==selected||q==skillSource)feedback.selection(c,x,y,cell,radius);
        p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.LEFT);p.setTypeface(GameDesign.PIECE);p.setFakeBoldText(false);p.setTextSize(cell*(largePieces?.65f:.552f));p.setColor(colors[q.owner]);
        String glyph=pieceGlyph(q);p.getTextBounds(glyph,0,glyph.length(),glyphBounds);
        float limit=radius*1.42f;float extent=Math.max(glyphBounds.width(),glyphBounds.height());if(extent>limit){p.setTextSize(p.getTextSize()*limit/extent);p.getTextBounds(glyph,0,glyph.length(),glyphBounds);}
        c.drawText(glyph,x-glyphBounds.exactCenterX(),y-glyphBounds.exactCenterY(),p);
        p.setFakeBoldText(false);p.clearShadowLayer();
    }
    private void drawAmbientGlow(Canvas c){float pulse=.72f+.28f*(float)Math.sin(animationPhase*Math.PI*2);float cx,cy;if(turn==0){cx=getWidth()*.5f;cy=getHeight();}else if(turn==1){cx=0;cy=getHeight()*.5f;}else if(turn==2){cx=getWidth()*.5f;cy=0;}else{cx=getWidth();cy=getHeight()*.5f;}int col=(colors[turn]&0x00ffffff)|((int)(58*pulse)<<24);p.setShader(new RadialGradient(cx,cy,Math.max(getWidth(),getHeight())*.55f,col,0x00000000,Shader.TileMode.CLAMP));c.drawRect(0,0,getWidth(),getHeight(),p);p.setShader(null);}
    private void drawMoveTrail(Canvas c,float pulse){float x1=ox+lastFx*cellX,y1=oy+lastFy*cellY,x2=ox+lastTx*cellX,y2=oy+lastTy*cellY;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(cell*.07f);p.setColor(0x66ffd875);c.drawLine(x1,y1,x2,y2,p);float t=(animationPhase*3)%1f;p.setStyle(Paint.Style.FILL);p.setColor(0xeefff0a0);c.drawCircle(x1+(x2-x1)*t,y1+(y2-y1)*t,cell*(.07f+pulse*.025f),p);}
    private void drawPetals(Canvas c){for(int i=0;i<10;i++){float t=(animationPhase+i*.137f)%1f;float x=((i*127f)%Math.max(1,getWidth()))+(float)Math.sin((t+i)*6.28f)*cell*.6f;float y=-cell+t*(getHeight()+cell*2);float gx=(x-ox)/cellX,gy=(y-oy)/cellY;if(gx>=-.6f&&gx<=18.6f&&gy>=-.6f&&gy<=18.6f&&((gx>=4.4f&&gx<=13.6f)||(gy>=4.4f&&gy<=13.6f)))continue;float size=cell*(.055f+(i%3)*.018f);p.setColor(i%2==0?0x55ff9c8d:0x44ffd0ae);c.save();c.rotate(t*220+i*31,x,y);c.drawOval(new RectF(x-size*1.7f,y-size*.65f,x+size*1.7f,y+size*.65f),p);c.restore();}}
    private String label(char t){switch(t){case'C':return"车";case'M':return"马";case'X':return"象";case'S':return"士";case'J':return"将";case'P':return"炮";default:return"兵";}}
    boolean isSkillMode(){return skillMode;}
    void setSkillMode(boolean enabled){skillMode=enabled;cancelSkill();}
    void setSkillRequester(SkillRequester value){skillRequester=value;cancelSkill();}
    int lastSkill(){return lastSkill;}
    int lastSkillActor(){return lastActor;}
    private void dealSkills(){
        java.util.List<Integer> deck=new ArrayList<>();for(int id=1;id<=SkillRules.COUNT;id++)deck.add(id);
        java.util.Random random=new java.util.Random(dealSeed^((long)(completedRounds/10)*0x9e3779b97f4a7c15L));java.util.Collections.shuffle(deck,random);
        java.util.Arrays.fill(skillUse,0);
        for(int owner=0;owner<4;owner++){java.util.Arrays.fill(skillCharges[owner],0);for(int slot=0;slot<SkillRules.HAND_SIZE;slot++){int id=skillMode?deck.get(slot*4+owner):0;skillHands[owner][slot]=id;if(id>0)skillCharges[owner][id]=1+random.nextInt(3);}}
    }
    int skillViewer(){return localPlayer>=0&&localPlayer<4?localPlayer:turn;}
    int[] skillHand(int owner){return owner>=0&&owner<4?skillHands[owner].clone():new int[0];}
    boolean hasSkill(int owner,int id){if(owner<0||owner>3||id<1||id>SkillRules.COUNT)return false;for(int held:skillHands[owner])if(held==id)return true;return false;}
    int skillRemaining(int owner,int id){return hasSkill(owner,id)?skillCharges[owner][id]:0;}
    boolean skillUsed(int owner,int id){return skillRemaining(owner,id)==0;}
    int skillGeneration(){return completedRounds/10;}
    int skillRounds(){return completedRounds;}
    int selectedSkill(){return pendingSkill;}
    boolean skillAvailable(int id){
        if(!skillMode||!canAct()||skillUsed(turn,id))return false;
        for(int y=0;y<19;y++)for(int x=0;x<19;x++)if(validCell(x,y)){
            if(!SkillRules.twoStep(id)){if(skillLegal(id,x,y,x,y))return true;}
            else {Piece q=pieceAt(x,y);if(id==30?enemySkillPiece(q):ownSkillPiece(q))for(int ty=0;ty<19;ty++)for(int tx=0;tx<19;tx++)if(skillLegal(id,x,y,tx,ty))return true;}
        }return false;
    }
    boolean canAct(){int count=0;for(boolean a:alive)if(a)count++;return count>1&&alive[turn]&&(localPlayer<0||localPlayer==turn);}
    boolean beginSkill(int id){
        if(!skillMode||!canAct()||skillUsed(turn,id))return false;
        pendingSkill=id;skillSource=null;selected=null;moves.clear();
        if(skillTargets().isEmpty()){pendingSkill=0;return false;}invalidate();announce();return true;
    }
    void cancelSkill(){pendingSkill=0;skillSource=null;selected=null;moves.clear();invalidate();announce();}
    private boolean ownSkillPiece(Piece q){return q!=null&&q.owner==turn&&q.type!='J'&&!q.sealed;}
    private boolean hostile(int actor,int other){return actor!=other&&!diplomacy.allied(actor,other);}
    private boolean enemySkillPiece(Piece q){return q!=null&&hostile(turn,q.owner)&&q.type!='J'&&!q.sealed;}
    private boolean restoration(int id,int tx,int ty){
        char type=SkillRules.restoreType(id);int count=0;for(Piece q:pieces)if(q.owner==turn&&q.type==type)count++;
        if(type==0||count>=(type=='B'?5:2)||pieceAt(tx,ty)!=null)return false;
        int[] files=type=='B'?new int[]{0,2,4,6,8}:type=='C'?new int[]{0,8}:type=='M'||type=='P'?new int[]{1,7}:type=='S'?new int[]{3,5}:new int[]{2,6};
        int depth=type=='B'?3:type=='P'?2:0;
        for(int file:files){int[] pos=toBoard(turn,file,depth);if(pos[0]==tx&&pos[1]==ty)return true;}return false;
    }
    private boolean skillLegal(int id,int sx,int sy,int tx,int ty){
        if(id<1||id>SkillRules.COUNT||!validCell(sx,sy)||!validCell(tx,ty))return false;
        Piece source=pieceAt(sx,sy),target=pieceAt(tx,ty);int dx=Math.abs(tx-sx),dy=Math.abs(ty-sy),distance=Math.max(dx,dy);
        if(!SkillRules.twoStep(id)){
            if(sx!=tx||sy!=ty)return false;
            if(SkillRules.restoreType(id)!=0)return restoration(id,tx,ty);
            if(id==4)return enemySkillPiece(target);
            if(id==25){if(target==null||!hostile(turn,target.owner)||target.type=='J')return false;for(Piece q:pieces)if(hostile(turn,q.owner)&&q.type!='J'&&!q.sealed&&Math.max(Math.abs(q.x-tx),Math.abs(q.y-ty))<=1)return true;return false;}
            if(target==null||target.owner!=turn||target.type=='J')return false;
            if(id==26)return target.sealed;
            if(id==27){for(Piece q:pieces)if(q.owner==turn&&q.sealed)return true;return false;}
            return (id==28||id==29)&&!target.sealed;
        }
        if(id==30)return enemySkillPiece(source)&&target==null&&distance==1;
        if(!ownSkillPiece(source)||distance==0)return false;
        if(id==2||id==10)return ownSkillPiece(target)&&(id==10||dx+dy<=4);
        if(id==11||id==12)return enemySkillPiece(target)&&(id==12||dx+dy<=3);
        if(id==7||id==8||id==9)return enemySkillPiece(target)&&(id==7?distance==1:id==8?distance<=2:(dx==0||dy==0)&&distance<=3);
        if(id==18||id==19)return (target==null||enemySkillPiece(target))&&distance<=2&&(id==18?dx==dy:dx==0||dy==0);
        if(target!=null)return false;
        if(id==1)return distance==1;
        if(id==5)return distance<=2;
        if(id==6)return true;
        int[] from=toLocal(turn,sx,sy),to=toLocal(turn,tx,ty);
        if(id==13)return to[1]==0&&to[0]>=0&&to[0]<=8;
        if(id==14)return from[0]==to[0]&&to[1]-from[1]>=1&&to[1]-from[1]<=3;
        if(id==15)return from[0]==to[0]&&from[1]-to[1]>=1&&from[1]-to[1]<=3;
        if(id==16)return from[1]==to[1]&&Math.abs(from[0]-to[0])<=2;
        return id==17&&dx*dy==2;
    }
    private List<int[]> skillTargets(){
        List<int[]> targets=new ArrayList<>();
        for(int y=0;y<19;y++)for(int x=0;x<19;x++)if(validCell(x,y)){
            boolean yes=false;
            if(!SkillRules.twoStep(pendingSkill))yes=skillLegal(pendingSkill,x,y,x,y);
            else if(skillSource!=null)yes=skillLegal(pendingSkill,skillSource.x,skillSource.y,x,y);
            else {Piece q=pieceAt(x,y);if(pendingSkill==30?enemySkillPiece(q):ownSkillPiece(q)){
                search:for(int ty=0;ty<19;ty++)for(int tx=0;tx<19;tx++)if(skillLegal(pendingSkill,x,y,tx,ty)){yes=true;break search;}
            }}
            if(yes)targets.add(new int[]{x,y});
        }return targets;
    }
    private void handleSkillTap(int x,int y){
        boolean legal=false;for(int[] target:skillTargets())if(target[0]==x&&target[1]==y)legal=true;
        if(!legal){reject(ox+x*cellX,oy+y*cellY);return;}
        if(SkillRules.twoStep(pendingSkill)&&skillSource==null){skillSource=pieceAt(x,y);invalidate();announce();return;}
        int id=pendingSkill,sx=skillSource==null?x:skillSource.x,sy=skillSource==null?y:skillSource.y;
        pendingSkill=0;skillSource=null;
        if(skillRequester!=null)skillRequester.request(turn,id,sx,sy,x,y);else trySkill(turn,id,sx,sy,x,y);
        invalidate();announce();
    }
    boolean trySkill(int owner,int id,int sx,int sy,int tx,int ty){
        int live=0;for(boolean a:alive)if(a)live++;
        if(!skillMode||live<2||owner!=turn||!alive[owner]||skillUsed(owner,id)||!skillLegal(id,sx,sy,tx,ty))return false;
        Piece source=pieceAt(sx,sy),target=pieceAt(tx,ty);
        save();if(--skillCharges[owner][id]==0)skillUse[owner]|=1<<(id-1);lastSkill=id;lastActor=owner;lastFx=sx;lastFy=sy;lastTx=tx;lastTy=ty;
        char restored=SkillRules.restoreType(id);
        if(restored!=0)pieces.add(new Piece(owner,restored,tx,ty));
        else if(id==4||id==25){for(Piece q:pieces)if(q.type!='J'&&hostile(owner,q.owner)&&(id==4?q==target:Math.max(Math.abs(q.x-tx),Math.abs(q.y-ty))<=1))q.sealed=true;}
        else if(id==26)target.sealed=false;
        else if(id==27){for(Piece q:pieces)if(q.owner==owner)q.sealed=false;}
        else if(id==28)pieces.remove(target);
        else if(id!=29){
            if(id==2||id==10||id==11||id==12){target.x=sx;target.y=sy;}
            else if(target!=null)pieces.remove(target);
            source.x=tx;source.y=ty;startMoveAnimation();
        }
        selected=null;pendingSkill=0;skillSource=null;moves.clear();tone(GameSound.SELECT);
        if(moveObserver!=null)moveObserver.onMove(skillLog());nextTurn();invalidate();announce();return true;
    }
    String skillLog(){return names[lastActor]+" · "+SkillRules.name(lastSkill)+" "+point(lastFx,lastFy)+(lastFx==lastTx&&lastFy==lastTy?"":" → "+point(lastTx,lastTy));}
    private String pieceGlyph(Piece q){return q.type=='J'&&q.owner==0?"帅":q.type=='B'&&q.owner!=0?"卒":label(q.type);}
    @Override public boolean onTouchEvent(MotionEvent e){
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                touchDown=true;tapEligible=true;inspecting=false;touchDownX=e.getX();touchDownY=e.getY();
                if(!sizeToggle.contains(touchDownX,touchDownY))postDelayed(inspectHold,450);return true;
            case MotionEvent.ACTION_MOVE:
                if(inspecting){loupeX=e.getX();loupeY=e.getY();invalidate();}
                else if(Math.hypot(e.getX()-touchDownX,e.getY()-touchDownY)>android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop()){tapEligible=false;removeCallbacks(inspectHold);}
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_DOWN:
                touchDown=false;tapEligible=false;inspecting=false;removeCallbacks(inspectHold);invalidate();return true;
            case MotionEvent.ACTION_UP:
                touchDown=false;removeCallbacks(inspectHold);if(inspecting||!tapEligible){inspecting=false;invalidate();return true;}break;
            default:return true;
        }
        performClick();
        if(sizeToggle.contains(e.getX(),e.getY())){togglePieceSize();performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);invalidate();return true;}
        if(liveCount()<2)return true;
        int x=Math.round((e.getX()-ox)/cellX),y=Math.round((e.getY()-oy)/cellY); if(!validCell(x,y)){reject(e.getX(),e.getY());return true;}
        if(pendingSkill>0){handleSkillTap(x,y);return true;}
        Piece hit=pieceAt(x,y);
        if(selected!=null&&containsMove(x,y)){if(requester!=null)requester.request(turn,selected.x,selected.y,x,y);else moveSelected(x,y);return true;}
        if(hit!=null&&hit.owner==turn&&(localPlayer<0||localPlayer==turn)){selected=hit;moves=legalMoves(hit);tone(GameSound.SELECT);performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);}else{selected=null;moves.clear();reject(e.getX(),e.getY());}
        invalidate();return true;
    }
    private void reject(float x,float y){rejectX=x;rejectY=y;rejectUntil=System.currentTimeMillis()+320;performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);invalidate();}
    @Override public boolean performClick(){super.performClick();return true;}
    private void moveSelected(int x,int y){save();lastSkill=0;int movingOwner=selected.owner;String moving=pieceGlyph(selected);lastFx=selected.x;lastFy=selected.y;lastTx=x;lastTy=y;Piece victim=pieceAt(x,y);String captured="";if(victim!=null){captured=" 吃 "+pieceGlyph(victim);pieces.remove(victim);tone(GameSound.CAPTURE);if(victim.type=='J')eliminate(victim.owner);}else tone(GameSound.MOVE);
        selected.x=x;selected.y=y;selected=null;moves.clear();startMoveAnimation();if(moveObserver!=null)moveObserver.onMove(names[movingOwner]+" · "+moving+" "+point(lastFx,lastFy)+" → "+point(x,y)+captured);nextTurn();invalidate();announce(); }
    private String point(int x,int y){return String.valueOf((char)('A'+x))+(19-y);}
    private void tone(int kind){if(soundEnabled)sounds.play(kind);}
    boolean tryNetworkMove(int owner,int sx,int sy,int tx,int ty){if(owner<0||owner>3||owner!=turn||!alive[owner]||liveCount()<2)return false;Piece q=pieceAt(sx,sy);if(q==null||q.owner!=owner)return false;selected=q;moves=legalMoves(q);if(!containsMove(tx,ty)){selected=null;moves.clear();return false;}moveSelected(tx,ty);return true;}
    int localSeat(){return localPlayer;}
    int liveCount(){int count=0;for(boolean value:alive)if(value)count++;return count;}
    boolean allied(int a,int b){return diplomacy.allied(a,b);}
    boolean treatyBroken(int a,int b){return diplomacy.broken(a,b);}
    boolean allianceOffered(int from,int to){return diplomacy.offered(from,to);}
    int diplomacySerial(){return diplomacy.serial;}
    String allianceLabel(int actor){StringBuilder text=new StringBuilder();String[] shortNames={"红","绿","蓝","金"};for(int i=0;i<4;i++)if(allied(actor,i))text.append(shortNames[i]);if(text.length()>0)return "盟·"+text;for(int i=0;i<4;i++)if(allianceOffered(i,actor))return "收到邀约";return "";}
    String diplomacyLog(){if(diplomacy.serial==0)return "";int a=diplomacy.lastOwner,b=diplomacy.lastTarget;String prefix=names[a]+" · ";switch(diplomacy.lastKind){case 0:return prefix+"向"+names[b]+"发起结盟";case 1:return prefix+"与"+names[b]+"缔结盟约";case 2:return prefix+"婉拒"+names[b]+"的盟约";case 3:return prefix+"撕毁与"+names[b]+"的盟约，本局不再结盟";default:return prefix+"投降退场";}}
    boolean canDiplomacy(int actor,int action,int target){return actor>=0&&actor<4&&alive[actor]&&liveCount()>1&&(action==DiplomacyState.SURRENDER||target>=0&&target<4&&alive[target])&&diplomacy.can(actor,action,target);}
    boolean tryDiplomacy(int actor,int action,int target){
        if(!canDiplomacy(actor,action,target))return false;
        save();diplomacy.apply(actor,action,target);selected=null;pendingSkill=0;skillSource=null;moves.clear();
        if(action==DiplomacyState.SURRENDER){eliminate(actor);if(actor==turn){nextTurn();}if(liveCount()==1){for(int i=0;i<4;i++)if(alive[i])turn=i;}}
        if(moveObserver!=null)moveObserver.onMove(diplomacyLog());invalidate();announce();return true;
    }
    String exportState(){
        StringBuilder b=new StringBuilder().append(turn).append(',').append(lastFx).append(',').append(lastFy).append(',').append(lastTx).append(',').append(lastTy);
        if(skillMode){b.append(",K4");for(int used:skillUse)b.append(',').append(used);b.append(',').append(lastSkill).append(',').append(lastActor);for(int[] hand:skillHands)for(int held:hand)b.append(',').append(held);for(int owner=0;owner<4;owner++)for(int id:skillHands[owner])b.append(',').append(skillCharges[owner][id]);b.append(',').append(completedRounds).append(',').append(roundMask).append(',').append(dealSeed);}
        b.append('|');for(Piece q:pieces){b.append(q.owner).append(',').append(q.type).append(',').append(q.x).append(',').append(q.y);if(skillMode)b.append(',').append(q.sealed?1:0);b.append(';');}if(diplomacy.serial>0)b.append('|').append(diplomacy.encode());return b.toString();
    }
    void importState(String state){try{
        String[] top=state.split("\\|",-1);if(top.length<2||top.length>3)return;DiplomacyState incomingDiplomacy=top.length==3?DiplomacyState.decode(top[2]):new DiplomacyState();String[] h=top[0].split(",");int newTurn=Integer.parseInt(h[0]);if(newTurn<0||newTurn>3)return;
        boolean variant=h.length==15+8*SkillRules.HAND_SIZE&&"K4".equals(h[5]);if(!variant&&h.length!=5)return;
        int[] uses=new int[4];int[][] hands=new int[4][SkillRules.HAND_SIZE],charges=new int[4][SkillRules.COUNT+1];int rounds=0,mask=0;long seed=0;
        if(variant)for(int owner=0;owner<4;owner++){int heldMask=0;for(int slot=0;slot<SkillRules.HAND_SIZE;slot++){int id=Integer.parseInt(h[12+owner*SkillRules.HAND_SIZE+slot]);if(id<1||id>SkillRules.COUNT||(heldMask&(1<<(id-1)))!=0)return;hands[owner][slot]=id;heldMask|=1<<(id-1);}uses[owner]=Integer.parseInt(h[6+owner]);if((uses[owner]&~heldMask)!=0)return;}
        if(variant){for(int owner=0;owner<4;owner++)for(int slot=0;slot<SkillRules.HAND_SIZE;slot++){int id=hands[owner][slot],n=Integer.parseInt(h[28+owner*4+slot]);if(n<0||n>3||((uses[owner]&(1<<(id-1)))!=0)!=(n==0))return;charges[owner][id]=n;}rounds=Integer.parseInt(h[44]);mask=Integer.parseInt(h[45]);seed=Long.parseLong(h[46]);if(rounds<0||rounds>1000000||mask<0||mask>15)return;}
        int action=variant?Integer.parseInt(h[10]):0,actor=variant?Integer.parseInt(h[11]):0;
        if(action<0||action>SkillRules.COUNT||actor<0||actor>3)return;
        int fx=Integer.parseInt(h[1]),fy=Integer.parseInt(h[2]),tx=Integer.parseInt(h[3]),ty=Integer.parseInt(h[4]);
        List<Piece> incoming=new ArrayList<>();boolean[][] occupied=new boolean[19][19];
        if(top.length>1)for(String item:top[1].split(";")){if(item.isEmpty())continue;String[] a=item.split(",");int o=Integer.parseInt(a[0]),x=Integer.parseInt(a[2]),y=Integer.parseInt(a[3]);char type=a[1].charAt(0);if(o<0||o>3||!validCell(x,y)||occupied[x][y]||"CMXSJPB".indexOf(type)<0)return;occupied[x][y]=true;Piece q=new Piece(o,type,x,y);q.sealed=variant&&a.length>4&&"1".equals(a[4]);if(q.type=='J'&&q.sealed)return;incoming.add(q);}
        boolean[] present=new boolean[4];for(Piece q:incoming)if(q.type=='J')present[q.owner]=true;for(int a=0;a<4;a++)for(int b=a+1;b<4;b++)if((!present[a]||!present[b])&&(incomingDiplomacy.allied(a,b)||incomingDiplomacy.offered(a,b)||incomingDiplomacy.offered(b,a)))return;
        diplomacy=incomingDiplomacy;turn=newTurn;lastFx=fx;lastFy=fy;lastTx=tx;lastTy=ty;skillMode=variant;System.arraycopy(uses,0,skillUse,0,4);for(int owner=0;owner<4;owner++)System.arraycopy(hands[owner],0,skillHands[owner],0,SkillRules.HAND_SIZE);for(int owner=0;owner<4;owner++)System.arraycopy(charges[owner],0,skillCharges[owner],0,SkillRules.COUNT+1);completedRounds=rounds;roundMask=mask;dealSeed=seed;lastSkill=action;lastActor=actor;
        pieces.clear();pieces.addAll(incoming);for(int i=0;i<4;i++)alive[i]=false;for(Piece q:pieces)if(q.type=='J')alive[q.owner]=true;selected=null;pendingSkill=0;skillSource=null;moves.clear();invalidate();announce();tone(GameSound.MOVE);
    }catch(Exception ignored){}}
    private void eliminate(int owner){diplomacy.eliminate(owner);alive[owner]=false;for(int i=pieces.size()-1;i>=0;i--)if(pieces.get(i).owner==owner)pieces.remove(i);}
    private void nextTurn(){if(skillMode)for(Piece q:pieces)if(q.owner==turn)q.sealed=false;int count=0;for(boolean a:alive)if(a)count++;if(count<=1)return;if(skillMode){roundMask|=1<<turn;int liveMask=0;for(int i=0;i<4;i++)if(alive[i])liveMask|=1<<i;if((roundMask&liveMask)==liveMask){completedRounds++;roundMask=0;if(completedRounds%10==0)dealSkills();}}do{turn=(turn+1)%4;}while(!alive[turn]);}
    private void save(){List<Piece> copy=new ArrayList<>();for(Piece q:pieces)copy.add(q.copy());Snapshot snapshot=new Snapshot(copy,turn,lastFx,lastFy,lastTx,lastTy);snapshot.skillUse=skillUse.clone();snapshot.hands=new int[4][];for(int i=0;i<4;i++)snapshot.hands[i]=skillHands[i].clone();snapshot.remaining=new int[4][];for(int i=0;i<4;i++)snapshot.remaining[i]=skillCharges[i].clone();snapshot.seed=dealSeed;snapshot.rounds=completedRounds;snapshot.roundMask=roundMask;snapshot.skill=lastSkill;snapshot.actor=lastActor;snapshot.diplomacy=diplomacy.copy();history.add(snapshot);}
    int undoCount(){return history.size();}
    void undo(){undoSteps(1);}
    boolean undoSteps(int count){
        if(count<1||count>history.size())return false;
        Snapshot s=history.get(history.size()-count);
        for(int i=0;i<count;i++)history.remove(history.size()-1);
        int brokenTreaties=diplomacy.broken;diplomacy=s.diplomacy.copy();diplomacy.retainBroken(brokenTreaties);pieces.clear();for(Piece q:s.pieces)pieces.add(q.copy());turn=s.turn;
        lastFx=s.fx;lastFy=s.fy;lastTx=s.tx;lastTy=s.ty;System.arraycopy(s.skillUse,0,skillUse,0,4);for(int i=0;i<4;i++)System.arraycopy(s.hands[i],0,skillHands[i],0,SkillRules.HAND_SIZE);for(int i=0;i<4;i++)System.arraycopy(s.remaining[i],0,skillCharges[i],0,SkillRules.COUNT+1);dealSeed=s.seed;completedRounds=s.rounds;roundMask=s.roundMask;lastSkill=s.skill;lastActor=s.actor;pendingSkill=0;skillSource=null;
        for(int i=0;i<4;i++)alive[i]=false;for(Piece q:pieces)if(q.type=='J')alive[q.owner]=true;
        selected=null;moves.clear();rejectUntil=0;invalidate();announce();return true;
    }
    private void announce(){if(listener==null)return;int count=0,last=-1;for(int i=0;i<4;i++)if(alive[i]){count++;last=i;}if(count==1){listener.onStatus(names[last]+"获胜！");tone(GameSound.WIN);}else if(pendingSkill>0)listener.onStatus(SkillRules.prompt(pendingSkill,skillSource!=null));else listener.onStatus(names[turn]+(skillMode?" · 秘术行动":"行动")+((localPlayer>=0&&localPlayer!=turn)?" · 等待对手":" · 请选择棋子"));}
    @Override protected void onDetachedFromWindow(){removeCallbacks(inspectHold);touchDown=false;inspecting=false;sounds.release();super.onDetachedFromWindow();}
    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);if(visibility!=VISIBLE){if(inspectHold!=null)removeCallbacks(inspectHold);touchDown=false;inspecting=false;}}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(!focused){removeCallbacks(inspectHold);touchDown=false;inspecting=false;invalidate();}}
    private Piece pieceAt(int x,int y){for(Piece q:pieces)if(q.x==x&&q.y==y)return q;return null;}
    private boolean containsMove(int x,int y){for(int[] m:moves)if(m[0]==x&&m[1]==y)return true;return false;}
    private List<int[]> legalMoves(Piece q){List<int[]> r=new ArrayList<>();if(skillMode&&q.sealed)return r;
        if(q.type=='C'||q.type=='P') rays(q,r,q.type=='P');
        else if(q.type=='M') horse(q,r);
        else if(q.type=='X') diagonal(q,r,2);
        else if(q.type=='S') diagonal(q,r,1);
        else if(q.type=='J') general(q,r);
        else pawn(q,r);
        return r;
    }
    private void addIf(Piece q,List<int[]>r,int x,int y){if(validCell(x,y)){Piece at=pieceAt(x,y);if(at==null||hostile(q.owner,at.owner))r.add(new int[]{x,y});}}
    private void rays(Piece q,List<int[]>r,boolean cannon){int[][]d={{1,0},{-1,0},{0,1},{0,-1}};for(int[]v:d){boolean screen=false;for(int n=1;n<19;n++){int x=q.x+v[0]*n,y=q.y+v[1]*n;if(!validCell(x,y))break;Piece at=pieceAt(x,y);if(!cannon){if(at==null)r.add(new int[]{x,y});else{if(hostile(q.owner,at.owner))r.add(new int[]{x,y});break;}}else if(!screen){if(at==null)r.add(new int[]{x,y});else screen=true;}else if(at!=null){if(hostile(q.owner,at.owner))r.add(new int[]{x,y});break;}}}}
    private void horse(Piece q,List<int[]>r){int[][]a={{2,1,1,0},{2,-1,1,0},{-2,1,-1,0},{-2,-1,-1,0},{1,2,0,1},{-1,2,0,1},{1,-2,0,-1},{-1,-2,0,-1}};for(int[]v:a)if(pieceAt(q.x+v[2],q.y+v[3])==null)addIf(q,r,q.x+v[0],q.y+v[1]);}
    private void diagonal(Piece q,List<int[]>r,int n){int[][]d={{1,1},{1,-1},{-1,1},{-1,-1}};for(int[]v:d){int x=q.x+v[0]*n,y=q.y+v[1]*n;if(n==2&&pieceAt(q.x+v[0],q.y+v[1])!=null)continue;if(q.type=='X'&&!ownZone(q.owner,x,y))continue;if(q.type=='S'&&!palace(q.owner,x,y))continue;addIf(q,r,x,y);}}
    private void general(Piece q,List<int[]>r){int[][]d={{1,0},{-1,0},{0,1},{0,-1}};for(int[]v:d){int x=q.x+v[0],y=q.y+v[1];if(palace(q.owner,x,y))addIf(q,r,x,y);}}
    private void pawn(Piece q,List<int[]>r){int[] f=forward(q.owner);addIf(q,r,q.x+f[0],q.y+f[1]);int[] l=toLocal(q.owner,q.x,q.y);if(l[1]>=5){addIf(q,r,q.x+f[1],q.y-f[0]);addIf(q,r,q.x-f[1],q.y+f[0]);}}
    private int[] forward(int o){if(o==0)return new int[]{0,-1};if(o==1)return new int[]{1,0};if(o==2)return new int[]{0,1};return new int[]{-1,0};}
    private boolean ownZone(int o,int x,int y){int[] l=toLocal(o,x,y);return l[0]>=0&&l[0]<=8&&l[1]>=0&&l[1]<=4;}
    private boolean palace(int o,int x,int y){int[] l=toLocal(o,x,y);return l[0]>=3&&l[0]<=5&&l[1]>=0&&l[1]<=2;}
}







