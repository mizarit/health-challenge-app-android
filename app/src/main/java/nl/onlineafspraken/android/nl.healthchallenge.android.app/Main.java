package nl.healthchallenge.android.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.KeyEvent;

import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import nl.healthchallenge.android.app.AndroidJS;
import nl.healthchallenge.android.app.CustomWebViewClient;
import nl.healthchallenge.android.app.GcmActivity;
import nl.healthchallenge.android.app.SensorActivity;

public class Main extends GcmActivity {
	WebView myWebView;
	ProgressBar myProgressBar;
	ImageView mySplash;
	RelativeLayout mySplashLayout;
    Context context;
    boolean hasSensor;

    static final String TAG = "HealthChallenge";

    public static final String PROPERTY_PAYLOAD = "payload";
    public static final String PROPERTY_PAYLOAD_ARGS = "payload_args";

    nl.healthchallenge.android.app.AndroidJS myAndroidJS;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.main );

        final SharedPreferences prefs = getSharedPreferences(nl.healthchallenge.android.app.Main.class.getSimpleName(), Context.MODE_PRIVATE);

        initGCM();
        hasSensor = (initSensors() != null);

        CookieManager.getInstance().setAcceptCookie(true);

        myProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        mySplash = (ImageView) findViewById(R.id.splash);
        myWebView = (WebView) findViewById(R.id.webview);
   	 	mySplashLayout = (RelativeLayout) findViewById(R.id.InnerRelativeOverallLayout);

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUseWideViewPort(true);

        String appCachePath = getApplicationContext().getCacheDir().getAbsolutePath();
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCachePath(appCachePath);



        myWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                myProgressBar.setProgress(progress);

                if (progress == 100) {
                    mySplashLayout.setVisibility(View.GONE);
                    myProgressBar.setVisibility(View.GONE);
                    mySplash.setVisibility(View.GONE);
                }
            }
        });

        myWebView.setWebViewClient(new CustomWebViewClient() { });

        myAndroidJS = new nl.healthchallenge.android.app.AndroidJS(getApplicationContext());
        myWebView.addJavascriptInterface(myAndroidJS, "Android");

        String url = this.getString(R.string.app_url);
        url = url.concat("?device=android");
        if (checkPlayServices()) {
            url = url.concat("&android_id=").concat(regid);
        }
        if (hasSensor) {
            url = url.concat("&sensor=1");
        }

        myWebView.loadUrl(url);

        (new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                while (!Thread.interrupted())
                    try
                    {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() // start actions in UI thread
                        {

                            @Override
                            public void run()
                            {

                                String payload = prefs.getString(PROPERTY_PAYLOAD, "");
                                String payload_args = prefs.getString(PROPERTY_PAYLOAD_ARGS, "");
                                if (!payload.isEmpty()) {
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putString(PROPERTY_PAYLOAD, "");
                                    editor.putString(PROPERTY_PAYLOAD_ARGS, "");
                                    editor.commit();

                                    Log.i(TAG, payload.toString());
                                    Log.i(TAG, payload_args.toString());

                                    String javascript = "javascript:"+payload.toString()+"("+payload_args.toString()+");";
                                    Log.i(TAG, javascript);
                                    myWebView.loadUrl(javascript);


                                }
                            }
                        });
                    }
                    catch (InterruptedException e)
                    {
                        // ooops
                    }
            }
        })).start(); // the while thread will start in BG thread

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            switch(keyCode)
            {
                case KeyEvent.KEYCODE_BACK:
                    if(myWebView.canGoBack()){
                        myWebView.goBack();
                    }else{
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}