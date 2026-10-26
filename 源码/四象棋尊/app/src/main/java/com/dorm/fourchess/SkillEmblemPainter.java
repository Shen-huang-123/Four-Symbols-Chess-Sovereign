package com.dorm.fourchess;

import android.graphics.*;

/** Thirty original vector seals; only presentation is selected by the stable skill ID. */
final class SkillEmblemPainter {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private Canvas c;
    private int ink;
    private void line(float x,float y,float xx,float yy){c.drawLine(x,y,xx,yy,p);}
    private void circle(float x,float y,float radius){c.drawCircle(x,y,radius,p);}
    private void poly(float... points){Path path=new Path();path.moveTo(points[0],points[1]);for(int i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);c.drawPath(path,p);}
    private void arc(float x,float y,float r,float start,float sweep){c.drawArc(x-r,y-r,x+r,y+r,start,sweep,false,p);}
    private void arrow(float x,float y,float xx,float yy){line(x,y,xx,yy);double a=Math.atan2(yy-y,xx-x);float s=.15f;line(xx,yy,xx-s*(float)Math.cos(a-.6),yy-s*(float)Math.sin(a-.6));line(xx,yy,xx-s*(float)Math.cos(a+.6),yy-s*(float)Math.sin(a+.6));}
    private void diamond(float x,float y,float r){poly(x,y-r,x+r,y,x,y+r,x-r,y,x,y-r);}
    private void star(float x,float y,float r){line(x-r,y,x+r,y);line(x,y-r,x,y+r);}
    private void token(float x,float y,float r,String text){
        p.setStyle(Paint.Style.FILL);p.setColor(GameDesign.alpha(ink,17));circle(x,y,r);p.setStyle(Paint.Style.STROKE);p.setColor(ink);circle(x,y,r);
        if(!text.isEmpty()){p.setStyle(Paint.Style.FILL);p.setTypeface(GameDesign.EMPHASIS);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(r*1.3f);c.drawText(text,x,y-(p.ascent()+p.descent())/2,p);p.setStyle(Paint.Style.STROKE);}
    }
    private void talisman(float x,float y,boolean broken){
        poly(x-.2f,y-.36f,x+.2f,y-.36f,x+.2f,y+.24f,x,y+.4f,x-.2f,y+.24f,x-.2f,y-.36f);
        if(broken){poly(x-.28f,y+.06f,x-.06f,y-.06f,x+.05f,y+.08f,x+.29f,y-.03f);line(x-.38f,y-.02f,x-.48f,y-.12f);line(x+.35f,y+.1f,x+.47f,y+.18f);}
        else{line(x,y-.27f,x,y+.22f);line(x-.12f,y-.15f,x+.12f,y-.15f);poly(x-.13f,y+.02f,x,y-.03f,x+.13f,y+.02f);}
    }
    private void shield(float x,float y,float r){poly(x-r,y-r*.65f,x,y-r,x+r,y-r*.65f,x+r*.75f,y+r*.45f,x,y+r,x-r*.75f,y+r*.45f,x-r,y-r*.65f);}
    private void restore(String piece,int variant){
        token(0,0,.32f,piece);arc(0,0,.57f,35,260);arrow(.01f,-.58f,.28f,-.52f);star(-.48f,.35f,.1f);
        if(variant==0){line(-.62f,-.16f,-.76f,-.16f);line(-.69f,-.23f,-.69f,-.09f);}
        if(variant==1){poly(-.54f,.49f,-.4f,.62f,-.12f,.56f);}
        if(variant==2){poly(.38f,-.51f,.53f,-.69f,.49f,-.42f,.66f,-.38f);}
        if(variant==3){shield(0,0,.76f);}
        if(variant==4){poly(-.48f,-.45f,-.7f,-.57f,-.6f,-.3f);poly(.45f,.48f,.7f,.55f,.58f,.29f);}
    }
    void draw(Canvas canvas,RectF bounds,int id,SkillCardTheme theme,boolean disabled){
        if(id<1||id>SkillRules.COUNT)return;c=canvas;ink=disabled?0xff9aa9a5:theme.edge;
        c.save();c.translate(bounds.centerX(),bounds.centerY());float scale=Math.min(bounds.width(),bounds.height())/2;c.scale(scale,scale);
        p.setShader(null);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.024f);p.setColor(GameDesign.alpha(ink,90));
        // Grade-specific ritual enclosure: seal tablet, constellation diamond, or open jade arcs.
        if(theme.pattern==0){poly(-.72f,-.88f,.72f,-.88f,.9f,-.64f,.9f,.62f,.55f,.9f,-.55f,.9f,-.9f,.62f,-.9f,-.64f,-.72f,-.88f);diamond(0,-.88f,.095f);diamond(0,.9f,.075f);for(int s:new int[]{-1,1}){poly(s*.76f,-.46f,s*.66f,-.55f,s*.76f,-.65f);poly(s*.76f,.46f,s*.66f,.55f,s*.76f,.65f);}}
        else if(theme.pattern==1){diamond(0,0,.92f);star(-.71f,-.45f,.055f);star(.71f,.45f,.055f);}
        else{arc(0,0,.86f,18,115);arc(0,0,.86f,198,115);poly(-.75f,.55f,-.88f,.62f,-.82f,.43f);}
        p.setColor(ink);p.setStrokeWidth(.041f);
        switch(id){
            case 1:token(-.23f,.21f,.26f,"");arrow(-.02f,.02f,.43f,-.43f);line(-.56f,.03f,-.7f,.16f);line(-.44f,.4f,-.56f,.54f);break;
            case 2:token(-.35f,.2f,.22f,"");token(.35f,-.2f,.22f,"");arrow(-.51f,-.18f,.18f,-.38f);arrow(.51f,.18f,-.18f,.38f);break;
            case 3:restore("兵",0);break;
            case 4:talisman(0,0,false);line(-.42f,-.4f,.42f,.4f);line(-.42f,.4f,.42f,-.4f);break;
            case 5:token(-.39f,.33f,.21f,"");arrow(-.12f,.05f,.37f,-.44f);arrow(.12f,.22f,.58f,-.24f);line(-.54f,-.05f,-.28f,-.31f);break;
            case 6:arc(0,-.02f,.48f,180,180);line(-.48f,-.02f,-.48f,.48f);line(.48f,-.02f,.48f,.48f);token(0,.22f,.2f,"");star(.61f,-.42f,.11f);poly(-.2f,.52f,0,.65f,.2f,.52f);break;
            case 7:token(-.3f,.32f,.23f,"");diamond(.24f,-.25f,.24f);arrow(-.1f,.09f,.37f,-.38f);poly(.15f,-.48f,.04f,-.61f,.31f,-.55f);break;
            case 8:shield(0,0,.49f);poly(-.2f,-.57f,.02f,-.17f,-.13f,.06f,.26f,.6f);line(-.5f,.04f,-.63f,.15f);line(.45f,-.12f,.6f,-.22f);break;
            case 9:arrow(-.61f,.25f,.57f,-.3f);poly(-.52f,-.12f,-.2f,-.24f,.04f,-.2f);poly(-.05f,.39f,.25f,.38f,.46f,.27f);line(-.22f,.25f,-.12f,.45f);break;
            case 10:token(-.39f,0,.24f,"士");token(.39f,0,.24f,"士");arrow(-.48f,-.4f,.41f,-.4f);arrow(.48f,.4f,-.41f,.4f);break;
            case 11:token(-.3f,.21f,.22f,"");diamond(.32f,-.2f,.26f);arc(0,0,.58f,208,110);arc(0,0,.58f,28,110);arrow(.4f,-.41f,.17f,-.56f);arrow(-.4f,.41f,-.17f,.56f);break;
            case 12:circle(0,0,.61f);arc(0,-.3f,.3f,90,180);arc(0,.3f,.3f,270,180);token(-.04f,-.3f,.1f,"");diamond(.04f,.3f,.12f);star(.69f,0,.07f);break;
            case 13:poly(-.61f,.1f,0,-.49f,.61f,.1f);poly(-.42f,-.07f,-.42f,.49f,.42f,.49f,.42f,-.07f);arrow(.28f,.13f,-.12f,.13f);line(-.62f,.61f,.62f,.61f);break;
            case 14:arrow(0,.56f,0,-.58f);poly(-.3f,.08f,0,-.2f,.3f,.08f);poly(-.3f,.39f,0,.11f,.3f,.39f);break;
            case 15:shield(0,-.09f,.43f);arrow(0,.09f,0,.66f);line(-.53f,-.47f,-.3f,-.47f);line(.3f,-.47f,.53f,-.47f);break;
            case 16:token(0,0,.23f,"");arrow(-.29f,0,-.64f,0);arrow(.29f,0,.64f,0);line(-.52f,.33f,.52f,.33f);break;
            case 17:token(-.37f,.4f,.2f,"马");poly(-.37f,.16f,-.37f,-.35f,.41f,-.35f);arrow(.19f,-.35f,.48f,-.35f);diamond(.48f,.33f,.1f);break;
            case 18:poly(-.52f,.55f,.43f,-.48f,.53f,-.58f,.5f,-.26f,-.35f,.59f,-.52f,.55f);line(-.48f,.21f,-.15f,.51f);arc(0,0,.64f,220,83);break;
            case 19:arc(0,0,.43f,205,147);arc(0,0,.61f,205,147);arrow(-.55f,.37f,.58f,.37f);line(0,-.13f,0,.17f);break;
            case 20:restore("马",0);break;
            case 21:restore("车",1);break;
            case 22:restore("炮",2);break;
            case 23:restore("士",3);break;
            case 24:restore("象",4);break;
            case 25:for(int i=0;i<3;i++){c.save();c.rotate(i*60);line(-.55f,0,.55f,0);poly(.3f,-.13f,.42f,0,.3f,.13f);poly(-.3f,-.13f,-.42f,0,-.3f,.13f);c.restore();}diamond(0,0,.18f);break;
            case 26:talisman(0,0,true);line(-.09f,-.25f,.09f,-.25f);line(0,-.32f,0,-.14f);break;
            case 27:for(int i=0;i<3;i++){float y=(i-1)*.3f;Path wind=new Path();wind.moveTo(-.65f,y);wind.cubicTo(-.1f,y-.27f,.35f,y+.2f,.59f,y-.1f);c.drawPath(wind,p);}star(.35f,-.54f,.1f);poly(-.16f,.26f,-.3f,.52f,-.12f,.62f);break;
            case 28:token(0,.19f,.29f,"");poly(-.16f,-.6f,.15f,-.6f,.09f,-.11f,0,.02f,-.1f,-.11f,-.16f,-.6f);line(-.28f,-.17f,.29f,-.17f);line(-.24f,.55f,-.34f,.66f);line(.23f,.54f,.34f,.66f);break;
            case 29:c.drawOval(-.49f,-.22f,.32f,.23f,p);poly(.27f,0,.59f,-.27f,.59f,.27f,.27f,0);circle(-.29f,-.04f,.035f);arc(-.03f,.58f,.35f,210,120);break;
            case 30:token(.25f,0,.25f,"");arrow(-.58f,0,-.02f,0);poly(-.55f,-.37f,-.28f,-.3f,-.14f,-.16f);poly(-.55f,.37f,-.28f,.3f,-.14f,.16f);line(.64f,-.25f,.64f,.25f);break;
        }
        c.restore();c=null;
    }
}
