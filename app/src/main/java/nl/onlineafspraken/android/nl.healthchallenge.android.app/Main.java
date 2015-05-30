package nl.healthchallenge.android.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.KeyEvent;

import android.webkit.ValueCallback;
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

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main extends GcmActivity {
	WebView myWebView;
	ProgressBar myProgressBar;
	ImageView mySplash;
	RelativeLayout mySplashLayout;
    Context context;
    boolean hasSensor;

    public static final String PROPERTY_PAYLOAD = "payload";
    public static final String PROPERTY_PAYLOAD_ARGS = "payload_args";
    public static final String PROPERTY_BASEURL = "baseurl";

    private static final int REQUEST_CODE = 6666; // onActivityResult request code

    final Activity activity = this;

    private SensorManager mSensorManager;
    private Sensor mStepCounterSensor;

    static final String TAG = "HealthChallenge";

    nl.healthchallenge.android.app.AndroidJS myAndroidJS;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if( requestCode == REQUEST_CODE) {
            // If the file selection was successful
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    // Get the URI of the selected file
                    final Uri uri = data.getData();
                    Log.i(TAG, "Uri = " + uri.toString());

                    long imageId = Long.parseLong(uri.getLastPathSegment());

                    Log.i(TAG, Long.toString(imageId));
                    Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(
                            getContentResolver(),
                            imageId,
                            MediaStore.Images.Thumbnails.MINI_KIND,
                            (BitmapFactory.Options) null );


                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    bitmap.recycle();
                    bitmap = null;

                    byte[] b = baos.toByteArray();
                    String encodedImage= Base64.encodeToString(b, Base64.NO_WRAP);

                    String javascript = "javascript:imageSelected('"+encodedImage+"');";
                    myWebView.loadUrl(javascript);
                }
            }
        }
    }

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

        myAndroidJS = new nl.healthchallenge.android.app.AndroidJS(getApplicationContext(), this);
        myWebView.addJavascriptInterface(myAndroidJS, "Android");

        final SharedPreferences prefs = getApplicationContext().getSharedPreferences(nl.healthchallenge.android.app.Main.class.getSimpleName(), Context.MODE_PRIVATE);
        String url = prefs.getString(PROPERTY_BASEURL, this.getString(R.string.app_url_alt));
        //String url = this.getString(R.string.app_url_alt);

        url = url.concat("?device=android");
        if (checkPlayServices()) {
            url = url.concat("&android_id=").concat(regid);
        }
        if (hasSensor) {
            url = url.concat("&sensor=1");
        }

        int steps = getSharedPreferences(nl.healthchallenge.android.app.Main.class.getSimpleName(), Context.MODE_PRIVATE).getInt("pedometer", 0);
        Log.i(TAG, "Steps from cache: " + String.valueOf(steps));

        if (hasSensor) {
            Log.i(TAG, "Starting service");
            Intent intent = new Intent(this, nl.healthchallenge.android.app.SensorListener.class);
            startService(intent);
        }

        Log.i(TAG, url);
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

    @Override
    protected void onResume() {
        super.onResume();
        myWebView = (WebView) findViewById(R.id.webview);
        try {
            String javascript = "javascript:loadDataset();";
            myWebView.loadUrl(javascript);
        }
        catch (Exception e) {}
    }
}