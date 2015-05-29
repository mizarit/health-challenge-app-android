package nl.healthchallenge.android.app;

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
    public void setSetting(String key, String value)
    {
        final SharedPreferences prefs = getMySharedPreferences(myContext);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);

        editor.commit();
    }

    @JavascriptInterface
    public String getSetting(String key)
    {
        final SharedPreferences prefs = getMySharedPreferences(myContext);
        String setting = prefs.getString(key, "");
        return setting;
    }

    private SharedPreferences getMySharedPreferences(Context context) {
        return myContext.getSharedPreferences(nl.healthchallenge.android.app.Main.class.getSimpleName(), Context.MODE_PRIVATE);
    }
}
