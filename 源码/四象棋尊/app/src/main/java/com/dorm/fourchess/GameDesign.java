package com.dorm.fourchess;

import android.graphics.Color;
import android.graphics.Typeface;

/** Shared visual contract. Never holds game state or input geometry. Values in dp/sp. */
final class GameDesign {
    private GameDesign() {}
    static final int NIGHT=0xff101e2b, INK_TEAL=0xff182f38;
    static final int PANEL=0xe61a2d3b, PANEL_QUIET=0xcc152633, PANEL_RAISED=0xf0213846;
    static final int GOLD=0xffdfc591, GOLD_BRIGHT=0xfff0d9a6, EDGE=0x998f856e, EDGE_QUIET=0x5578888c;
    static final int TEXT=0xfff1e8d6, SECONDARY=0xffadbec5, MUTED=0xff8399a3, DISABLED=0xff72838b;
    static final int IVORY=0xfff3ead7, IVORY_SHADE=0xffeaddc5, GRID=0xc05b3e29;
    static final int RED=0xffc1372e, GREEN=0xff27704b, BLUE=0xff285f88, AMBER=0xffa96f18;
    static final int RED_LIGHT=0xffe7ad9e, GREEN_LIGHT=0xffabcdb8, BLUE_LIGHT=0xffabc9df, AMBER_LIGHT=0xffe5ca93;
    static final int SHADOW=0x40071019, RIPPLE=0x33dfc591;
    static final float SPACE_XS=4, SPACE_SM=8, SPACE_MD=12, SPACE_LG=16, SPACE_XL=24;
    static final float RADIUS_SM=8, RADIUS_PANEL=12, RADIUS_DIALOG=16;
    static final float STROKE=.7f, STROKE_ACTIVE=1.1f, TOUCH_MIN=44;
    static final float TYPE_DISPLAY=29, TYPE_TITLE=19, TYPE_BODY=15, TYPE_LABEL=12, TYPE_CAPTION=11;
    static final int PRESS_MS=80, RELEASE_MS=150, CONFIRM_MS=220, ENTER_MS=300, TURN_MS=600;
    static final Typeface BODY=Typeface.create("sans-serif",Typeface.NORMAL);
    static final Typeface EMPHASIS=Typeface.create("sans-serif-medium",Typeface.NORMAL);
    static final Typeface TITLE=Typeface.create("serif",Typeface.BOLD);
    static final Typeface PIECE=Typeface.create("sans-serif",Typeface.BOLD);
    static int alpha(int color,int opacity){return Color.argb(opacity,Color.red(color),Color.green(color),Color.blue(color));}
    static int factionInk(int index){switch(index){case 0:return RED;case 1:return GREEN;case 2:return BLUE;default:return AMBER;}}
    static int factionLight(int index){switch(index){case 0:return RED_LIGHT;case 1:return GREEN_LIGHT;case 2:return BLUE_LIGHT;default:return AMBER_LIGHT;}}
    static int[] factionInks(){return new int[]{RED,GREEN,BLUE,AMBER};}
    static int[] factionLights(){return new int[]{RED_LIGHT,GREEN_LIGHT,BLUE_LIGHT,AMBER_LIGHT};}
}
