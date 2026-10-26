package com.dorm.fourchess;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.text.*;
import android.view.*;
import android.widget.Button;

/** Compact native skill buttons. The board owns all charges, eligibility and targeting. */
final class SkillRackView extends ViewGroup {
    interface Listener { void select(int slot); }
    private Listener listener;
    private boolean stacked;
    private final Tile[] tiles=new Tile[SkillRules.HAND_SIZE];
    SkillRackView(Context c){super(c);for(int i=0;i<tiles.length;i++){final int slot=i;Tile tile=new Tile(c);tile.setOnClickListener(v->{if(listener!=null)listener.select(slot);});tiles[i]=tile;addView(tile);}}
    void setListener(Listener value){listener=value;}
    void setStacked(boolean value){stacked=value;}
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    void bind(FourChessView board,boolean revealed){
        setVisibility(board.isSkillMode()?VISIBLE:GONE);if(!board.isSkillMode())return;
        int owner=board.skillViewer();int[] hand=board.skillHand(owner);
        for(int i=0;i<tiles.length;i++){
            int id=hand[i],remaining=board.skillRemaining(owner,id);
            boolean ready=board.currentTurn()==owner&&board.skillAvailable(id);
            String reason=remaining==0?"次数已用完":!board.canAct()||board.currentTurn()!=owner?"等待你的回合":!ready?"暂无合法目标":"";
            int accent=SkillCardTheme.forSkill(id).edge;
            Tile tile=tiles[i];tile.title=revealed?SkillRules.name(id):"未揭晓秘卷";tile.description=revealed?shortDescription(id):"点击揭晓本轮技能";
            tile.skillId=id;tile.reason=reason;tile.revealed=revealed;tile.remaining=revealed?remaining:-1;tile.accent=accent;tile.setText(tile.title);tile.setEnabled(!revealed||ready);
            tile.setSelected(revealed&&board.selectedSkill()==id);tile.setAlpha(1f);
            tile.setContentDescription(revealed?SkillCardTheme.label(id)+"，"+tile.title+"，剩余"+remaining+"次，"+SkillRules.DETAILS[id-1]+(reason.isEmpty()?"":"，"+reason):"第"+(i+1)+"张技能，点击揭晓");
            tile.refreshBackground();tile.invalidate();
        }
    }
    private String shortDescription(int id){String s=SkillRules.DETAILS[id-1];int end=s.indexOf('，');if(end<0)end=s.indexOf('。');return end>0?s.substring(0,end):s;}
    @Override protected void onMeasure(int ws,int hs){
        int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs),gap=dp(6),cols=stacked?2:4,rows=stacked?2:1;
        int tw=Math.max(1,(w-gap*(cols-1))/cols),th=Math.max(1,Math.min(dp(56),(h-gap*(rows-1))/rows));
        for(Tile tile:tiles)tile.measure(MeasureSpec.makeMeasureSpec(tw,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(th,MeasureSpec.EXACTLY));setMeasuredDimension(w,h);
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        int cols=stacked?2:4,rows=stacked?2:1,gap=dp(6),tw=tiles[0].getMeasuredWidth(),th=tiles[0].getMeasuredHeight(),y=(b-t-rows*th-gap*(rows-1))/2;
        for(int i=0;i<tiles.length;i++){int x=(i%cols)*(tw+gap),top=y+(i/cols)*(th+gap);tiles[i].layout(x,top,x+tw,top+th);}
    }
    private final class Tile extends Button {
        String title="",description="",reason="";int remaining,accent,skillId;boolean revealed;private long selectedAt=-1000;private final SkillCardPainter painter=new SkillCardPainter(getContext());
        Tile(Context c){super(c);setAllCaps(false);setMinWidth(0);setMinimumWidth(0);setMinHeight(0);setMinimumHeight(0);setPadding(0,0,0,0);setSingleLine(true);setWillNotDraw(false);setElevation(dp(1));}
        void refreshBackground(){setBackgroundColor(android.graphics.Color.TRANSPARENT);setWillNotDraw(false);}
        @Override public void setSelected(boolean value){if(value&&!isSelected())selectedAt=android.os.SystemClock.uptimeMillis();super.setSelected(value);}
        @Override protected void onDraw(Canvas c){
            c.save();c.translate(getScrollX(),getScrollY());RectF rect=new RectF(dp(1),dp(1),getWidth()-dp(1),getHeight()-dp(2));
            if(revealed){boolean motion=android.os.Build.VERSION.SDK_INT<26||android.animation.ValueAnimator.areAnimatorsEnabled();float shine=isSelected()&&motion?(android.os.SystemClock.uptimeMillis()-selectedAt)/340f:0;painter.draw(c,rect,skillId,remaining,isSelected()||isPressed(),!isEnabled(),remaining==0?"已用尽":reason,true,shine);if(shine>0&&shine<1)postInvalidateOnAnimation();}
            else{android.graphics.Paint p=new android.graphics.Paint(3);p.setColor(GameDesign.PANEL_RAISED);c.drawRoundRect(rect,dp(7),dp(7),p);p.setColor(GameDesign.GOLD_BRIGHT);p.setTextSize(Math.min(13*getResources().getDisplayMetrics().scaledDensity,getHeight()*.26f));p.setTypeface(GameDesign.EMPHASIS);p.setTextAlign(Paint.Align.CENTER);c.drawText("秘卷 · 点击揭晓",getWidth()/2f,getHeight()/2f-(p.ascent()+p.descent())/2,p);}
            c.restore();
        }
        @Override public boolean onTouchEvent(MotionEvent e){if(isEnabled()){if(e.getActionMasked()==MotionEvent.ACTION_DOWN)animate().scaleX(.985f).scaleY(.985f).setDuration(80).start();else if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL)animate().scaleX(1).scaleY(1).setDuration(100).start();}return super.onTouchEvent(e);}
    }
    @Override protected LayoutParams generateDefaultLayoutParams(){return new LayoutParams(-1,-1);}
}
