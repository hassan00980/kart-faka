package com.khammeni.game;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.parseColor("#0c2119"));
        int pad = dp(24);
        root.setPadding(pad, dp(70), pad, pad);

        TextView title = new TextView(this);
        title.setText("🃏 كرت فكة");
        title.setTextSize(46);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#e6c873"));
        title.setGravity(Gravity.CENTER);

        TextView sub = new TextView(this);
        sub.setText("لعبة تخمين شخصيات + لعبة الجاسوس 🕵️ + مزاد كرة ⚽\nكل اللاعيبة على نفس الواي فاي أو الهوت سبوت");
        sub.setTextSize(15);
        sub.setTextColor(Color.parseColor("#a9c0b2"));
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(12);
        sub.setLayoutParams(subLp);

        Button host = bigButton("🛠️ استضف لعبة", "شغّل اللعبة على موبايلك\nوخلّي صحابك ينضموا ليك تلقائيًا");
        host.setOnClickListener(v -> startActivity(new Intent(this, HostActivity.class)));

        Button join = bigButton("👥 انضم كلاعب", "اتصل بأوضة صاحبك اللي بيستضيف");
        join.setOnClickListener(v -> startActivity(new Intent(this, JoinActivity.class)));

        TextView note = new TextView(this);
        note.setText("💡 وحّد منكم يعمل هوت سبوت → الباقيين يتصلوا بيه →\nالكل يفتح كرت فكة ويبدأ اللعب");
        note.setTextSize(13);
        note.setTextColor(Color.parseColor("#7e988a"));
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        noteLp.topMargin = dp(20);
        note.setLayoutParams(noteLp);

        TextView credit = new TextView(this);
        credit.setText("© Hassan Abd EL-zaher — جميع الحقوق محفوظة");
        credit.setTextSize(11);
        credit.setTextColor(Color.parseColor("#7e988a"));
        credit.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams creditLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        creditLp.topMargin = dp(24);
        credit.setLayoutParams(creditLp);

        root.addView(title);
        root.addView(sub);
        root.addView(host);
        root.addView(join);
        root.addView(note);
        root.addView(credit);
        setContentView(root);
    }

    private Button bigButton(String text, String sub) {
        Button b = new Button(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(20);
        b.setLayoutParams(lp);
        b.setText("\n" + text + "\n" + sub + "\n");
        b.setTextSize(18);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(Color.parseColor("#231a05"));
        b.setGravity(Gravity.CENTER);

        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#e6c873"), Color.parseColor("#c9a227")});
        g.setCornerRadius(dp(18));
        g.setStroke(dp(1), Color.parseColor("#8f7220"));
        b.setBackground(g);
        return b;
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}