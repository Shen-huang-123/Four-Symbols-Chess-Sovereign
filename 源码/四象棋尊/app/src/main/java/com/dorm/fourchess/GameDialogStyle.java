package com.dorm.fourchess;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/** Shared window and field treatment; retains native dialog navigation and semantics. */
final class GameDialogStyle {
    private static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    private static GradientDrawable surface(Context c,int fill,int edge,float radius){
        GradientDrawable bg=new GradientDrawable();bg.setColor(fill);bg.setCornerRadius(dp(c,radius));bg.setStroke(dp(c,1),edge);return bg;
    }
    static void title(AlertDialog dialog,String title){
        Context c=dialog.getContext();TextView label=new TextView(c);label.setTag("game_dialog_title");label.setText(title);label.setTextSize(GameDesign.TYPE_TITLE);label.setTextColor(GameDesign.GOLD_BRIGHT);
        label.setTypeface(GameDesign.EMPHASIS);label.setPadding(dp(c,GameDesign.SPACE_XL),dp(c,22),dp(c,GameDesign.SPACE_XL),dp(c,15));if(android.os.Build.VERSION.SDK_INT>=28)label.setAccessibilityHeading(true);dialog.setCustomTitle(label);
    }
    private static void content(View view){
        Context c=view.getContext();
        if(view instanceof EditText){
            EditText field=(EditText)view;field.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI|android.view.inputmethod.EditorInfo.IME_ACTION_DONE);field.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_DONE){((android.view.inputmethod.InputMethodManager)c.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);return true;}return false;});field.setTextColor(GameDesign.TEXT);field.setHintTextColor(GameDesign.MUTED);field.setTextSize(16);field.setPadding(dp(c,GameDesign.SPACE_MD),dp(c,4),dp(c,GameDesign.SPACE_MD),dp(c,4));
            android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
            states.addState(new int[]{android.R.attr.state_focused},surface(c,GameDesign.PANEL_RAISED,GameDesign.GOLD,GameDesign.RADIUS_SM));states.addState(new int[]{},surface(c,GameDesign.INK_TEAL,GameDesign.EDGE_QUIET,GameDesign.RADIUS_SM));field.setBackground(states);
            if(field.getLayoutParams() instanceof ViewGroup.MarginLayoutParams){ViewGroup.MarginLayoutParams lp=(ViewGroup.MarginLayoutParams)field.getLayoutParams();lp.bottomMargin=dp(c,GameDesign.SPACE_SM);field.setLayoutParams(lp);}
        }else if(view instanceof TextView)((TextView)view).setTextColor(GameDesign.SECONDARY);
        else if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)content(((ViewGroup)view).getChildAt(i));
    }
    static void apply(AlertDialog dialog,View fields){
        Context c=dialog.getContext();if(fields!=null)content(fields);
        Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(surface(c,GameDesign.PANEL_RAISED,GameDesign.EDGE,GameDesign.RADIUS_DIALOG));window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setDimAmount(.55f);window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}
        if(fields!=null&&window!=null){
            View decor=window.getDecorView(),heading=decor.findViewWithTag("game_dialog_title");int panelId=c.getResources().getIdentifier("buttonPanel","id","android");View actions=decor.findViewById(panelId);
            decor.getViewTreeObserver().addOnGlobalLayoutListener(()->{
                android.graphics.Rect visible=new android.graphics.Rect();decor.getWindowVisibleDisplayFrame(visible);
                // At extreme landscape IME heights, editing takes precedence; Done restores all actions.
                android.view.WindowInsets insets=decor.getRootWindowInsets();boolean keyboard=android.os.Build.VERSION.SDK_INT>=30?insets!=null&&insets.isVisible(android.view.WindowInsets.Type.ime()):visible.height()+dp(c,100)<c.getResources().getDisplayMetrics().heightPixels;
                boolean editing=keyboard&&fields.hasFocus()&&visible.height()>0&&visible.height()<dp(c,200);
                int state=editing?View.GONE:View.VISIBLE;
                if(heading!=null&&heading.getVisibility()!=state)heading.setVisibility(state);
                if(actions!=null&&actions.getVisibility()!=state)actions.setVisibility(state);
                if(editing&&fields.getParent() instanceof android.widget.ScrollView){View focus=fields.findFocus();if(focus!=null)((android.widget.ScrollView)fields.getParent()).scrollTo(0,Math.max(0,focus.getTop()-dp(c,4)));}
            });
        }
        TextView message=dialog.findViewById(android.R.id.message);if(message!=null){message.setTextColor(GameDesign.TEXT);message.setTextSize(GameDesign.TYPE_BODY);message.setLineSpacing(dp(c,3),1);}
        for(int which:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL}){
            Button button=dialog.getButton(which);if(button==null)continue;boolean primary=which==AlertDialog.BUTTON_POSITIVE;
            button.setAllCaps(false);button.setTextSize(14);button.setMinHeight(dp(c,48));button.setPadding(dp(c,GameDesign.SPACE_LG),0,dp(c,GameDesign.SPACE_LG),0);
            button.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{GameDesign.DISABLED,primary?GameDesign.GOLD_BRIGHT:GameDesign.SECONDARY}));
            button.setBackground(new RippleDrawable(ColorStateList.valueOf(GameDesign.RIPPLE),surface(c,primary?GameDesign.INK_TEAL:0x00172630,primary?GameDesign.EDGE:0x00172630,8),null));
        }
    }
}
