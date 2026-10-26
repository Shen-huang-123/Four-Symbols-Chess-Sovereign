package com.dorm.fourchess;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.Button;
/** Four real, focusable skill shortcuts; all validation remains in FourChessView. */
final class SkillRackView extends ViewGroup {
 interface Listener{void select(int slot);}private Listener listener;private boolean stacked;private final Button[] tiles=new Button[4];
 SkillRackView(Context c){super(c);for(int i=0;i<4;i++){final int slot=i;Button b=new Button(c);b.setAllCaps(false);b.setGravity(17);b.setTypeface(GameDesign.EMPHASIS);b.setTextSize(12);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(2,2,2,2);b.setMaxLines(3);if(android.os.Build.VERSION.SDK_INT>=26)b.setAutoSizeTextTypeUniformWithConfiguration(9,13,1,2);b.setOnClickListener(v->{if(listener!=null)listener.select(slot);});tiles[i]=b;addView(b);}}
 void setListener(Listener l){listener=l;}void setStacked(boolean value){stacked=value;}
 private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
 void bind(FourChessView board,boolean revealed){setVisibility(board.isSkillMode()?VISIBLE:GONE);if(!board.isSkillMode())return;int owner=board.skillViewer();int[] hand=board.skillHand(owner);for(int i=0;i<4;i++){int id=hand[i];boolean used=board.skillUsed(owner,id);Button b=tiles[i];b.setText(revealed?SkillRules.name(id)+"\n"+(used?"已使用":SkillRules.tier(id)):"秘卷\n点击揭晓");b.setContentDescription(revealed?SkillRules.name(id)+"，"+SkillRules.DETAILS[id-1]+(used?"，已使用":"，剩余一次"):"第"+(i+1)+"张技能，点击揭晓");int color=revealed?SkillRules.tierColor(id):GameDesign.GOLD;GradientDrawable bg=new GradientDrawable();bg.setColor(GameDesign.PANEL);bg.setCornerRadius(dp(9));bg.setStroke(dp(1),used?GameDesign.EDGE_QUIET:color);b.setBackground(bg);b.setTextColor(used?GameDesign.MUTED:color);b.setAlpha(used?.55f:1f);}}
 @Override protected void onMeasure(int ws,int hs){int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs),gap=dp(6),cols=stacked?2:4,rows=stacked?2:1;int side=Math.max(1,Math.min(dp(92),Math.min((w-gap*(cols-1))/cols,(h-gap*(rows-1))/rows)));for(Button b:tiles)b.measure(MeasureSpec.makeMeasureSpec(side,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(side,MeasureSpec.EXACTLY));setMeasuredDimension(w,h);}
 @Override protected void onLayout(boolean c,int l,int t,int r,int b){int cols=stacked?2:4,rows=stacked?2:1,gap=dp(6),side=tiles[0].getMeasuredWidth(),x=(r-l-cols*side-gap*(cols-1))/2,y=(b-t-rows*side-gap*(rows-1))/2;for(int i=0;i<4;i++){int left=x+(i%cols)*(side+gap),top=y+(i/cols)*(side+gap);tiles[i].layout(left,top,left+side,top+side);}}
 @Override protected LayoutParams generateDefaultLayoutParams(){return new LayoutParams(-1,-1);}
}
