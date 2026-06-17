package com.diplomacia.bot;

import android.app.Activity;
import android.graphics.Color;
import android.provider.Settings;
import android.util.Base64;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebView.WebViewTransport;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginWebViewActivity extends Activity {
    private static final Pattern JWT_PATTERN = Pattern.compile("eyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}");
    private static final String CHROME_MOBILE_UA = "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout root;
    private WebView webView;
    private WebView popupWebView;
    private WebView activeWebView;
    private TextView statusText;
    private int accountIndex;
    private boolean captured;
    private boolean exchangingGoogleToken;

    private final Runnable pollToken = new Runnable() {
        @Override
        public void run() {
            if (captured || activeWebView == null) {
                return;
            }
            scanPageForToken(activeWebView);
            handler.postDelayed(this, 1500L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountIndex = getIntent().getIntExtra("accountIndex", 0);
        buildUi();
        webView.loadUrl(BotConfig.DEFAULT_BASE_URL);
        handler.postDelayed(pollToken, 1500L);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.destroy();
        }
        if (popupWebView != null) {
            popupWebView.destroy();
        }
        super.onDestroy();
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(13, 22, 40));

        statusText = new TextView(this);
        statusText.setText("Giris yap. Token otomatik aranıyor...");
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(15);
        statusText.setPadding(18, 18, 18, 18);
        root.addView(statusText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        webView = new WebView(this);
        configureWebView(webView);
        activeWebView = webView;
        root.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private void configureWebView(WebView view) {
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setUserAgentString(CHROME_MOBILE_UA);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(view, true);
        view.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                statusText.setText("Google penceresi aciliyor...");
                if (popupWebView != null) {
                    root.removeView(popupWebView);
                    popupWebView.destroy();
                }
                popupWebView = new WebView(LoginWebViewActivity.this);
                configureWebView(popupWebView);
                activeWebView = popupWebView;
                root.addView(popupWebView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
                WebViewTransport transport = (WebViewTransport) resultMsg.obj;
                transport.setWebView(popupWebView);
                resultMsg.sendToTarget();
                return true;
            }

            @Override
            public void onCloseWindow(WebView window) {
                if (window == popupWebView) {
                    root.removeView(popupWebView);
                    popupWebView.destroy();
                    popupWebView = null;
                    activeWebView = webView;
                    statusText.setText("Google penceresi kapandi. Token aranıyor...");
                    scanPageForToken(webView);
                }
            }
        });
        view.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                scanText(request.getUrl().toString());
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                statusText.setText("Sayfa yuklendi. Token aranıyor...");
                scanText(url);
                scanPageForToken(view);
            }
        });
    }

    private void scanPageForToken(WebView target) {
        if (target == null) {
            return;
        }
        String script = "(() => {" +
                "const safe = (fn) => { try { return fn(); } catch(e) { return ''; } };" +
                "return [" +
                "location.href," +
                "safe(() => JSON.stringify({...localStorage}))," +
                "safe(() => JSON.stringify({...sessionStorage}))," +
                "safe(() => document.cookie)," +
                "safe(() => document.body ? document.body.innerText.slice(0, 8000) : '')" +
                "].join('\\n');" +
                "})()";
        target.evaluateJavascript(script, value -> scanText(decodeJsString(value)));
    }

    private String decodeJsString(String value) {
        if (value == null || "null".equals(value)) {
            return "";
        }
        try {
            return new JSONArray("[" + value + "]").getString(0);
        } catch (Exception ignored) {
            return value;
        }
    }

    private void scanText(String text) {
        if (captured || text == null) {
            return;
        }
        Matcher matcher = JWT_PATTERN.matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            if (isDiplomaciaToken(token)) {
                saveToken(token);
                return;
            }
            if (isGoogleToken(token)) {
                exchangeGoogleIdToken(token);
                return;
            }
        }
    }

    private void saveToken(String token) {
        captured = true;
        BotConfig config = BotConfig.load(this, accountIndex);
        new BotConfig(accountIndex, token, config.skill, config.type, config.enabled, config.queue).save(this);
        LogStore.append(this, "Hesap " + (accountIndex + 1) + " Diplomacia token yakalandi.");
        statusText.setText("Token yakalandi. Pencere kapatiliyor...");
        handler.postDelayed(this::finish, 700L);
    }

    private void exchangeGoogleIdToken(String idToken) {
        if (exchangingGoogleToken || captured) {
            return;
        }
        exchangingGoogleToken = true;
        statusText.setText("Google token alindi. Diplomacia token isteniyor...");
        new Thread(() -> {
            try {
                JSONObject payload = decodeJwtPayload(idToken);
                JSONObject body = new JSONObject();
                body.put("id_token", idToken);
                body.put("google_picture", payload.optString("picture", null));
                body.put("device_fingerprint", deviceFingerprint());
                body.put("locale", "tr");

                HttpURLConnection connection = (HttpURLConnection) new URL(BotConfig.DEFAULT_BASE_URL + "/api/auth/google").openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(bytes);
                }

                int status = connection.getResponseCode();
                String response = DiplomaciaApi.readResponse(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
                JSONObject json = response.trim().isEmpty() ? new JSONObject() : new JSONObject(response);
                String token = json.optString("token", "");
                runOnUiThread(() -> {
                    exchangingGoogleToken = false;
                    if (status >= 200 && status < 300 && isDiplomaciaToken(token)) {
                        saveToken(token);
                    } else {
                        statusText.setText("Diplomacia token alinamadi: " + json.optString("error", "HTTP " + status));
                        LogStore.append(this, "Hesap " + (accountIndex + 1) + " Diplomacia token alinamadi: " + json.optString("error", "HTTP " + status));
                    }
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    exchangingGoogleToken = false;
                    statusText.setText("Token alma hatasi: " + error.getMessage());
                    LogStore.append(this, "Hesap " + (accountIndex + 1) + " token alma hatasi: " + error.getMessage());
                });
            }
        }).start();
    }

    private JSONObject deviceFingerprint() throws Exception {
        JSONObject object = new JSONObject();
        object.put("model", android.os.Build.MODEL);
        object.put("brand", android.os.Build.BRAND);
        object.put("os", "Android " + android.os.Build.VERSION.RELEASE);
        object.put("platform", "android");
        object.put("device_id", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
        return object;
    }

    private boolean isGoogleToken(String token) {
        JSONObject payload = decodeJwtPayload(token);
        return "https://accounts.google.com".equals(payload.optString("iss")) || payload.has("azp");
    }

    private boolean isDiplomaciaToken(String token) {
        JSONObject payload = decodeJwtPayload(token);
        return payload.has("id") && payload.has("username") && !payload.has("iss");
    }

    private JSONObject decodeJwtPayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return new JSONObject();
            }
            byte[] decoded = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            return new JSONObject(new String(decoded, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }
}
