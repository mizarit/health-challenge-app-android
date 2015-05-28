package nl.healthchallenge.android.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
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

    final Activity activity = this;
    public Uri imageUri;

    private static final int FILECHOOSER_RESULTCODE   = 2888;
    private ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI = null;

    private SensorManager mSensorManager;
    private Sensor mStepCounterSensor;

    static final String TAG = "HealthChallenge";

    nl.healthchallenge.android.app.AndroidJS myAndroidJS;

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {

        if(requestCode==FILECHOOSER_RESULTCODE)
        {

            if (null == this.mUploadMessage) {
                return;

            }

            Uri result=null;

            try{
                if (resultCode != RESULT_OK) {

                    result = null;

                } else {

                    // retrieve from the private variable if the intent is null
                    result = intent == null ? mCapturedImageURI : intent.getData();
                }
            }
            catch(Exception e)
            {
                Toast.makeText(getApplicationContext(), "activity :"+e,
                        Toast.LENGTH_LONG).show();
            }

            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;

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

            // openFileChooser for Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType){

                // Update message
                mUploadMessage = uploadMsg;

                try{

                    // Create AndroidExampleFolder at sdcard

                    File imageStorageDir = new File(
                            Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_PICTURES)
                            , "AndroidExampleFolder");

                    if (!imageStorageDir.exists()) {
                        // Create AndroidExampleFolder at sdcard
                        imageStorageDir.mkdirs();
                    }

                    // Create camera captured image file path and name
                    File file = new File(
                            imageStorageDir + File.separator + "IMG_"
                                    + String.valueOf(System.currentTimeMillis())
                                    + ".jpg");

                    mCapturedImageURI = Uri.fromFile(file);

                    // Camera capture image intent
                    final Intent captureIntent = new Intent(
                            android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("image/*");

                    // Create file chooser intent
                    Intent chooserIntent = Intent.createChooser(i, "Image Chooser");

                    // Set camera intent to file chooser
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
                            , new Parcelable[] { captureIntent });

                    // On select image call onActivityResult method of activity
                    startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);

                }
                catch(Exception e){
                    Toast.makeText(getBaseContext(), "Exception:"+e,
                            Toast.LENGTH_LONG).show();
                }

            }

            // openFileChooser for Android < 3.0
            public void openFileChooser(ValueCallback<Uri> uploadMsg){
                openFileChooser(uploadMsg, "");
            }

            //openFileChooser for other Android versions
            public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                        String acceptType,
                                        String capture) {

                openFileChooser(uploadMsg, acceptType);
            }

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

        String url = this.getString(R.string.app_url_alt);
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