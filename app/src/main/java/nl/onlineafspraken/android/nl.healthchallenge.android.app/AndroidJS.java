package nl.healthchallenge.android.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.MediaStore;
import android.widget.Toast;
import android.webkit.JavascriptInterface;
import android.os.Vibrator;
import android.media.RingtoneManager;
import android.media.MediaPlayer;
import android.net.Uri;

import java.util.PriorityQueue;

class AndroidJS {
    Context myContext;
    Activity myActivity;
    public static final String PROPERTY_PAYLOAD = "payload";
    public static final String PROPERTY_PAYLOAD_ARGS = "payload_args";
    public static final String PROPERTY_USE_SOUND = "sound";
    public static final String PROPERTY_USE_VIBRATE = "vibrate";
    public static final String PROPERTY_USE_NOTIFICATIONS = "notifications";
    public static final String PROPERTY_BACK_CALLBACK = "back_callback";

    private static final int REQUEST_CODE = 6666; // onActivityResult request code

    public AndroidJS(Context context, Activity activity) {
        myContext = context;
        myActivity = activity;
    }

    @JavascriptInterface
    public void attachFileInput() {
        Intent pickIntent = new Intent();
        pickIntent.setType("image/*");
        pickIntent.setAction(Intent.ACTION_GET_CONTENT);

        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        String pickTitle = "Foto maken of selecteren"; // Or get from strings.xml
        Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);
        chooserIntent.putExtra
                (
                        Intent.EXTRA_INITIAL_INTENTS,
                        new Intent[] { takePhotoIntent }
                );

        myActivity.startActivityForResult(chooserIntent, REQUEST_CODE);
    }

    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(myContext, toast, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void vibrate(int duration) {
        Vibrator v = (Vibrator)myContext.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(500);
    }

    @JavascriptInterface
    public void beep() {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        MediaPlayer mp = MediaPlayer.create(myContext, notification);
        mp.start();
    }

    @JavascriptInterface
    public String getPayload()
    {
        final SharedPreferences prefs = getMySharedPreferences(myContext);
        String payload = prefs.getString(PROPERTY_PAYLOAD, "");
        if (!payload.isEmpty()) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PROPERTY_PAYLOAD, "");
            editor.commit();

            return payload.toString();
        }

        return "";
    }

    @JavascriptInterface
    public String getPayloadArgs()
    {

        final SharedPreferences prefs = getMySharedPreferences(myContext);
        String payload_args = prefs.getString(PROPERTY_PAYLOAD_ARGS, "");
        if (!payload_args.isEmpty()) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PROPERTY_PAYLOAD_ARGS, "");
            editor.commit();

            return payload_args.toString();
        }

        return "";
    }

    @JavascriptInterface
    public void setSetting(String key, String value)
    {
        final SharedPreferences prefs = getMySharedPreferences(myContext);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.commit();

        if(key.equals("sound")) {
            if (value.equals("1")) {
                beep();
            }
        } else if(key.equals("vibrate")) {
            if (value.equals("1")) {
                vibrate(500);
            }
        } else if(key.equals("notifications")) {
            if (value.equals("1")) {
                if(prefs.getString(PROPERTY_USE_SOUND,"1").equals("1")) {
                    beep();
                }
                if(prefs.getString(PROPERTY_USE_VIBRATE,"1").equals("1")) {
                    vibrate(500);
                }
            }
        }
    }

    @JavascriptInterface
    public String getSetting(String key)
    {
        final SharedPreferences prefs = getMySharedPreferences(myContext);
        String setting = prefs.getString(key, "");
        return setting;
    }

    @JavascriptInterface
    public void setPhysicalBackCallback(String callback)
    {
        final SharedPreferences prefs = getMySharedPreferences(myContext);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_BACK_CALLBACK, callback);
        editor.commit();
    }

    private SharedPreferences getMySharedPreferences(Context context) {
        return myContext.getSharedPreferences(nl.healthchallenge.android.app.Main.class.getSimpleName(), Context.MODE_PRIVATE);
    }
}
