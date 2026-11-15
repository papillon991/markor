/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.web;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.util.FontStacks;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.web.GsWebViewClient;

public class MarkorWebViewClient extends GsWebViewClient {
    protected final Activity _activity;

    public MarkorWebViewClient(final WebView webView, final Activity activity) {
        super(webView);
        _activity = activity;
    }

    // Serves the bundled font stacks (assets/fontstacks) to the view mode, with CORS header as the page origin is file://
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        final String name = FontStacks.assetNameForUrl(request.getUrl().toString());
        if (name != null) {
            try {
                final WebResourceResponse response = new WebResourceResponse("font/ttf", null,
                        view.getContext().getAssets().open(FontStacks.ASSET_DIR + "/" + name));
                final java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Access-Control-Allow-Origin", "*");
                response.setResponseHeaders(headers);
                return response;
            } catch (java.io.IOException ignored) {
            }
        }
        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        try {
            Context context = view.getContext();

            if (url.equals("about:blank")) {
                view.reload();
                return true;
            }
            if (url.startsWith("file:///android_asset/")) {
                return false;
            } else if (url.startsWith("file://")) {
                DocumentActivity.launch(_activity, Uri.parse(url));
            } else {
                MarkorContextUtils su = new MarkorContextUtils(_activity);
                AppSettings settings = AppSettings.get(_activity);
                if (!settings.isOpenLinksWithChromeCustomTabs() || (settings.isOpenLinksWithChromeCustomTabs() && !su.openWebpageInChromeCustomTab(context, url))) {
                    su.openWebpageInExternalBrowser(context, url);
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return true;
    }
}
