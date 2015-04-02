package nl.healthchallenge.android.applite;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import android.webkit.JavascriptInterface;
import android.os.Vibrator;
import android.media.RingtoneManager;
import android.media.MediaPlayer;
import android.net.Uri;

import java.util.PriorityQueue;

class AndroidJS {
    Context myContext;
    public static final String PROPERTY_PAYLOAD = "payload";
    public static final String PROPERTY_PAYLOAD_ARGS = "payload_args";
    public static final String PROPERTY_USE_SOUND = "sound";
    public static final String PROPERTY_USE_VIBRATE = "vibrate";
    public static final String PROPERTY_USE_NOTIFICATIONS = "notifications";

    public AndroidJS(Context context) {
        myContext = context;
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

        switch(key){
            case "sound":
                if (value.equals("1")) {
                    beep();
                }
                break;

            case "vibrate":
                if (value.equals("1")) {
                    vibrate(500);
                }
                break;

            case "notifications":
                if (value.equals("1")) {
                    if(prefs.getString(PROPERTY_USE_SOUND,"1").equals("1")) {
                        beep();
                    }
                    if(prefs.getString(PROPERTY_USE_VIBRATE,"1").equals("1")) {
                        vibrate(500);
                    }
                }
                break;
        }
    }

    @JavascriptInterface
    public String getSetting(String key)
    {
        final SharedPreferences prefs = getMySharedPreferences(myContext);
        String setting = prefs.getString(key, "");
        return setting;
    }

    private SharedPreferences getMySharedPreferences(Context context) {
        return myContext.getSharedPreferences(nl.healthchallenge.android.applite.Main.class.getSimpleName(), Context.MODE_PRIVATE);
    }
}
