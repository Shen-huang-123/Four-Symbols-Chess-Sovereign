"""Editable original 100-unit illustration paths -> Android vectors + transparent SVG masters.
No downloaded artwork, fonts, filters, or runtime generation dependencies.
"""
from pathlib import Path
from xml.sax.saxutils import escape
import math

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'app/src/main/res/drawable'
SVG=Path(__file__).resolve().parent/'技能纹章'
NAMES='疾行 移形 回春 封印 神行 天遁 破阵 裂甲 穿云 换防 斗转 乾坤 归营 挺进 退守 侧步 飞跃 闪击 横扫 唤马 重铸 鸣炮 护国 灵象 寒潮 解咒 春风 献祭 摸鱼 驱离'.split()
LEGEND={6,9,10,12,21,25};RARE={5,7,8,11,17,18,19,20,22,30}

def art(skill):
    grade=0 if skill in LEGEND else 1 if skill in RARE else 2
    edge,light,mid,dark,qi=[('#E4BE78','#F5E1AC','#B47D43','#47302F','#BA6E53'),('#A7CBDD','#DBECEA','#5F9BAA','#223D52','#75B5BE'),('#A7CBB5','#DAE8CB','#5A9983','#25483F','#75B9A1')][grade]
    paths=[]
    def path(d,fill='none',stroke=None,width=1,alpha=1):paths.append((d,fill,stroke,width,alpha))
    def line(d,color=edge,width=1,alpha=1):path(d,'none',color,width,alpha)
    def polygon(points,fill,stroke=edge,width=.9,alpha=1):path('M'+' L'.join(f'{x:g},{y:g}' for x,y in points)+' Z',fill,stroke,width,alpha)
    def oval(x,y,rx,ry,fill,stroke=None,width=.8,alpha=1):
        k=.552285;path(f'M{x-rx},{y} C{x-rx},{y-ry*k} {x-rx*k},{y-ry} {x},{y-ry} C{x+rx*k},{y-ry} {x+rx},{y-ry*k} {x+rx},{y} C{x+rx},{y+ry*k} {x+rx*k},{y+ry} {x},{y+ry} C{x-rx*k},{y+ry} {x-rx},{y+ry*k} {x-rx},{y} Z',fill,stroke,width,alpha)
    def star(x,y,s=2):polygon([(x,y-s),(x+s*.3,y-s*.3),(x+s,y),(x+s*.3,y+s*.3),(x,y+s),(x-s*.3,y+s*.3),(x-s,y),(x-s*.3,y-s*.3)],light,None,alpha=.85)
    def cloud(x,y,s=1):
        # An asymmetric ruyi cloud silhouette, filled before its etched inner curl.
        d=f'M{x-24*s},{y+5*s} C{x-34*s},{y-5*s} {x-20*s},{y-13*s} {x-14*s},{y-7*s} C{x-15*s},{y-26*s} {x+10*s},{y-23*s} {x+11*s},{y-9*s} C{x+27*s},{y-17*s} {x+34*s},{y+4*s} {x+20*s},{y+6*s} Z'
        path(d,dark,mid,1,.85);line(f'M{x-17*s},{y} C{x-21*s},{y-8*s} {x-8*s},{y-10*s} {x-8*s},{y-1*s} C{x-8*s},{y+3*s} {x-2*s},{y+3*s} {x+5*s},{y+2*s}',edge,.7,.75)
    def rune_ring(cx,cy,r,tilt=.42):
        oval(cx,cy,r,r*tilt,'none',edge,.9,.65)
        for i in range(10):
            a=i*math.tau/10;x=cx+math.cos(a)*r;y=cy+math.sin(a)*r*tilt
            line(f'M{x-1.4:.2f},{y-1.5:.2f} L{x+1.4:.2f},{y-1.5:.2f} M{x:.2f},{y-2.2:.2f} L{x:.2f},{y+2.0:.2f}',light,.6,.7)
    def token(x,y,r=13,mark='jade',opacity=1):
        oval(x,y+5,r,r*.44,dark,mid,1,opacity);path(f'M{x-r},{y} L{x-r},{y+5} C{x-r},{y+12} {x+r},{y+12} {x+r},{y+5} L{x+r},{y} Z',mid,edge,.8,opacity)
        oval(x,y,r,r*.74,mid,edge,1,opacity);oval(x,y-1,r*.78,r*.54,dark,edge,.65,opacity)
        path(f'M{x-r*.65},{y-3} C{x-r*.2},{y-8} {x+r*.1},{y-8} {x+r*.6},{y-3}',light,None,alpha=.20*opacity)
        if mark=='jade':polygon([(x,y-5),(x+3,y),(x,y+5),(x-3,y)],light,None,alpha=opacity)
        elif mark=='guard':line(f'M{x-5},{y-2} L{x+5},{y-2} M{x},{y-6} L{x},{y+5} M{x-4},{y+4} L{x+4},{y+4}',light,1.3,opacity)
        elif mark=='pawn':line(f'M{x-4},{y-4} L{x+4},{y-4} M{x-4},{y} L{x+4},{y} M{x},{y-4} L{x},{y+4} M{x-4},{y+6} L{x},{y+3} L{x+4},{y+6}',light,1.1,opacity)
    def wind(x,y,wide=1,flip=False):
        z=-1 if flip else 1
        path(f'M{x-35*wide},{y+8*z} C{x-12*wide},{y-20*z} {x+22*wide},{y+7*z} {x+33*wide},{y-17*z} C{x+28*wide},{y+12*z} {x-12*wide},{y-4*z} {x-35*wide},{y+8*z} Z',qi,None,alpha=.6)
        line(f'M{x-30*wide},{y+7*z} C{x-8*wide},{y-11*z} {x+15*wide},{y+9*z} {x+31*wide},{y-13*z}',light,.7,.7)
    def sword(x=50,y=48,angle=36):
        # Rotate coordinates in the source so SVG and Android have exactly the same geometry.
        def rot(pts):
            a=math.radians(angle);return [(x+px*math.cos(a)-py*math.sin(a),y+px*math.sin(a)+py*math.cos(a)) for px,py in pts]
        polygon(rot([(-3,17),(-4,-21),(0,-39),(4,-21),(3,17)]),mid,edge,1.1)
        polygon(rot([(0,-35),(0,17),(3,17),(4,-21)]),light,None,alpha=.85)
        polygon(rot([(-11,15),(-10,19),(0,21),(10,19),(11,15),(0,17)]),edge,None)
        polygon(rot([(-2,21),(-2,32),(0,35),(2,32),(2,21)]),dark,edge,.9)
    def shield(x=50,y=48,tortoise=False,broken=False):
        polygon([(x-22,y-22),(x,y-31),(x+22,y-22),(x+19,y+14),(x,y+32),(x-19,y+14)],dark,edge,1.5)
        polygon([(x-16,y-18),(x,y-24),(x+16,y-18),(x+13,y+11),(x,y+23),(x-13,y+11)],mid,edge,.7)
        if tortoise:
            oval(x,y,15,20,dark,edge,1)
            polygon([(x,y-10),(x+8,y-5),(x+8,y+6),(x,y+12),(x-8,y+6),(x-8,y-5)],mid,edge,.9)
            line(f'M{x},{y-10} L{x},{y-19} M{x+8},{y-5} L{x+14},{y-9} M{x+8},{y+6} L{x+13},{y+12} M{x},{y+12} L{x},{y+20} M{x-8},{y+6} L{x-13},{y+12} M{x-8},{y-5} L{x-14},{y-9}',edge,.8)
            path(f'M{x+8},{y-24} C{x+33},{y-33} {x+36},{y-9} {x+19},{y-8} C{x+39},{y+5} {x+27},{y+27} {x+10},{y+23}', 'none',qi,2)
            polygon([(x+8,y-25),(x+13,y-29),(x+17,y-25),(x+12,y-22)],light,None)
        else:polygon([(x,y-12),(x+8,y),(x,y+12),(x-8,y)],edge,None,alpha=.9)
        if broken:path(f'M{x-6},{y-32} L{x+3},{y-11} L{x-6},{y+2} L{x+8},{y+33}','none',dark,3.2);line(f'M{x-4},{y-30} L{x+5},{y-11} L{x-4},{y+2} L{x+10},{y+31}',light,.8)
    def lotus(x,y):
        for sign in [-1,1]:
            path(f'M{x},{y} C{x+sign*8},{y-17} {x+sign*27},{y-16} {x+sign*31},{y-7} C{x+sign*27},{y+6} {x+sign*8},{y+6} {x},{y} Z',mid,edge,.8)
            line(f'M{x},{y} Q{x+sign*16},{y+1} {x+sign*28},{y-7}',light,.6,.6)
        path(f'M{x},{y} C{x-12},{y-9} {x-8},{y-22} {x},{y-25} C{x+8},{y-22} {x+12},{y-9} {x},{y} Z',qi,edge,.7)
    def horse(x=50,y=49):
        path(f'M{x-20},{y+23} C{x-25},{y+8} {x-17},{y-7} {x-8},{y-15} L{x-11},{y-27} L{x-1},{y-22} L{x+8},{y-30} L{x+10},{y-16} L{x+23},{y-3} L{x+19},{y+7} L{x+7},{y+3} Q{x-1},{y+12} {x+13},{y+23} Z',mid,edge,1.1)
        path(f'M{x-15},{y+20} C{x-20},{y+3} {x-6},{y-5} {x-2},{y-16} L{x+3},{y-20} C{x-1},{y-1} {x-8},{y+2} {x-5},{y+15} Z',dark,None)
        oval(x+7,y-10,1.6,1.6,light);line(f'M{x-11},{y-17} Q{x-26},{y-8} {x-27},{y+5} M{x-15},{y-9} Q{x-31},{y} {x-29},{y+13}',qi,1.2)
    def talisman(broken=False):
        polygon([(37,17),(66,21),(62,73),(50,84),(33,72)],mid,edge,1.1)
        polygon([(41,22),(61,25),(58,68),(50,76),(39,67)],dark,None)
        line('M44,31 L57,33 M50,26 L48,60 L43,66 M42,43 L56,45 M44,52 L53,50 L55,58',light,1.6)
        line('M36,20 Q25,7 21,24 M64,24 Q79,12 81,31',qi,1,.8)
        if broken:
            path('M26,48 L43,43 L53,53 L72,44','none',dark,5)
            line('M25,47 L43,42 L53,52 L73,43',light,1.2)
            polygon([(23,42),(18,35),(20,46)],mid,None);polygon([(75,51),(83,58),(81,48)],qi,None)

    # Ground/atmosphere is deliberately different for each semantic family.
    if skill in {1,5,14,16,17}:wind(48,62,.95)
    elif skill in {2,6,10,11,12}:rune_ring(50,74,37,.30)
    elif skill in {7,8,9,18,19}:cloud(26,69,.65);cloud(76,37,.45)
    elif skill in {13,15,23}:rune_ring(50,73,36,.30)
    elif skill in {3,20,21,22,24,26,27,28,29}:oval(50,79,32,6,'none',mid,.8,.7)
    else:wind(50,76,.75)

    if skill==1:
        token(31,58,13,opacity=.25);token(43,53,13,opacity=.5);wind(50,36,1);token(59,47,15);wind(51,67,1,True);star(82,28)
    elif skill==2:
        line('M23,56 C10,15 67,5 82,39 M77,51 C90,89 30,94 18,67',edge,1.5)
        path('M30,34 C35,12 64,25 56,42 C50,55 35,44 42,58 C50,72 70,58 70,42','none',qi,4)
        token(30,54,14);token(71,36,14);star(32,17);star(72,79)
    elif skill==3:
        lotus(50,72);token(50,42,17,'pawn');path('M35,33 C19,17 33,5 40,17 C45,25 37,27 35,23','none',qi,1.1);star(66,21)
    elif skill==4:
        talisman();line('M17,39 L78,70 M24,75 L79,36',edge,2);star(18,40);star(78,68)
    elif skill==5:
        token(29,65,12,opacity=.2);token(45,49,13,opacity=.45);token(66,31,15);wind(49,32,1);wind(53,76,1,True);star(80,14)
    elif skill==6:
        # Floating chessman, broken spatial aperture and foreground ritual platform.
        path('M22,65 C7,26 37,3 62,17 C83,27 84,48 78,62','none',edge,2)
        path('M27,59 C17,31 41,14 62,25 C74,33 77,47 72,57','none',mid,3)
        polygon([(13,27),(22,20),(18,34),(24,44),(16,40)],light,None,alpha=.7)
        polygon([(79,24),(87,32),(81,43),(86,53),(76,46)],qi,None)
        token(50,44,18);rune_ring(50,70,30,.23)
        line('M38,65 L39,55 M50,65 L50,58 M62,64 L61,55',qi,.8,.7);star(72,13,3)
    elif skill==7:
        token(31,65,14);sword(55,45,40)
        for pts in [[(69,40),(84,33),(77,47)],[(68,54),(83,60),(73,64)],[(60,26),(66,15),(67,31)]]:polygon(pts,mid,edge,.7)
    elif skill==8:shield(broken=True);sword(52,49,32);star(24,21)
    elif skill==9:
        cloud(31,35,.9);cloud(71,72,.75);path('M21,84 Q59,40 80,13 Q70,47 28,88 Z',qi,None,alpha=.4);sword(51,50,40);star(80,15,3)
    elif skill==10:
        token(28,50,17,'guard');token(73,45,17,'guard');line('M18,26 C34,8 71,12 85,30 M81,70 C63,90 30,88 15,68',edge,1.3);polygon([(80,22),(86,30),(75,29)],edge,None);polygon([(21,76),(15,68),(27,69)],mid,None)
    elif skill==11:
        token(26,61,15);polygon([(71,24),(87,39),(75,57),(59,40)],mid,edge,1.2);polygon([(71,28),(73,50),(83,39)],light,None,alpha=.5);line('M16,42 C11,15 54,8 68,20 M84,60 C90,89 44,92 31,78',qi,1.8);star(41,15);star(63,85)
    elif skill==12:
        oval(50,45,31,31,dark,edge,1.2);path('M50,14 C78,16 79,44 50,45 C26,46 29,74 50,76 C11,76 10,14 50,14 Z',mid,None)
        oval(50,30,5,5,light);oval(50,60,5,5,dark,edge,.7);rune_ring(50,48,41,1);star(81,17)
    elif skill==13:
        polygon([(26,43),(50,26),(76,43),(63,45),(50,36),(37,46)],mid,edge,1)
        polygon([(22,58),(50,42),(79,58),(66,62),(50,53),(35,63)],dark,edge,1)
        polygon([(31,60),(69,60),(69,79),(31,79)],mid,edge,.9)
        polygon([(45,79),(45,66),(55,66),(55,79)],dark,None)
        line('M50,17 L50,27 M25,80 L76,80 M22,84 L79,84',edge,1.1);cloud(18,56,.35)
    elif skill==14:
        wind(53,39,.85);sword(50,43,0);token(50,73,12);line('M28,52 Q25,30 36,19 M74,53 Q78,31 65,20',qi,1.4)
    elif skill==15:shield(50,46,True);rune_ring(50,77,29,.24);star(19,24)
    elif skill==16:
        token(22,49,13,opacity=.18);token(38,49,13,opacity=.4);token(63,49,16);wind(49,31,.95);wind(50,72,1.1,True);line('M15,43 L32,40 M22,58 L36,57',light,1)
    elif skill==17:
        cloud(50,76,1);horse(51,45);path('M19,68 C5,37 37,8 73,13','none',qi,2);polygon([(66,9),(76,13),(65,20)],edge,None);star(81,23)
    elif skill==18:
        path('M15,72 Q17,18 64,15 Q33,36 26,79 Z',qi,None,alpha=.45);sword(50,50,39);line('M19,64 Q28,23 61,22',light,.9);star(80,24)
    elif skill==19:
        path('M19,59 C28,12 80,8 86,35 C66,18 39,37 35,62 Z',mid,edge,1)
        polygon([(24,65),(75,38),(78,42),(27,70)],light,edge,.8);wind(52,78,.9,True)
    elif skill==20:lotus(50,81);horse(50,45);star(77,18);star(21,30,1.6)
    elif skill==21:
        polygon([(29,27),(66,27),(77,37),(62,46),(43,40),(43,66),(29,66)],mid,edge,1.2)
        polygon([(43,41),(58,46),(55,55),(43,54)],light,None,alpha=.55)
        polygon([(23,71),(78,71),(72,77),(29,77)],dark,edge,.9)
        path('M48,68 C32,50 55,46 53,30 C70,45 72,54 64,66 C75,59 77,48 76,45 C89,66 73,83 58,81 C41,83 30,73 31,62 C33,68 42,72 48,68 Z',qi,edge,.8)
        line('M43,20 L36,11 M72,26 L84,17',edge,.8);star(29,19)
    elif skill==22:
        polygon([(24,48),(66,31),(76,47),(34,65)],mid,edge,1.1);oval(70,39,8,11,dark,edge,1);oval(70,39,4,6,mid,None)
        oval(35,69,11,11,dark,edge,1);oval(35,69,4,4,mid,edge,.7);polygon([(33,60),(64,54),(69,66),(39,75)],dark,edge,.9)
        path('M76,35 Q89,24 86,12 Q98,22 90,42 Q88,30 76,35 Z',qi,None);cloud(77,20,.3);star(55,20)
    elif skill==23:shield(50,49);polygon([(35,48),(50,37),(65,48),(60,49),(50,43),(40,49)],light,None);line('M40,50 L40,59 L60,59 L60,50 M47,59 L47,51 L53,51 L53,59',dark,1);lotus(50,85)
    elif skill==24:
        lotus(50,81);path('M23,56 C14,32 29,20 46,25 C61,13 83,28 79,47 L80,72 Q74,79 71,69 L68,52 L58,57 L58,73 L47,73 L46,58 Q35,64 30,61 L30,73 L21,73 Z',mid,edge,1.1)
        path('M27,37 C11,30 14,57 30,54 Q40,47 27,37 Z',dark,edge,.8);path('M75,43 Q88,44 82,64 Q80,67 78,61','none',light,2)
        line('M38,34 Q45,28 52,34 L49,45 L40,45 Z',edge,.8);oval(69,36,1.5,1.5,light)
    elif skill==25:
        for i in range(6):
            a=math.radians(i*60);ux,uy=math.cos(a),math.sin(a);nx,ny=-uy,ux
            pts=[(50+ux*12,48+uy*12),(50+ux*31+nx*3,48+uy*31+ny*3),(50+ux*38,48+uy*38),(50+ux*31-nx*3,48+uy*31-ny*3)]
            polygon(pts,mid,edge,.8);line(f'M{50+ux*23+nx*7},{48+uy*23+ny*7} L{50+ux*29},{48+uy*29} L{50+ux*23-nx*7},{48+uy*23-ny*7}',light,.7)
        polygon([(50,34),(62,41),(62,55),(50,62),(38,55),(38,41)],dark,edge,1.1);star(50,48,8)
    elif skill==26:talisman(True);wind(50,74,1);star(19,49);star(80,43)
    elif skill==27:
        path('M31,78 C27,48 62,30 64,13 Q70,46 43,65 Z',mid,edge,.8)
        for x,y,s in [(35,58,-1),(49,46,1),(58,32,-1)]:path(f'M{x},{y} Q{x+s*24},{y-23} {x+s*25},{y-4} Q{x+s*13},{y+1} {x},{y} Z',qi,edge,.6)
        wind(49,76,1);star(24,21)
    elif skill==28:
        token(50,70,18);path('M50,61 C31,51 55,30 47,16 C69,31 67,46 58,55 C67,50 74,45 74,37 C86,57 62,73 50,61 Z',qi,edge,.8)
        polygon([(28,72),(20,81),(34,77)],mid,None);polygon([(67,75),(80,81),(76,70)],mid,None)
    elif skill==29:
        # Leaping jade koi: silhouette, tail, belly, fin rays and sparse scale engraving.
        oval(47,84,32,5,'none',edge,.8,.55);oval(47,84,21,3,'none',mid,.6,.7)
        path('M28,67 C18,46 42,22 68,24 C81,26 79,38 65,46 C49,53 41,59 28,67 Z',mid,edge,1.1)
        path('M28,66 C44,39 60,35 75,29 C70,45 49,48 42,59 Z',light,None,alpha=.65)
        path('M30,62 C17,55 8,59 11,76 L23,72 L26,86 C38,81 39,71 30,62 Z',qi,edge,.8)
        path('M48,39 Q31,23 36,18 Q52,21 58,32 Z',dark,edge,.7)
        path('M51,48 Q65,55 56,61 L41,57 Z',qi,edge,.7)
        for x,y in [(39,44),(46,38),(53,33),(37,51),(46,46)]:line(f'M{x-2},{y} Q{x},{y+4} {x+3},{y}',edge,.65,.7)
        oval(70,30,2.1,2.1,dark);oval(69.5,29.5,.6,.6,light);line('M77,34 Q86,36 88,29 M15,65 L25,67 M18,74 L28,70',edge,.7)
        wind(51,81,.9);star(80,56,1.5)
    elif skill==30:
        oval(23,51,11,29,'none',edge,1.4);oval(23,51,6,19,dark,mid,1)
        path('M33,22 C52,30 53,71 33,79 M43,18 C68,31 68,69 43,83','none',qi,2)
        token(76,47,15);line('M86,24 L93,19 M85,69 L93,76 M58,46 L66,46 M59,53 L65,52',light,1);star(45,17)

    # A few grade-specific punctuation details, never a common enclosing icon frame.
    if grade==0:star(13,18,2);star(87,79,1.7)
    elif grade==1:star(16,20,1.4)
    return paths

