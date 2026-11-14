package com.modaw249.apps;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int FILE_CHOOSER_REQUEST = 2490;
    private static final String STORE_URL =
            "https://modaw249.github.io/249apps/";

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        CookieManager.getInstance().setAcceptCookie(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, WebResourceRequest request) {

                Uri uri = request.getUrl();
                String scheme = uri.getScheme();

                if ("https".equalsIgnoreCase(scheme)
                        || "http".equalsIgnoreCase(scheme)) {
                    return false;
                }

                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (ActivityNotFoundException ignored) {
                    Toast.makeText(MainActivity.this,
                            "لا يوجد تطبيق لفتح هذا الرابط",
                            Toast.LENGTH_SHORT).show();
                }

                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }

                filePathCallback = callback;

                try {
                    startActivityForResult(
                            params.createIntent(),
                            FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException e) {
                    filePathCallback = null;
                    return false;
                }
            }
        });

        webView.setDownloadListener(
                (url, userAgent, contentDisposition,
                 mimeType, contentLength) -> {

            try {
                DownloadManager.Request request =
                        new DownloadManager.Request(Uri.parse(url));

                request.setMimeType(mimeType);

                String cookies =
                        CookieManager.getInstance().getCookie(url);

                if (cookies != null) {
                    request.addRequestHeader("Cookie", cookies);
                }

                if (userAgent != null) {
                    request.addRequestHeader("User-Agent", userAgent);
                }

                request.setTitle("تحميل من 249apps");
                request.setDescription("جارٍ تنزيل الملف");

                request.setNotificationVisibility(
                        DownloadManager.Request
                                .VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                String fileName = URLUtil.guessFileName(
                        url, contentDisposition, mimeType);

                request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        fileName);

                DownloadManager manager =
                        (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

                if (manager == null) {
                    throw new IllegalStateException(
                            "Download service is unavailable");
                }

                manager.enqueue(request);

                Toast.makeText(this,
                        "بدأ تحميل الملف",
                        Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Toast.makeText(this,
                        "تعذر بدء التحميل",
                        Toast.LENGTH_LONG).show();
            }
        });

        if (savedInstanceState == null) {
            webView.loadUrl(STORE_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST
                && filePathCallback != null) {

            filePathCallback.onReceiveValue(
                    WebChromeClient.FileChooserParams
                            .parseResult(resultCode, data));

            filePathCallback = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (webView != null) {
            webView.saveState(outState);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }

        if (webView != null) {
            webView.destroy();
        }

        super.onDestroy();
    }
}