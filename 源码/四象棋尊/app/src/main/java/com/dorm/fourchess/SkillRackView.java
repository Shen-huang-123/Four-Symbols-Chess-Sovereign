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
            int accent=(id==7||id==30)?GameDesign.BLUE_LIGHT:(id==15||id==24)?GameDesign.GREEN_LIGHT:SkillRules.tierColor(id);
            Tile tile=tiles[i];tile.title=revealed?SkillRules.name(id):"未揭晓秘卷";tile.description=revealed?shortDescription(id):"点击揭晓本轮技能";
            tile.remaining=revealed?remaining:-1;tile.accent=accent;tile.setText(tile.title);tile.setEnabled(!revealed||ready);
            tile.setSelected(revealed&&board.selectedSkill()==id);tile.setAlpha(!revealed||ready?1f:.52f);
            tile.setContentDescription(revealed?tile.title+"，剩余"+remaining+"次，"+SkillRules.DETAILS[id-1]+(reason.isEmpty()?"":"，"+reason):"第"+(i+1)+"张技能，点击揭晓");
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
        String title="",description="";int remaining,accent;final TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);
        Tile(Context c){super(c);setAllCaps(false);setMinWidth(0);setMinimumWidth(0);setMinHeight(0);setMinimumHeight(0);setPadding(0,0,0,0);setSingleLine(true);setWillNotDraw(false);setElevation(dp(1));}
        void refreshBackground(){
            StateListDrawable states=new StateListDrawable();states.addState(new int[]{android.R.attr.state_pressed},surface(0xee314959,GameDesign.GOLD_BRIGHT));states.addState(new int[]{android.R.attr.state_selected},surface(0xee263d50,GameDesign.GOLD_BRIGHT));states.addState(new int[]{},surface(0xd51b3040,isEnabled()?0x997f8f93:GameDesign.EDGE_QUIET));setBackground(states);setWillNotDraw(false);
        }
        GradientDrawable surface(int fill,int edge){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(7));d.setStroke(Math.max(1,dp(.7f)),edge);return d;}
        @Override protected void onDraw(Canvas c){
            c.save();c.translate(getScrollX(),getScrollY());float sd=getResources().getDisplayMetrics().scaledDensity,h=getHeight(),pad=dp(10),badge=dp(18),cx=getWidth()-pad-badge/2,cy=pad+badge/2;
            paint.setColor(isSelected()?GameDesign.GOLD_BRIGHT:accent);c.drawRoundRect(pad/2,dp(10),pad/2+dp(2),h-dp(10),dp(1),dp(1),paint);
            paint.setColor(0x553f596b);c.drawCircle(cx,cy,badge/2,paint);paint.setColor(GameDesign.GOLD_BRIGHT);paint.setTypeface(GameDesign.EMPHASIS);paint.setTextSize(Math.min(11*sd,h*.26f));paint.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fm=paint.getFontMetrics();c.drawText(remaining<0?"?":String.valueOf(remaining),cx,cy-(fm.ascent+fm.descent)/2,paint);
            paint.setTextAlign(Paint.Align.LEFT);paint.setTextSize(Math.min(14*sd,h*.32f));paint.setColor(GameDesign.GOLD_BRIGHT);paint.setTypeface(GameDesign.EMPHASIS);
            String heading=TextUtils.ellipsize(title,paint,Math.max(1,getWidth()-pad*2-badge-dp(6)),TextUtils.TruncateAt.END).toString();fm=paint.getFontMetrics();c.drawText(heading,pad,h*.32f-(fm.ascent+fm.descent)/2,paint);
            paint.setTypeface(GameDesign.BODY);paint.setTextSize(Math.min(10*sd,h*.24f));paint.setColor(GameDesign.SECONDARY);String detail=TextUtils.ellipsize(description,paint,Math.max(1,getWidth()-pad*2),TextUtils.TruncateAt.END).toString();fm=paint.getFontMetrics();c.drawText(detail,pad,h*.73f-(fm.ascent+fm.descent)/2,paint);c.restore();
        }
        @Override public boolean onTouchEvent(MotionEvent e){if(isEnabled()){if(e.getActionMasked()==MotionEvent.ACTION_DOWN)animate().scaleX(.985f).scaleY(.985f).setDuration(80).start();else if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL)animate().scaleX(1).scaleY(1).setDuration(100).start();}return super.onTouchEvent(e);}
    }
    @Override protected LayoutParams generateDefaultLayoutParams(){return new LayoutParams(-1,-1);}
}
