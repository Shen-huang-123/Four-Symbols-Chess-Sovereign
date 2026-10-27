package com.dorm.fourchess;

import android.graphics.*;

/** Semantic blind embossing at 5-12% opacity. Confined to the illustration field. */
final class SkillCardPatternPainter {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    void draw(Canvas c,RectF card,SkillCardVisual visual,SkillCardTheme theme){
        c.saveLayerAlpha(card,20);c.clipRect(card.left,card.top,card.right,card.top+card.height()*.54f);
        c.translate(card.left,card.top);c.scale(card.width()/100,card.height()*.54f/100);
        p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.45f);p.setColor(theme.edge);
        for(int side:new int[]{-1,1}){
            c.save();if(side==1){c.translate(100,0);c.scale(-1,1);}
            Path motif=new Path();
            switch(visual.background){
                case STRIKE:
                    motif.moveTo(4,89);motif.cubicTo(7,60,26,58,19,31);motif.cubicTo(37,58,20,77,16,91);
                    motif.moveTo(8,76);motif.cubicTo(6,67,20,50,16,42);motif.moveTo(3,55);motif.lineTo(14,35);motif.lineTo(12,55);break;
                case WIND:
                    motif.moveTo(3,72);motif.cubicTo(25,68,32,41,8,40);motif.cubicTo(-3,40,1,27,16,29);
                    motif.moveTo(6,89);motif.cubicTo(37,80,45,49,26,17);motif.moveTo(3,60);motif.cubicTo(16,57,19,48,12,48);break;
                case BRONZE:
                    motif.moveTo(3,81);motif.lineTo(3,37);motif.lineTo(14,23);motif.lineTo(27,37);motif.lineTo(25,71);motif.lineTo(14,87);motif.close();
                    motif.moveTo(7,76);motif.lineTo(7,40);motif.lineTo(14,31);motif.lineTo(22,40);motif.lineTo(21,68);motif.lineTo(14,79);motif.close();
                    motif.moveTo(14,39);motif.lineTo(14,69);motif.moveTo(9,49);motif.lineTo(19,49);motif.moveTo(9,59);motif.lineTo(19,59);break;
                case INK:
                    motif.moveTo(0,77);motif.cubicTo(16,60,16,35,28,17);motif.moveTo(9,63);motif.cubicTo(0,39,7,37,13,50);
                    motif.moveTo(15,48);motif.cubicTo(29,44,33,31,17,39);motif.moveTo(1,91);motif.cubicTo(23,76,36,95,43,86);break;
            }
            c.drawPath(motif,p);c.restore();
        }
        c.restore();
    }
}
