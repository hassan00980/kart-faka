package com.khammeni.game;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class JoinActivity extends Activity {

    private TextView status;
    private LinearLayout hostsBox;
    private WebView web;
    private ValueCallback<Uri[]> fileCb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#1a1040"));
        root.setPadding(dp(16), dp(20), dp(16), 0);
        setContentView(root);

        TextView title = new TextView(this);
        title.setText("👥 انضم كلاعب");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        status = new TextView(this);
        status.setText("بنفشش عن أوضة قريبة... 🔎");
        status.setTextSize(15);
        status.setTextColor(Color.WHITE);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(14), 0, dp(6));
        root.addView(status);

        hostsBox = new LinearLayout(this);
        hostsBox.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroller = new ScrollView(this);
        scroller.addView(hostsBox, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        slp.topMargin = dp(6);
        root.addView(scroller, slp);

        EditText ipInput = new EditText(this);
        ipInput.setHint("اكتب عنوان السيرفر يدويًا... مثلاً 192.168.43.1");
        ipInput.setTextColor(Color.WHITE);
        ipInput.setHintTextColor(Color.parseColor("#8a87a0"));
        ipInput.setSingleLine(true);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ilp.topMargin = dp(10);
        ipInput.setLayoutParams(ilp);
        root.addView(ipInput);

        Button manual = new Button(this);
        manual.setText("اتصل بالعنوان ده");
        manual.setTextColor(Color.WHITE);
        manual.setBackgroundColor(Color.parseColor("#7b6cff"));
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mlp.topMargin = dp(8);
        manual.setLayoutParams(mlp);
        manual.setOnClickListener(v -> {
            String ip = ipInput.getText().toString().trim()
                    .replace("http://", "").replace("/", "");
            if (!ip.isEmpty()) openGame("http://" + ip + ":3000");
        });
        root.addView(manual);

        // زر مسح رمز QR — Feature 8 (Discovery + QR)
        Button scanQr = new Button(this);
        scanQr.setText("🧾 امسح QR");
        scanQr.setTextColor(Color.WHITE);
        scanQr.setBackgroundColor(Color.parseColor("#2ec9a8"));
        LinearLayout.LayoutParams slp2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp2.topMargin = dp(8);
        scanQr.setLayoutParams(slp2);
        scanQr.setOnClickListener(v -> launchQrScan());
        root.addView(scanQr);

        // فحص تلقائي للبيئة
        new Thread(() -> {
            List<String> found = Discovery.scan(6000, this);
            new Handler(Looper.getMainLooper()).post(() -> showHosts(found));
        }).start();
    }

    /** مسح رمز QR (Feature 8): يفتح الماسح القياسي لو متسطب، وإلا يفتح الكاميرا */
    private void launchQrScan() {
        try {
            Intent scan = new Intent("com.google.zxing.client.android.SCAN");
            scan.putExtra("SCAN_MODE", "QR_CODE_MODE");
            if (scan.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(scan, 777);
                return;
            }
        } catch (Exception ignored) { }
        Intent camera = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (camera.resolveActivity(getPackageManager()) != null) {
            Toast.makeText(this, "📷 التقط صورة للـ QR ثم اكتب العنوان يدويًا", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "مفيش ماسح متسطب — اكتب عنوان السيرفر يدويًا", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 778) {
            // نتيجة اختيار ملف من WebView (ميديا الشات)
            if (fileCb != null) {
                fileCb.onReceiveValue(resultCode == RESULT_OK && data != null
                        ? WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                        : null);
                fileCb = null;
            }
            return;
        }
        if (requestCode != 777 || data == null) return;
        if (resultCode == RESULT_OK) {
            String qr = data.getStringExtra("SCAN_RESULT");
            if (qr != null && !qr.trim().isEmpty()) {
                String url = qr.trim();
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "http://" + url.replaceAll("^[a-zA-Z]+://", "").replaceAll("/.*$", "") + ":3000";
                }
                openGame(url.replaceAll("/$", ""));
            }
        }
    }

    private void showHosts(List<String> ips) {
        if (isFinishing()) return;
        hostsBox.removeAllViews();
        if (ips.isEmpty()) {
            status.setText("مفيش أوضة قريبة اتلاقت 😕\nتأكد إن صاحبك بيستضيف وانت على نفس الواي فاي/الهوت سبوت");
            return;
        }
        status.setText("لقينا " + ips.size() + " أوضة قريبة 👇");
        for (String ip : ips) {
            Button b = new Button(this);
            b.setText("اتصل بـ " + ip);
            b.setTextColor(Color.WHITE);
            b.setBackgroundColor(Color.parseColor("#2ec973"));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(8);
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> openGame("http://" + ip + ":3000"));
            hostsBox.addView(b);
        }
    }

    private void openGame(String url) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#1a1040"));
        setContentView(root);

        TextView bar = new TextView(this);
        bar.setText("متصل بـ " + url);
        bar.setTextSize(14);
        bar.setTextColor(Color.WHITE);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(0, dp(10), 0, dp(6));
        root.addView(bar);

        Button back = new Button(this);
        back.setText("⬅️ رجوع");
        back.setTextColor(Color.WHITE);
        back.setBackgroundColor(Color.parseColor("#5a5480"));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.setMargins(dp(16), 0, dp(16), dp(8));
        back.setLayoutParams(blp);
        back.setOnClickListener(v -> recreate());
        root.addView(back);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        // ميديا في الشات (Feature 2): اختيار ملفات + إذن كاميرا/ميكروفون في WebView
        s.setMediaPlaybackRequiresUserGesture(true);
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
        web.loadUrl(url);
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