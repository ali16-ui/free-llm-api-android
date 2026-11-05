package app.freellmapi.webview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

/** Thin WebView shell around a FreeLLMAPI server (Termux, LAN, Docker or hosted). */
public class MainActivity extends Activity {
    private static final String PREFS = "freellmapi";
    private static final String KEY_URL = "server_url";
    private static final String DEFAULT_URL = "http://127.0.0.1:5173";
    private static final int REQ_FILE = 42;

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        FrameLayout root = new FrameLayout(this);
        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String host = r.getUrl().getHost();
                String cur = Uri.parse(savedUrl()).getHost();
                if (host != null && host.equals(cur)) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl())); } catch (Exception ignored) {}
                return true;
            }
            @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) {
                    v.loadData("<body style='background:#0b0b0f;color:#ddd;font-family:sans-serif;padding:24px'>"
                        + "<h2>Can't reach server</h2><p>" + e.getDescription() + "</p>"
                        + "<p>Check that FreeLLMAPI is running, then pull down / reopen. "
                        + "Use the system Back button's long-press menu or reopen the app to change the URL.</p></body>",
                        "text/html", "UTF-8");
                }
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                try { startActivityForResult(p.createIntent(), REQ_FILE); }
                catch (Exception e) { fileCallback = null; return false; }
                return true;
            }
        });

        if (getPrefs().getString(KEY_URL, null) == null) promptForUrl(true);
        else web.loadUrl(savedUrl());
    }

    private SharedPreferences getPrefs() { return getSharedPreferences(PREFS, MODE_PRIVATE); }
    private String savedUrl() { return getPrefs().getString(KEY_URL, DEFAULT_URL); }

    private void promptForUrl(boolean first) {
        final EditText in = new EditText(this);
        in.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        in.setText(savedUrl());
        new AlertDialog.Builder(this)
            .setTitle("FreeLLMAPI server URL")
            .setMessage("Address of your running FreeLLMAPI dashboard, e.g. http://127.0.0.1:5173 (Termux), http://192.168.1.20:3001 or https://your-host.")
            .setView(in)
            .setCancelable(!first)
            .setPositiveButton("Connect", (d, w) -> {
                String u = in.getText().toString().trim();
                if (!u.matches("(?i)^https?://.+")) u = "http://" + u;
                getPrefs().edit().putString(KEY_URL, u).apply();
                web.loadUrl(u);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override public boolean onCreateOptionsMenu(android.view.Menu m) { return false; }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override public boolean onKeyLongPress(int code, android.view.KeyEvent e) {
        if (code == android.view.KeyEvent.KEYCODE_BACK) { promptForUrl(false); return true; }
        return super.onKeyLongPress(code, e);
    }

    @Override public boolean onKeyDown(int code, android.view.KeyEvent e) {
        if (code == android.view.KeyEvent.KEYCODE_BACK) e.startTracking();
        return super.onKeyDown(code, e);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_FILE && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            fileCallback = null;
        } else super.onActivityResult(req, res, data);
    }
}
