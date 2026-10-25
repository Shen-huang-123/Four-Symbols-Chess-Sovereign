package com.dorm.fourchess;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ScrollView;

public class MainActivity extends Activity implements NetworkManager.Callback {
    private FourChessView board;
    private FrameLayout screen;
    private LinearLayout gameUi;
    private ModeSelectView modePage;
    private FactionStripView playerStrip;
    private TextView status, networkStatus;
    private TextView timer, moveLog;
    private LinearLayout stateBox;
    private Button undoButton,phoneUndo,phoneSound,historyButton;
    private TextView compactState,dialogMoves;
    private CompactControlsLayout mobileControls;
    private AlertDialog historyDialog;
    private ScrollView historyScroll;
    private final java.util.ArrayList<String> displayedMoves=new java.util.ArrayList<>();
    private final java.util.ArrayList<Integer> moveTimes=new java.util.ArrayList<>();
    private NetworkManager network;
    private int networkSession; private boolean matchActive,hosting;
    private AlertDialog entryDialog,exitDialog,connectionDialog;
    private final Handler clockHandler=new Handler(Looper.getMainLooper());
    private int clockTurn=-1,turnSeconds=0;
    private boolean soundOn=true;
    private static final String EMPTY_HISTORY="等待第一手落子";
    private final int ink=0xff191f1c;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(android.os.Build.VERSION.SDK_INT>=28){android.view.WindowManager.LayoutParams attributes=getWindow().getAttributes();attributes.layoutInDisplayCutoutMode=android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;getWindow().setAttributes(attributes);}
        if(android.os.Build.VERSION.SDK_INT>=29){getWindow().setNavigationBarContrastEnforced(false);getWindow().setStatusBarContrastEnforced(false);}
        immersive();
        getWindow().setStatusBarColor(ink); getWindow().setNavigationBarColor(ink);
        buildUi(); board.setStatusListener(text -> {status.setText(compactStatus(text));if(clockTurn!=board.currentTurn()){clockTurn=board.currentTurn();turnSeconds=0;}updateHud();});
        board.setMoveObserver(this::recordMove);clockHandler.post(clockTick);showModePage();
    }

    private void buildUi() {
        screen=new GameWindowLayout(this);gameUi=new LinearLayout(this);gameUi.setOrientation(LinearLayout.VERTICAL);gameUi.setBackgroundColor(Color.TRANSPARENT);
        DuelLayout duel=new DuelLayout(this);gameUi.addView(duel,new LinearLayout.LayoutParams(-1,-1));
        playerStrip=new FactionStripView(this);duel.addView(playerStrip);board=new FourChessView(this);duel.addView(board);
        MatchPanelLayout panel=new MatchPanelLayout(this);panel.setPadding(dp(7),dp(5),dp(7),dp(5));panel.setBackground(panelBackground());duel.addView(panel);
        TextView brand=text("四 象 棋 尊",16,0xffe1cba0);brand.setTypeface(Typeface.create("serif",Typeface.BOLD));brand.setSingleLine();fitText(brand,10,16);panel.addView(brand);
        stateBox=new LinearLayout(this);stateBox.setOrientation(LinearLayout.VERTICAL);stateBox.setPadding(dp(7),dp(4),dp(7),dp(4));stateBox.setBackground(frame(0xff303125,0xffab915e,false));panel.addView(stateBox);
        status=text("准备开局",13,0xff813b2d);status.setTypeface(Typeface.DEFAULT,Typeface.BOLD);status.setSingleLine();fitText(status,9,13);stateBox.addView(status,new LinearLayout.LayoutParams(-1,0,.8f));
        timer=text("本手 00:00",24,0xff813b2d);timer.setTypeface(Typeface.create("monospace",Typeface.BOLD));timer.setSingleLine();fitText(timer,14,24);stateBox.addView(timer,new LinearLayout.LayoutParams(-1,0,1.2f));
        networkStatus=text("同屏模式",10,0xffaba68f);networkStatus.setMaxLines(2);networkStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);fitText(networkStatus,8,10);stateBox.addView(networkStatus,new LinearLayout.LayoutParams(-1,0,1.2f));
        LinearLayout historyBox=new LinearLayout(this);historyBox.setOrientation(LinearLayout.VERTICAL);historyBox.setPadding(dp(7),dp(4),dp(7),dp(3));android.graphics.drawable.Drawable historyBg=frame(0xff202722,0xff665d43,false);historyBox.setBackground(historyBg);panel.addView(historyBox);TextView historyTitle=text("棋谱 · 最新在上",10,0xffc8b18a);historyTitle.setSingleLine();fitText(historyTitle,8,10);historyTitle.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);historyBox.addView(historyTitle,new LinearLayout.LayoutParams(-1,dp(20)));
        historyScroll=new ScrollView(this);historyScroll.setFillViewport(true);historyScroll.setClipToPadding(true);if(android.os.Build.VERSION.SDK_INT>=29){GradientDrawable thumb=new GradientDrawable();thumb.setColor(0xff96835b);thumb.setCornerRadius(dp(2));historyScroll.setVerticalScrollbarThumbDrawable(thumb);historyScroll.setVerticalScrollbarTrackDrawable(new android.graphics.drawable.ColorDrawable(0xff1a231e));}historyBox.addView(historyScroll,new LinearLayout.LayoutParams(-1,0,1));moveLog=text(EMPTY_HISTORY,11,0xffbdb09a);moveLog.setGravity(Gravity.TOP|Gravity.LEFT);moveLog.setIncludeFontPadding(false);moveLog.setPadding(dp(1),dp(3),dp(1),dp(3));moveLog.setLineSpacing(dp(3),1.05f);historyScroll.addView(moveLog,new ScrollView.LayoutParams(-1,-2));
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(0,0,0,0);
        Button mode=button("模式",0xff553c32,0),rules=button("规则",0xff33444a,1),undo=button("悔棋",0xff514633,2),sound=button("声音 开",0xff35463b,3);undoButton=undo;
        mode.setOnClickListener(v->showModePage()); rules.setOnClickListener(v->showRules()); undo.setOnClickListener(v->showUndoOptions());
        sound.setOnClickListener(v->{soundOn=!soundOn;board.setSoundEnabled(soundOn);sound.setText(soundOn?"声音 开":"声音 关");sound.setSelected(soundOn);((ActionGlyph)sound.getCompoundDrawables()[0]).setMuted(!soundOn);syncCompactControls();});sound.setSelected(soundOn);
        LinearLayout row1=new LinearLayout(this),row2=new LinearLayout(this);row1.addView(mode);row1.addView(rules);row2.addView(undo);row2.addView(sound);row.addView(row1,new LinearLayout.LayoutParams(-1,0,1));row.addView(row2,new LinearLayout.LayoutParams(-1,0,1));Button exit=button("退出对局",0xff553c32,5);exit.setOnClickListener(v->showExitConfirm());LinearLayout exitRow=new LinearLayout(this);exitRow.addView(exit);row.addView(exitRow,new LinearLayout.LayoutParams(-1,0,1));panel.addView(row);
        mobileControls=new CompactControlsLayout(this);duel.addView(mobileControls);
        compactState=text("准备开局",12,0xffeed8a4);compactState.setTypeface(Typeface.DEFAULT,Typeface.BOLD);compactState.setMaxLines(2);fitText(compactState,8,12);mobileControls.addView(compactState);
        Button phoneMode=dockButton("模式",0),phoneRules=dockButton("规则",1);
        phoneUndo=dockButton("悔棋",2);phoneSound=dockButton("声音",3);historyButton=dockButton("棋谱",4);Button phoneExit=dockButton("退出",5);phoneExit.setOnClickListener(v->showExitConfirm());
        phoneMode.setOnClickListener(v->mode.performClick());phoneRules.setOnClickListener(v->rules.performClick());phoneUndo.setOnClickListener(v->undo.performClick());phoneSound.setOnClickListener(v->sound.performClick());historyButton.setOnClickListener(v->showHistory());
        for(Button action:new Button[]{phoneMode,phoneRules,phoneUndo,phoneSound,historyButton,phoneExit}){action.setPadding(dp(3),0,dp(3),0);mobileControls.addView(action);}
        TextView rotateHint=text("长按放大棋盘 · 横竖屏自适应",10,0xffafa38b);rotateHint.setSingleLine();fitText(rotateHint,8,10);mobileControls.addView(rotateHint);
        screen.addView(gameUi,new FrameLayout.LayoutParams(-1,-1));
        modePage=new ModeSelectView(this);modePage.setListener(new ModeSelectView.Listener(){
            public void onLocal(){showNamesDialog(false);}
            public void onHost(){showNamesDialog(true);}
            public void onJoin(){joinDialog();}
        });screen.addView(modePage,new FrameLayout.LayoutParams(-1,-1));
        screen.setOnApplyWindowInsetsListener((view,insets)->{applySafeInsets(insets);return insets;});setContentView(screen);screen.requestApplyInsets();
    }

    private void applySafeInsets(android.view.WindowInsets insets){
        int left=0,right=0,top=0,bottom=0;
        if(android.os.Build.VERSION.SDK_INT>=30){
            android.graphics.Insets safe=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());
            left=safe.left;right=safe.right;top=safe.top;bottom=safe.bottom;
            // Keep a small gesture guard, rather than the entire hidden navigation bar.
            bottom=Math.max(bottom,Math.min(dp(6),insets.getInsets(android.view.WindowInsets.Type.mandatorySystemGestures()).bottom));
        }else{
            int flags=getWindow().getDecorView().getSystemUiVisibility();
            if((flags&android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)==0){left=insets.getSystemWindowInsetLeft();right=insets.getSystemWindowInsetRight();bottom=insets.getSystemWindowInsetBottom();}
            if((flags&android.view.View.SYSTEM_UI_FLAG_FULLSCREEN)==0)top=insets.getSystemWindowInsetTop();
        }
        if(android.os.Build.VERSION.SDK_INT>=28&&insets.getDisplayCutout()!=null){android.view.DisplayCutout cutout=insets.getDisplayCutout();left=Math.max(left,cutout.getSafeInsetLeft());right=Math.max(right,cutout.getSafeInsetRight());top=Math.max(top,cutout.getSafeInsetTop());bottom=Math.max(bottom,cutout.getSafeInsetBottom());}
        screen.setPadding(left,top,right,bottom);
    }
    private String compactStatus(String value){return value.replace("行动 · 请选择棋子","行棋").replace("行动 · 等待对手","等待对手");}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);return t;}
    private void fitText(TextView view,int min,int max){view.setIncludeFontPadding(false);view.setHorizontallyScrolling(false);if(android.os.Build.VERSION.SDK_INT>=26)view.setAutoSizeTextTypeUniformWithConfiguration(min,max,1,android.util.TypedValue.COMPLEX_UNIT_SP);}
    private Button button(String label,int color,int glyph){
        Button button=new Button(this);button.setText(label);button.setContentDescription(label);button.setTextColor(0xffe6ce97);button.setTextSize(12);
        button.setAllCaps(false);button.setSingleLine();button.setMinHeight(0);button.setMinimumHeight(0);button.setMinWidth(0);button.setMinimumWidth(0);
        button.setGravity(Gravity.CENTER);button.setPadding(dp(5),0,dp(4),0);button.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));fitText(button,9,12);
        ActionGlyph icon=new ActionGlyph(glyph);icon.setBounds(0,0,dp(14),dp(14));button.setCompoundDrawables(icon,null,null,null);button.setCompoundDrawablePadding(dp(2));
        button.setBackground(frame(color,0xff9b8354,false));button.setElevation(0);button.setStateListAnimator(null);
        button.setOnTouchListener((view,event)->{if(event.getAction()==android.view.MotionEvent.ACTION_DOWN)view.animate().scaleX(.97f).scaleY(.97f).setDuration(70).start();else if(event.getAction()==android.view.MotionEvent.ACTION_UP||event.getAction()==android.view.MotionEvent.ACTION_CANCEL)view.animate().scaleX(1).scaleY(1).setDuration(130).start();return false;});
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-1,1);params.setMargins(dp(2),dp(3),dp(2),dp(3));button.setLayoutParams(params);return button;
    }
    private Button dockButton(String label,int glyph){
        Button action=new ActionDockButton(this);action.setText(label);action.setContentDescription(label);action.setTextColor(0xffeadcc0);action.setTextSize(11);action.setAllCaps(false);action.setSingleLine();action.setMinHeight(0);action.setMinimumHeight(0);action.setMinWidth(0);action.setMinimumWidth(0);action.setPadding(0,0,0,0);fitText(action,8,11);
        ActionGlyph icon=new ActionGlyph(glyph);icon.setBounds(0,0,dp(14),dp(14));action.setCompoundDrawables(icon,null,null,null);action.setCompoundDrawablePadding(0);action.setStateListAnimator(null);action.setElevation(0);
        action.setOnTouchListener((view,event)->{int type=event.getActionMasked();if(type==android.view.MotionEvent.ACTION_DOWN)view.animate().scaleX(.92f).scaleY(.92f).setDuration(80).start();else if(type==android.view.MotionEvent.ACTION_UP||type==android.view.MotionEvent.ACTION_CANCEL)view.animate().scaleX(1).scaleY(1).setDuration(180).start();return false;});return action;
    }
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private int lighten(int c){return Color.rgb(Math.min(255,Color.red(c)+28),Math.min(255,Color.green(c)+28),Math.min(255,Color.blue(c)+28));}
    private android.graphics.drawable.Drawable panelBackground(){return frame(0xff242a24,0xff8a7751,true);}
    private BattleSkin.Frame frame(int fill,int edge,boolean ornament){return new BattleSkin.Frame(fill,edge,getResources().getDisplayMetrics().density,ornament);}

    private void showModePage(){modePage.onEnter();gameUi.setVisibility(android.view.View.INVISIBLE);modePage.setVisibility(android.view.View.VISIBLE);modePage.bringToFront();modePage.invalidate();screen.invalidate();}
    private void recordMove(String entry){displayedMoves.add(0,entry);moveTimes.add(0,turnSeconds);renderMoves();}
    private void clearMoves(){displayedMoves.clear();moveTimes.clear();turnSeconds=0;clockTurn=board.currentTurn();refreshClock();renderMoves();}
    private void showUndoOptions(){
        if(network!=null){toast("局域网对局暂不支持协商悔棋，请在同屏模式使用");return;}
        int available=board.undoCount();if(available==0){toast("还没有可撤回的棋步");return;}
        AlertDialog.Builder dialog=new AlertDialog.Builder(this).setTitle("悔棋").setMessage("将恢复棋子、行动方、上一手标记和该手已用时间。取消不会改变棋局。")
            .setPositiveButton("撤回上一步",(d,w)->performUndo(1)).setNegativeButton("取消",null);
        if(available>=4)dialog.setNeutralButton("撤回最近四步",(d,w)->performUndo(4));dialog.show();
    }
    private void performUndo(int count){
        if(network!=null)return;
        int seconds=moveTimes.size()>=count?moveTimes.get(count-1):0;
        if(!board.undoSteps(count)){toast("没有足够的棋步可撤回");return;}
        for(int i=0;i<count&&!displayedMoves.isEmpty();i++)displayedMoves.remove(0);
        for(int i=0;i<count&&!moveTimes.isEmpty();i++)moveTimes.remove(0);
        turnSeconds=seconds;clockTurn=board.currentTurn();refreshClock();renderMoves();toast("已撤回 "+count+" 步");
    }
    private void refreshClock(){timer.setText(String.format(java.util.Locale.CHINA,"本手 %02d:%02d",turnSeconds/60,turnSeconds%60));playerStrip.setActivePlayer(board.currentTurn(),turnSeconds);if(compactState!=null)compactState.setText(status.getText()+(getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_PORTRAIT?" · ":"\n")+String.format(java.util.Locale.CHINA,"%02d:%02d",turnSeconds/60,turnSeconds%60));}
    private void updateHud(){
        int[] tints={0xffefb09b,0xffb0caaa,0xffa7c7dc,0xffe0c88d};
        status.setTextColor(tints[board.currentTurn()]);timer.setTextColor(0xffeed8a4);stateBox.setBackground(frame(0xff303125,0xffab915e,false));
        for(int i=0;i<4;i++)playerStrip.setAlive(i,board.isPlayerAlive(i));
        boolean canUndo=network==null&&board.undoCount()>0;undoButton.setEnabled(canUndo);undoButton.setAlpha(canUndo?1f:.48f);syncCompactControls();refreshClock();
    }
    private void renderMoves(){
        android.text.SpannableStringBuilder result=new android.text.SpannableStringBuilder();
        String[] sides={"红方","绿方","蓝方","金方"};int[] tones={0xffe5a594,0xff9dc3a6,0xff9fbdd0,0xffddbe7e};
        for(int i=0;i<displayedMoves.size();i++){
            int start=result.length();String number=String.format(java.util.Locale.CHINA,"%02d  ",displayedMoves.size()-i);String entry=displayedMoves.get(i);
            result.append(number).append(entry).append("\n");
            if(i==0){result.setSpan(new android.text.style.StyleSpan(Typeface.BOLD),start,result.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);result.setSpan(new android.text.style.ForegroundColorSpan(0xffead3a5),start,result.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
            result.setSpan(new android.text.style.ForegroundColorSpan(0xff94856b),start,start+number.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            for(int owner=0;owner<4;owner++)if(entry.startsWith(sides[owner]))result.setSpan(new android.text.style.ForegroundColorSpan(tones[owner]),start+number.length(),start+number.length()+2,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        moveLog.setText(result.length()==0?EMPTY_HISTORY:result);if(dialogMoves!=null)dialogMoves.setText(moveLog.getText());if(historyScroll!=null)historyScroll.post(()->historyScroll.scrollTo(0,0));
    }
    private void hideModePage(){modePage.setVisibility(android.view.View.GONE);gameUi.setVisibility(android.view.View.VISIBLE);screen.invalidate();updateHud();}
    @Override public void onBackPressed(){if(modePage!=null&&modePage.getVisibility()==android.view.View.VISIBLE){if(matchActive)hideModePage();else super.onBackPressed();}else showExitConfirm();}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused)immersive();}
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_FULLSCREEN|android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}

    private void syncCompactControls(){
        if(phoneUndo==null)return;
        phoneUndo.setEnabled(undoButton.isEnabled());phoneUndo.setAlpha(undoButton.isEnabled()?1f:.48f);
        phoneSound.setSelected(soundOn);phoneSound.setContentDescription(soundOn?"声音已开启":"声音已关闭");((ActionGlyph)phoneSound.getCompoundDrawables()[0]).setMuted(!soundOn);
        compactState.setTextColor(status.getCurrentTextColor());
    }
    private void showHistory(){
        if(historyDialog!=null&&historyDialog.isShowing())return;
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(8),dp(16),dp(8));content.setBackgroundColor(0xff202722);
        TextView connection=text(networkStatus.getText().toString(),11,0xffb9b09a);connection.setMaxLines(3);connection.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);content.addView(connection,new LinearLayout.LayoutParams(-1,-2));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        dialogMoves=text(moveLog.getText().toString(),14,0xffe7d4b0);dialogMoves.setText(moveLog.getText());dialogMoves.setGravity(Gravity.TOP|Gravity.LEFT);dialogMoves.setLineSpacing(dp(6),1.1f);dialogMoves.setPadding(0,dp(8),0,dp(8));scroll.addView(dialogMoves,new ScrollView.LayoutParams(-1,-2));content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        historyDialog=new AlertDialog.Builder(this,android.R.style.Theme_Material_Dialog_Alert).setTitle("棋谱 · 最新在上").setView(content).setPositiveButton("关闭",null).setNeutralButton("棋子：标准 70%",null).create();
        historyDialog.setOnDismissListener(d->{historyDialog=null;dialogMoves=null;});
        historyDialog.setOnShowListener(d->{Button size=historyDialog.getButton(AlertDialog.BUTTON_NEUTRAL);size.setTextColor(0xffe7cd93);historyDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xffe7cd93);size.setText(board.usesLargePieces()?"棋子：大字 84%":"棋子：标准 70%");size.setOnClickListener(v->{board.togglePieceSize();size.setText(board.usesLargePieces()?"棋子：大字 84%":"棋子：标准 70%");});resizeHistoryDialog();});historyDialog.show();
    }
    private void resizeHistoryDialog(){
        if(historyDialog!=null&&historyDialog.getWindow()!=null)historyDialog.getWindow().setLayout(Math.min(dp(560),(int)(screen.getWidth()*.92f)),Math.min(dp(540),(int)(screen.getHeight()*.86f)));
    }
    @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);screen.requestApplyInsets();screen.requestLayout();refreshClock();if(historyDialog!=null)screen.post(this::resizeHistoryDialog);}
    private android.content.SharedPreferences playerPrefs(){return getSharedPreferences("player_names",MODE_PRIVATE);}
    private EditText nameInput(String label,String value){
        EditText input=new EditText(this);input.setHint(label);input.setContentDescription(label);input.setSingleLine(true);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});input.setText(value);input.setSelectAllOnFocus(true);return input;
    }
    private LinearLayout entryContent(){LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(8),dp(20),dp(8));return content;}
    private void presentEntry(String title,LinearLayout content,String confirm,Runnable accepted){
        if(entryDialog!=null&&entryDialog.isShowing())return;
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.addView(content);
        entryDialog=new AlertDialog.Builder(this).setTitle(title).setView(scroll).setPositiveButton(confirm,(dialog,which)->accepted.run()).setNegativeButton("取消",null).create();
        entryDialog.setOnDismissListener(dialog->entryDialog=null);entryDialog.show();
    }
    private void startMatch(String[] names){closeNetwork();board.newGame();clearMoves();board.setLocalPlayer(-1);playerStrip.setPlayerNames(names);matchActive=true;hideModePage();}
    private void showNamesDialog(boolean hostMode){
        LinearLayout content=entryContent();EditText[] names=new EditText[hostMode?1:4];String[] sides={"南 · 红方昵称","西 · 绿方昵称","北 · 蓝方昵称","东 · 金方昵称"};
        for(int i=0;i<names.length;i++){String label=hostMode?"我的昵称（最多12个字）":sides[i]+"（最多12个字）";TextView heading=text(label,12,0xff736450);heading.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);content.addView(heading,new LinearLayout.LayoutParams(-1,dp(28)));names[i]=nameInput(label,playerPrefs().getString(hostMode?"nickname":"local_"+i,hostMode?"棋友":PlayerNames.DEFAULTS[i]));content.addView(names[i],new LinearLayout.LayoutParams(-1,dp(48)));}
        presentEntry(hostMode?"创建房间 · 设置昵称":"同屏对局 · 四位棋友",content,hostMode?"创建房间":"开始对局",()->{
            android.content.SharedPreferences.Editor saved=playerPrefs().edit();String[] values=new String[4];
            if(hostMode){String nickname=PlayerNames.clean(names[0].getText().toString(),"棋友");saved.putString("nickname",nickname).apply();values=new String[]{nickname,"待加入","待加入","待加入"};startMatch(values);host();}
            else{for(int i=0;i<4;i++){values[i]=PlayerNames.clean(names[i].getText().toString(),PlayerNames.DEFAULTS[i]);saved.putString("local_"+i,values[i]);}saved.apply();startMatch(values);networkStatus.setText("同屏模式 · 四人轮流操作");}
        });
    }
    private NetworkManager newNetwork(String nickname){
        final int session=++networkSession;
        return new NetworkManager(this,new NetworkManager.Callback(){
            private void dispatch(Runnable action){runOnUiThread(()->{if(session==networkSession&&network!=null)action.run();});}
            public void onNetworkStatus(String status){dispatch(()->MainActivity.this.onNetworkStatus(status));}
            public void onAssigned(int owner){dispatch(()->MainActivity.this.onAssigned(owner));}
            public void onState(String state){dispatch(()->MainActivity.this.onState(state));}
            public void onHostMove(int owner,int sx,int sy,int tx,int ty){dispatch(()->MainActivity.this.onHostMove(owner,sx,sy,tx,ty));}
            public void onNames(String[] names){dispatch(()->playerStrip.setPlayerNames(names));}
            public void onConnectionClosed(String reason){dispatch(()->connectionLost(reason));}
        },nickname);
    }
    private void host(){hosting=true;network=newNetwork(playerPrefs().getString("nickname","棋友"));board.setLocalPlayer(0);board.setMoveRequester((o,sx,sy,tx,ty)->{if(network!=null)network.hostMove(o,sx,sy,tx,ty);});networkStatus.setText("正在创建房间…");network.host(board);}
    private void joinDialog(){
        LinearLayout content=entryContent();EditText nickname=nameInput("我的昵称（最多12个字）",playerPrefs().getString("nickname","棋友"));content.addView(nickname,new LinearLayout.LayoutParams(-1,dp(48)));
        EditText address=new EditText(this);address.setSingleLine(true);address.setHint("房主地址，例如 192.168.1.8");address.setContentDescription("房主地址");address.setInputType(InputType.TYPE_CLASS_PHONE);address.setText(playerPrefs().getString("last_host",""));content.addView(address,new LinearLayout.LayoutParams(-1,dp(48)));
        TextView hint=text("连接同一 Wi-Fi 或房主热点后加入",11,0xff736450);content.addView(hint,new LinearLayout.LayoutParams(-1,dp(36)));
        presentEntry("加入房间 · 昵称与地址",content,"加入房间",()->{
            String ip=address.getText().toString().trim();if(ip.isEmpty()){toast("请填写房主地址");return;}
            String name=PlayerNames.clean(nickname.getText().toString(),"棋友");playerPrefs().edit().putString("nickname",name).putString("last_host",ip).apply();startMatch(new String[]{"房主","待分配","待分配","待分配"});hosting=false;network=newNetwork(name);board.setLocalPlayer(9);board.setMoveRequester((o,sx,sy,tx,ty)->{if(network!=null)network.clientMove(o,sx,sy,tx,ty);});networkStatus.setText("正在连接 "+ip+"…");network.join(ip);
        });
        final Button enter=entryDialog.getButton(AlertDialog.BUTTON_POSITIVE);enter.setEnabled(!address.getText().toString().trim().isEmpty());address.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){enter.setEnabled(!s.toString().trim().isEmpty());}public void afterTextChanged(android.text.Editable text){}});
    }
    private void showExitConfirm(){
        if(exitDialog!=null&&exitDialog.isShowing())return;
        String message=network==null?"退出后返回模式选择页，当前棋局不会保留。":hosting?"你是房主，退出将关闭房间并通知其他玩家。":"退出后断开房间连接并返回模式选择页。";
        exitDialog=new AlertDialog.Builder(this).setTitle("退出当前对局？").setMessage(message).setNegativeButton("继续对局",null).setPositiveButton("退出对局",(dialog,which)->exitToLobby()).create();exitDialog.setOnDismissListener(dialog->exitDialog=null);exitDialog.show();
    }
    private void exitToLobby(){
        if(historyDialog!=null)historyDialog.dismiss();if(entryDialog!=null)entryDialog.dismiss();if(connectionDialog!=null)connectionDialog.dismiss();closeNetwork();matchActive=false;board.newGame();board.setLocalPlayer(-1);clearMoves();showModePage();
    }
    private void connectionLost(String reason){
        closeNetwork();matchActive=false;board.setLocalPlayer(9);networkStatus.setText(reason);
        if(connectionDialog==null){connectionDialog=new AlertDialog.Builder(this).setTitle("房间连接已结束").setMessage(reason).setPositiveButton("返回大厅",(dialog,which)->exitToLobby()).setCancelable(false).create();connectionDialog.setOnDismissListener(dialog->connectionDialog=null);connectionDialog.show();}
    }
    private void showRules(){new AlertDialog.Builder(this).setTitle("四象棋尊规则").setMessage("红（南）→ 绿（西）→ 蓝（北）→ 金（东）依次行动。点击棋子后，亮点就是可走位置。\n\n主将被吃后，该方棋子全部退出；最后存活的一方获胜。炮吃子需隔一个棋子，马有蹩马腿，象不能离开本方区域。\n\n局域网：一人创建房间，其余三人输入房主显示的地址加入。建议由房主开启手机热点。") .setPositiveButton("开始下棋",null).show();}
    private void closeNetwork(){networkSession++;NetworkManager previous=network;network=null;hosting=false;if(previous!=null)previous.close();board.setMoveRequester(null);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    @Override public void onNetworkStatus(String s){runOnUiThread(()->networkStatus.setText(s));}
    @Override public void onAssigned(int owner){runOnUiThread(()->{board.setLocalPlayer(owner);networkStatus.setText("已入座 · "+board.ownerName(owner));});}
    @Override public void onState(String state){runOnUiThread(()->{String before=board.exportState();board.importState(state);String after=board.exportState();if(!before.equals(after)){try{String[] header=after.split("\\|",2)[0].split(",");int sx=Integer.parseInt(header[1]),sy=Integer.parseInt(header[2]),tx=Integer.parseInt(header[3]),ty=Integer.parseInt(header[4]);if(sx>=0){recordMove("联机落子 "+(char)('A'+sx)+(19-sy)+" → "+(char)('A'+tx)+(19-ty));}else clearMoves();}catch(RuntimeException ignored){}}});}
    @Override public void onHostMove(int owner,int sx,int sy,int tx,int ty){runOnUiThread(()->{if(board.tryNetworkMove(owner,sx,sy,tx,ty)&&network!=null)network.broadcast(board.exportState());});}
    @Override public void onDestroy(){clockHandler.removeCallbacks(clockTick);closeNetwork();super.onDestroy();}
    private final Runnable clockTick=new Runnable(){@Override public void run(){if(matchActive){turnSeconds++;refreshClock();}clockHandler.postDelayed(this,1000);}};
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}











