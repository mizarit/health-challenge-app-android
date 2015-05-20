package nl.healthchallenge.android.applite;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
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

import nl.healthchallenge.android.applite.AndroidJS;
import nl.healthchallenge.android.applite.CustomWebViewClient;
import nl.healthchallenge.android.applite.GcmActivity;
import nl.healthchallenge.android.applite.SensorActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Main extends GcmActivity {
	WebView myWebView;
	ProgressBar myProgressBar;
	ImageView mySplash;
	RelativeLayout mySplashLayout;
    Context context;
    boolean hasSensor;

    private SensorManager mSensorManager;
    private Sensor mStepCounterSensor;

    static final String TAG = "HealthChallenge";

    nl.healthchallenge.android.applite.AndroidJS myAndroidJS;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.main );

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

        myAndroidJS = new nl.healthchallenge.android.applite.AndroidJS(getApplicationContext());
        myWebView.addJavascriptInterface(myAndroidJS, "Android");

        String url = this.getString(R.string.app_url_alt);
        url = url.concat("?device=android");
        if (checkPlayServices()) {
            url = url.concat("&android_id=").concat(regid);
        }
        if (hasSensor) {
            url = url.concat("&sensor=1");
        }

        int steps = getSharedPreferences(nl.healthchallenge.android.applite.Main.class.getSimpleName(), Context.MODE_PRIVATE).getInt("pedometer", 0);
        Log.i(TAG, "Steps from cache: " + String.valueOf(steps));

        if (hasSensor) {
            Log.i(TAG, "Starting service");
            Intent intent = new Intent(this, nl.healthchallenge.android.applite.SensorListener.class);
            startService(intent);
        }

        Log.i(TAG, url);
        myWebView.loadUrl(url);
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