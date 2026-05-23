package com.example.nativeapk;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.SharedPreferences;
import android.util.TypedValue;

public class MainActivity extends Activity {
    private int counter = 0;
    private TextView counterText;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("app_state", MODE_PRIVATE);
        counter = prefs.getInt("counter", 0);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(248, 250, 252));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(48), dp(24), dp(32));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = new TextView(this);
        title.setText("Native APK Template");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.rgb(15, 23, 42));
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView subtitle = new TextView(this);
        subtitle.setText("App Android native thuần Java. Không WebView, không HTML.");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        subtitle.setTextColor(Color.rgb(71, 85, 105));
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.setMargins(0, dp(12), 0, dp(32));
        root.addView(subtitle, subtitleParams);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(20), dp(24), dp(20), dp(24));
        card.setBackground(makeRoundedRect(Color.WHITE, dp(18)));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(24));
        root.addView(card, cardParams);

        counterText = new TextView(this);
        counterText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        counterText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        counterText.setTextColor(Color.rgb(15, 23, 42));
        counterText.setGravity(Gravity.CENTER);
        card.addView(counterText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        updateCounterText();

        Button addButton = makeButton("Tăng số đếm");
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        buttonParams.setMargins(0, dp(24), 0, 0);
        card.addView(addButton, buttonParams);
        addButton.setOnClickListener(v -> {
            counter++;
            saveCounter();
            updateCounterText();
        });

        Button resetButton = makeButton("Reset");
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        resetParams.setMargins(0, dp(12), 0, 0);
        card.addView(resetButton, resetParams);
        resetButton.setOnClickListener(v -> {
            counter = 0;
            saveCounter();
            updateCounterText();
            Toast.makeText(this, "Đã reset", Toast.LENGTH_SHORT).show();
        });

        TextView note = new TextView(this);
        note.setText("Anh sửa MainActivity.java để thay giao diện và logic app. GitHub Actions sẽ tự build APK debug khi push hoặc chạy thủ công.");
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        note.setTextColor(Color.rgb(71, 85, 105));
        note.setGravity(Gravity.CENTER);
        root.addView(note, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        setContentView(scrollView);
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setBackground(makeRoundedRect(Color.rgb(37, 99, 235), dp(14)));
        return button;
    }

    private GradientDrawable makeRoundedRect(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private void updateCounterText() {
        counterText.setText("Số đếm: " + counter);
    }

    private void saveCounter() {
        prefs.edit().putInt("counter", counter).apply();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}
