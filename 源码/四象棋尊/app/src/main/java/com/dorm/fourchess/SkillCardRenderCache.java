package com.dorm.fourchess;

import android.graphics.*;

/** Native drawing-command cache: text is still real text, with no full-card bitmap allocation. */
final class SkillCardRenderCache {
    private final Picture[] planes=new Picture[3];
    private final RectF bounds=new RectF();
    private float width=-1,height=-1;
    private int skill=-1,charges=-1;
    private boolean selected;
    private int recordings;
    void prepare(SkillCardPainter painter,RectF card,int id,int remaining,boolean selection){
        boolean geometry=width!=card.width()||height!=card.height()||skill!=id||charges!=remaining;
        if(!geometry&&selected==selection)return;
        width=card.width();height=card.height();skill=id;charges=remaining;selected=selection;bounds.set(0,0,width,height);
        for(int i=0;i<3;i++){
            if(!geometry&&i!=0)continue;
            Picture picture=new Picture();Canvas recording=picture.beginRecording(Math.max(1,(int)Math.ceil(width)),Math.max(1,(int)Math.ceil(height)));
            painter.drawLayer(recording,bounds,id,remaining,selection,remaining==0,remaining==0?"已用尽":"",false,1,1,1<<i);
            picture.endRecording();planes[i]=picture;recordings++;
        }
    }
    void draw(Canvas c,RectF r,float artOpacity,float textOpacity){
        c.save();c.translate(r.left,r.top);c.drawPicture(planes[0]);
        drawPlane(c,planes[1],artOpacity);drawPlane(c,planes[2],textOpacity);c.restore();
    }
    private void drawPlane(Canvas c,Picture picture,float opacity){
        if(opacity<=0)return;
        if(opacity>=1){c.drawPicture(picture);return;}
        int layer=c.saveLayerAlpha(bounds,Math.round(opacity*255));c.drawPicture(picture);c.restoreToCount(layer);
    }
    int recordingCount(){return recordings;}
}
