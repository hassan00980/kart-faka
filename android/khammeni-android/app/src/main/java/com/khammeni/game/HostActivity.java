package com.khammeni.game;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;

public class HostActivity extends Activity {

    private WebView web;
    private TextView status;
    private boolean startedLoading = false;
    private ValueCallback<Uri[]> fileCb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#1a1040"));
        root.setPadding(0, 0, 0, 0);
        setContentView(root);

        status = new TextView(this);
        status.setText("⏳ بيجهز السيرفر...");
        status.setTextSize(15);
        status.setTextColor(Color.WHITE);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(12), dp(10), dp(12), dp(6));
        root.addView(status);

        Button stopBtn = new Button(this);
        stopBtn.setText("🛑 إيقاف الاستضافة والخروج");
        stopBtn.setTextColor(Color.WHITE);
        stopBtn.setBackgroundColor(Color.parseColor("#ff4d4d"));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.setMargins(dp(16), 0, dp(16), dp(8));
        stopBtn.setLayoutParams(slp);
        stopBtn.setOnClickListener(v -> {
            stopService(new Intent(this, HostService.class));
            finish();
        });
        root.addView(stopBtn);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        // ميديا في الشات (Feature 2): ملفات + إذن كاميرا/ميكروفون
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView wv, ValueCallback<Uri[]> filePathCallback, FileChooserParams params) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = filePathCallback;
                Intent i = params.createIntent();
                try {
                    startActivityForResult(i, 778);
                } catch (Exception e) {
                    fileCb = null;
                    return false;
                }
                return true;
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }
        });
        web.setWebViewClient(new WebViewClient());
        root.addView(web, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        // طلب إذن الإشعارات (أندرويد 13+)
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        Intent svc = new Intent(this, HostService.class);
        startForegroundService(svc);

        // استنى السيرفر يجهز وبعدين افتح الصفحة
        new Thread(() -> {
            boolean up = false;
            for (int i = 0; i < 40 && !up; i++) {
                try {
                    Thread.sleep(250);
                    HttpURLConnection c = (HttpURLConnection) new URL("http://localhost:3000/api/info").openConnection();
                    c.setConnectTimeout(500);
                    c.setReadTimeout(500);
                    up = c.getResponseCode() == 200;
                    c.disconnect();
                } catch (Exception ignored) { }
            }
            final boolean ready = up;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (ready) {
                    startedLoading = true;
                    web.loadUrl("http://localhost:3000");
                    status.setText(hostText());
                } else {
                    status.setText("⚠️ السيرفر ما اشتغلش... جرب تعيد فتح الشاشة");
                    Toast.makeText(this, "السيرفر ما اشتغلش 😢", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private String hostText() {
        java.util.List<String> ips = HostService.getIpsText();
        StringBuilder b = new StringBuilder("📡 الموبايل ده هو السيرفر!\n");
        b.append("صاحبك اللي بيستضيف... إنت! صحابك الحواليك يوصلوا ليك أوتوماتيك.\n");
        if (!ips.isEmpty()) {
            b.append("العناوين: ");
            b.append(String.join("، ", ips));
        }
        return b.toString();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 778) {
            if (fileCb != null) {
                fileCb.onReceiveValue(resultCode == RESULT_OK && data != null
                        ? WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                        : null);
                fileCb = null;
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}