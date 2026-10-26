package com.dorm.fourchess;

/** Thirty deliberately uneven skills with randomly dealt charges. IDs are stable wire identifiers. */
final class SkillRules {
    static final int COUNT=30, HAND_SIZE=4;
    static final String[] NAMES={"疾行","移形","回春","封印","神行","天遁","破阵","裂甲","穿云","换防","斗转","乾坤","归营","挺进","退守","侧步","飞跃","闪击","横扫","唤马","重铸","鸣炮","护国","灵象","寒潮","解咒","春风","献祭","摸鱼","驱离"};
    static final String[] DETAILS={
        "己方非主将移至相邻空位，可斜行，不吃子。",
        "交换距离不超过4格（横纵相加）的两枚己方非主将。",
        "在空着的初始兵位补回一枚损失的兵卒，最多5枚。",
        "封住一枚敌方非主将，直到对方下一回合结束。",
        "己方非主将跳至两格范围内的空位，可斜行。",
        "己方非主将传送至任意有效空位，不能吃子。",
        "己方非主将吃掉相邻敌方非主将，可斜向。",
        "己方非主将吃掉两格范围内的敌方非主将，可越子。",
        "己方非主将吃掉同行或同列三格内的敌方非主将，可越子。",
        "交换任意两枚己方非主将的位置。",
        "己方非主将与距离不超过3格（横纵相加）的敌方非主将交换。",
        "己方非主将与任意一枚敌方非主将交换位置。",
        "己方非主将传送到本方底线的任意空位。",
        "己方非主将向本方前进方向跳进1至3格空位，可越子。",
        "己方非主将向本方后退方向跳退1至3格空位，可越子。",
        "己方非主将沿本方横向跳到1至2格外空位。",
        "己方非主将按马步跳到空位，不受马腿限制。",
        "己方非主将沿斜线移动1至2格，可越子并吃敌方非主将。",
        "己方非主将沿直线移动1至2格，可越子并吃敌方非主将。",
        "在空着的初始马位召回一枚损失的马，最多2枚。",
        "在空着的初始车位召回一枚损失的车，最多2枚。",
        "在空着的初始炮位召回一枚损失的炮，最多2枚。",
        "在空着的初始士位召回一枚损失的士，最多2枚。",
        "在空着的初始象位召回一枚损失的象，最多2枚。",
        "选择敌方非主将，将它及相邻敌方非主将一同封印，各到其下一回合结束。",
        "解除一枚己方非主将的封印。",
        "选择任意己方非主将，解除所有己方棋子的封印；须确有封印。",
        "移除一枚己方非主将，结束回合。弱势整活牌，谨慎使用。",
        "选择己方非主将，什么也不做并结束回合。弱势整活牌，可用于让一手。",
        "把一枚敌方非主将挪到它相邻的空位，可斜向。"
    };
    static String name(int id){return id>=1&&id<=COUNT?NAMES[id-1]:"";}
    static String tier(int id){switch(id){case 6:case 9:case 10:case 12:case 21:case 25:return "传说";case 5:case 7:case 8:case 11:case 17:case 18:case 19:case 20:case 22:case 30:return "奇策";case 28:case 29:return "整活";default:return "常规";}}
    static int tierColor(int id){String t=tier(id);return t.equals("传说")?GameDesign.AMBER_LIGHT:t.equals("奇策")?GameDesign.BLUE_LIGHT:t.equals("整活")?GameDesign.MUTED:GameDesign.GREEN_LIGHT;}
    static boolean twoStep(int id){return id==1||id==2||(id>=5&&id<=19)||id==30;}
    static char restoreType(int id){switch(id){case 3:return 'B';case 20:return 'M';case 21:return 'C';case 22:return 'P';case 23:return 'S';case 24:return 'X';default:return 0;}}
    static String prompt(int id,boolean source){if(!twoStep(id))return name(id)+"："+(restoreType(id)!=0?"选择发亮的空初始位置":id==26||id==27||id==28||id==29?"选择发亮的己方非主将":"选择发亮的敌方非主将");return name(id)+"："+(source?"选择发亮的目标位置":id==30?"选择敌方非主将":"选择己方非主将");}
    static String guide(){StringBuilder b=new StringBuilder("30种技能随机发牌，每人4张，同局16张互不重复，不保证强度均衡。每张随机1至3次；存活各方各行动一次计一回合，每满10回合用新4张替换旧牌及剩余次数；施放占用一次行动，也可不出技能、正常走棋。主将不参与技能，技能不能直接消灭主将。被封印棋子不能参与位移、换位或攻击，仍可被普通走棋吃掉。\n\n");for(int i=1;i<=COUNT;i++)b.append("【").append(tier(i)).append("】").append(name(i)).append("：").append(DETAILS[i-1]).append("\n\n");return b.append("取消瞄准不消耗。悔棋恢复技能、次数、效果与抽牌进度，重走同一抽牌节点不会刷牌。联机由房主发牌并校验，所有人需安装v4.5或以上。技能牌随棋局同步，不提供防作弊的隐藏手牌机制。").toString();}
}
