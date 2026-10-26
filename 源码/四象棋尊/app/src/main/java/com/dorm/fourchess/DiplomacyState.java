package com.dorm.fourchess;

/** Match-scoped treaties. Broken pairs remain barred until a new game, including across undo. */
final class DiplomacyState {
    static final int OFFER=0,ACCEPT=1,DECLINE=2,BREAK=3,SURRENDER=4;
    int allies,broken,offers,serial,lastKind=-1,lastOwner=-1,lastTarget=-1;
    static int pair(int a,int b){if(a<0||a>3||b<0||b>3||a==b)return 0;int x=Math.min(a,b),y=Math.max(a,b);return 1<<(x==0?y-1:x==1?y+1:5);}
    private static int offer(int a,int b){return 1<<(a*4+b);}
    boolean allied(int a,int b){return (allies&pair(a,b))!=0;}
    boolean broken(int a,int b){return (broken&pair(a,b))!=0;}
    boolean offered(int from,int to){return pair(from,to)!=0&&(offers&offer(from,to))!=0;}
    boolean can(int actor,int action,int target){
        if(actor<0||actor>3||action<0||action>4)return false;
        if(action==SURRENDER)return target==-1;
        if(pair(actor,target)==0)return false;
        if(action==BREAK)return allied(actor,target);
        if(allied(actor,target)||broken(actor,target))return false;
        return action==OFFER?!offered(actor,target):offered(target,actor);
    }
    void apply(int actor,int action,int target){
        if(action==OFFER)offers|=offer(actor,target);
        else if(action==ACCEPT){allies|=pair(actor,target);clearOffers(actor,target);}
        else if(action==DECLINE)offers&=~offer(target,actor);
        else if(action==BREAK){allies&=~pair(actor,target);broken|=pair(actor,target);clearOffers(actor,target);}
        serial++;lastKind=action;lastOwner=actor;lastTarget=target;
    }
    private void clearOffers(int a,int b){offers&=~(offer(a,b)|offer(b,a));}
    void eliminate(int owner){for(int i=0;i<4;i++)if(i!=owner){allies&=~pair(owner,i);clearOffers(owner,i);}}
    DiplomacyState copy(){DiplomacyState d=new DiplomacyState();d.allies=allies;d.broken=broken;d.offers=offers;d.serial=serial;d.lastKind=lastKind;d.lastOwner=lastOwner;d.lastTarget=lastTarget;return d;}
    void retainBroken(int permanentlyBroken){broken|=permanentlyBroken;allies&=~broken;for(int a=0;a<4;a++)for(int b=a+1;b<4;b++)if(broken(a,b)){clearOffers(a,b);if(serial==0){serial=1;lastKind=BREAK;lastOwner=a;lastTarget=b;}}}
    String encode(){return "D1,"+allies+","+broken+","+offers+","+serial+","+lastKind+","+lastOwner+","+lastTarget;}
    static DiplomacyState decode(String text){
        String[] f=text.split(",",-1);if(f.length!=8||!f[0].equals("D1"))throw new IllegalArgumentException("diplomacy version");
        DiplomacyState d=new DiplomacyState();d.allies=Integer.parseInt(f[1]);d.broken=Integer.parseInt(f[2]);d.offers=Integer.parseInt(f[3]);d.serial=Integer.parseInt(f[4]);d.lastKind=Integer.parseInt(f[5]);d.lastOwner=Integer.parseInt(f[6]);d.lastTarget=Integer.parseInt(f[7]);
        if(d.allies<0||d.allies>63||d.broken<0||d.broken>63||(d.allies&d.broken)!=0||d.offers<0||(d.offers&~0x7bde)!=0||d.serial<0||d.serial>1000000000)throw new IllegalArgumentException("invalid treaty mask");
        for(int a=0;a<4;a++)for(int b=a+1;b<4;b++)if((d.allied(a,b)||d.broken(a,b))&&(d.offered(a,b)||d.offered(b,a)))throw new IllegalArgumentException("conflicting offer");
        if(d.serial==0){if(d.lastKind!=-1||d.lastOwner!=-1||d.lastTarget!=-1||d.allies!=0||d.broken!=0||d.offers!=0)throw new IllegalArgumentException("empty diplomacy");}
        else if(d.lastKind<0||d.lastKind>4||d.lastOwner<0||d.lastOwner>3||(d.lastKind==SURRENDER?d.lastTarget!=-1:pair(d.lastOwner,d.lastTarget)==0))throw new IllegalArgumentException("invalid diplomacy event");
        return d;
    }
}
