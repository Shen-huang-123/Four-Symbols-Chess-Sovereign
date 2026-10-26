package com.dorm.fourchess;

import android.app.*;
import android.os.*;
import java.lang.reflect.*;
import java.util.*;

/** Independent 30-skill effect fixtures and random deal / malformed-state checks. */
public final class DraftSkillProbe extends Instrumentation {
    private final StringBuilder log=new StringBuilder();
    private void check(boolean ok,String label){if(!ok)throw new AssertionError(label);log.append("PASS ").append(label).append('\n');}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private Object field(Object obj,String name)throws Exception{Field f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);}
    @SuppressWarnings("unchecked") private List<FourChessView.Piece> pieces(FourChessView b)throws Exception{return (List<FourChessView.Piece>)field(b,"pieces");}
    private FourChessView.Piece at(FourChessView b,int[] xy)throws Exception{for(FourChessView.Piece p:pieces(b))if(p.x==xy[0]&&p.y==xy[1])return p;return null;}
    private int[] square(int o,int f,int depth){return o==0?new int[]{5+f,18-depth}:o==1?new int[]{depth,13-f}:o==2?new int[]{13-f,depth}:new int[]{18-depth,5+f};}
    private int[] xy(int o,int x,int y){return square(o,x-5,18-y);}
    private void piece(StringBuilder s,int owner,char type,int[] p,boolean sealed){s.append(owner).append(',').append(type).append(',').append(p[0]).append(',').append(p[1]).append(',').append(sealed?1:0).append(';');}
    private String fixture(int owner,int skill){
        StringBuilder s=new StringBuilder(owner+",-1,-1,-1,-1,K3,0,0,0,0,0,0");
        for(int o=0;o<4;o++){s.append(',').append(skill);int n=1;for(int slot=1;slot<4;slot++){while(n==skill)n++;s.append(',').append(n++);}}
        s.append('|');for(int o=0;o<4;o++)piece(s,o,'J',square(o,4,0),false);
        piece(s,owner,'C',xy(owner,9,9),false);piece(s,owner,'M',xy(owner,10,9),false);piece(s,owner,'P',xy(owner,8,9),true);
        int enemy=(owner+1)%4;piece(s,enemy,'C',xy(owner,10,10),false);piece(s,enemy,'M',xy(owner,11,10),false);piece(s,enemy,'B',xy(owner,9,11),false);return s.toString();
    }
    private int[][] action(int o,int id){
        int[] from=xy(o,9,9),to;
        switch(id){
            case 1:to=xy(o,10,8);break;case 2:case 10:to=xy(o,10,9);break;
            case 3:to=square(o,0,3);from=to;break;
            case 4:case 25:to=xy(o,10,10);from=to;break;
            case 5:to=xy(o,11,9);break;case 6:to=xy(o,13,10);break;
            case 7:case 11:case 18:to=xy(o,10,10);break;
            case 8:case 12:to=xy(o,11,10);break;case 9:case 19:to=xy(o,9,11);break;
            case 13:to=square(o,0,0);break;case 14:to=square(o,4,11);break;
            case 15:to=square(o,4,8);break;case 16:to=square(o,6,9);break;case 17:to=square(o,6,10);break;
            case 20:to=square(o,1,0);from=to;break;case 21:to=square(o,0,0);from=to;break;
            case 22:to=square(o,1,2);from=to;break;case 23:to=square(o,3,0);from=to;break;case 24:to=square(o,2,0);from=to;break;
            case 26:to=xy(o,8,9);from=to;break;case 27:case 28:case 29:to=from;break;
            default:from=xy(o,10,10);to=xy(o,11,11);
        }return new int[][]{from,to};
    }
    private boolean cast(FourChessView b,int o,int id,int[][] a){return b.trySkill(o,id,a[0][0],a[0][1],a[1][0],a[1][1]);}
    private void runChecks()throws Exception{
        FourChessView b=new FourChessView(getTargetContext()),copy=new FourChessView(getTargetContext());b.setSkillMode(true);
        Set<String> deals=new HashSet<>();Set<Integer> seen=new HashSet<>();
        for(int round=0;round<100;round++){
            b.newGame();Set<Integer> unique=new HashSet<>();
            for(int o=0;o<4;o++){int[] hand=b.skillHand(o);check(hand.length==4,"four cards round "+round+" side "+o);for(int id:hand){check(id>=1&&id<=30&&unique.add(id),"valid globally unique deal "+round+" id "+id);seen.add(id);}int original=hand[0];hand[0]=0;check(b.skillHand(o)[0]==original,"hand accessor defensive copy "+round+" side "+o);}
            String state=b.exportState();deals.add(state);copy.importState(state);check(copy.exportState().equals(state),"all hands synchronize round "+round);
            b.cancelSkill();check(b.exportState().equals(state),"cancel never rerolls round "+round);
        }
        check(deals.size()>95&&seen.size()==30,"randomness produces diverse deals and covers all thirty skills");
        check(SkillRules.NAMES.length==30&&SkillRules.DETAILS.length==30,"complete catalog descriptions");
        for(int o=0;o<4;o++)for(int id=1;id<=30;id++){
            String start=fixture(o,id);b.importState(start);check(start.equals(b.exportState()),"fixture import "+o+"/"+id);
            int[][] a=action(o,id);int count=pieces(b).size();
            check(!cast(b,(o+1)%4,id,a)&&start.equals(b.exportState()),"reject wrong seat "+o+"/"+id);
            check(!b.trySkill(o,id,-1,0,a[1][0],a[1][1])&&!b.trySkill(o,id,a[0][0],a[0][1],19,19),"reject invalid coordinates "+o+"/"+id);
            int[] king=square(o,4,0);check(!b.trySkill(o,id,king[0],king[1],king[0],king[1]),"own general excluded "+o+"/"+id);
            int[] enemyKing=square((o+1)%4,4,0);check(!b.trySkill(o,id,a[0][0],a[0][1],enemyKing[0],enemyKing[1]),"enemy general excluded "+o+"/"+id);
            check(b.beginSkill(id),"skill has selectable targets "+o+"/"+id);b.cancelSkill();check(start.equals(b.exportState()),"target cancellation unchanged "+o+"/"+id);
            check(cast(b,o,id,a),"cast skill "+o+"/"+id);check(b.currentTurn()==(o+1)%4&&b.skillUsed(o,id)&&b.undoCount()==1,"exactly one turn and charge "+o+"/"+id);
            boolean revive=SkillRules.restoreType(id)!=0,capture=id==7||id==8||id==9||id==18||id==19;
            check(pieces(b).size()==count+(revive?1:capture||id==28?-1:0),"piece count effect "+o+"/"+id);
            if(revive)check(at(b,a[1])!=null&&at(b,a[1]).type==SkillRules.restoreType(id)&&at(b,a[1]).owner==o,"revival type and side "+o+"/"+id);
            else if(id==4||id==25){check(at(b,a[1]).sealed,"target sealed "+o+"/"+id);if(id==25)check(at(b,xy(o,11,10)).sealed&&at(b,xy(o,9,11)).sealed,"area seal affects neighbors "+o);}
            else if(id==26||id==27)check(!at(b,xy(o,8,9)).sealed,"cleanse removes seal "+o+"/"+id);
            else if(id==28)check(at(b,a[1])==null,"sacrifice removes own piece "+o);
            else if(id==29)check(at(b,a[1])!=null&&at(b,a[1]).owner==o,"weak pass keeps own piece "+o);
            else {check(at(b,a[1])!=null&&at(b,a[1]).owner==(id==30?(o+1)%4:o),"move or capture reaches target "+o+"/"+id);if(id==2||id==10||id==11||id==12)check(at(b,a[0])!=null&&at(b,a[0]).owner==(id==11||id==12?(o+1)%4:o),"swap source populated "+o+"/"+id);else check(at(b,a[0])==null,"source cleared "+o+"/"+id);}
            String after=b.exportState();copy.importState(after);check(after.equals(copy.exportState()),"effect metadata roundtrip "+o+"/"+id);
            b.importState(o+after.substring(after.indexOf(',')));String used=b.exportState();check(!cast(b,o,id,a)&&used.equals(b.exportState()),"spent skill cannot be recast "+o+"/"+id);
            check(b.undoSteps(1)&&start.equals(b.exportState()),"undo exact hands charges pieces seals "+o+"/"+id);
            check(!b.trySkill(o,30==id?29:30,a[0][0],a[0][1],a[1][0],a[1][1]),"unowned card rejected "+o+"/"+id);
        }
        b.importState(fixture(0,6));String valid=b.exportState();
        String[] parts=valid.split("\\|",2),header=parts[0].split(",");String[] bad=header.clone();bad[12]="31";b.importState(String.join(",",bad)+"|"+parts[1]);check(valid.equals(b.exportState()),"malformed hand id rejected atomically");
        bad=header.clone();bad[13]=bad[12];b.importState(String.join(",",bad)+"|"+parts[1]);check(valid.equals(b.exportState()),"duplicate card rejected atomically");
        bad=header.clone();bad[6]=Integer.toString(1<<29);b.importState(String.join(",",bad)+"|"+parts[1]);check(valid.equals(b.exportState()),"unowned spent mask rejected atomically");
        b.importState(valid+"0,B,9,9,0;");check(valid.equals(b.exportState()),"duplicate square rejected atomically");
        b.setSkillMode(false);b.newGame();String normal=b.exportState();check(!normal.contains("K3")&&!b.beginSkill(6)&&!b.trySkill(0,6,5,15,6,14),"ordinary mode never deals or casts skills");
        copy.importState(normal);check(copy.exportState().equals(normal),"ordinary protocol unchanged");
    }
    @Override public void onStart(){Bundle result=new Bundle();try{final Throwable[] error={null};runOnMainSync(()->{try{runChecks();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError(error[0]);log.append("DRAFT COMPLETE\n");result.putString("stream",log.toString());finish(Activity.RESULT_OK,result);}catch(Throwable e){result.putString("stream",log+"FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