def write_all():
    OUT.mkdir(parents=True,exist_ok=True);SVG.mkdir(parents=True,exist_ok=True)
    for skill,name in enumerate(NAMES,1):
        vectors=[];svgs=[]
        for d,fill,stroke,width,alpha in art(skill):
            attrs=f'android:pathData="{d}" android:fillColor="{fill if fill!="none" else "#00000000"}" android:fillAlpha="{alpha:g}"'
            svg=f'd="{d}" fill="{fill}" opacity="{alpha:g}"'
            if stroke:attrs+=f' android:strokeColor="{stroke}" android:strokeWidth="{width:g}" android:strokeAlpha="{alpha:g}" android:strokeLineJoin="round" android:strokeLineCap="round"';svg+=f' stroke="{stroke}" stroke-width="{width:g}" stroke-linejoin="round" stroke-linecap="round"'
            vectors.append('    <path '+attrs+' />');svgs.append('  <path '+svg+' />')
        xml='<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="100dp" android:height="100dp" android:viewportWidth="100" android:viewportHeight="100">\n'+'\n'.join(vectors)+'\n</vector>\n'
        svg='<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><title>'+escape(name)+'</title>\n'+'\n'.join(svgs)+'\n</svg>\n'
        (OUT/f'skill_art_{skill:02}.xml').write_text(xml,encoding='utf-8');(SVG/f'skill_art_{skill:02}.svg').write_text(svg,encoding='utf-8')
    print('WROTE 30 Android illustrations and 30 editable SVG masters')

if __name__=='__main__':write_all()
