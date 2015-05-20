package nl.healthchallenge.android.applite;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.SSLSocketFactory;

public class SensorListener extends Service implements SensorEventListener {
    static final String TAG = "SensorListener";
    public static final String PROPERTY_REG_ID = "registration_id";

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public void onAccuracyChanged(final Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (event.values[0] > Integer.MAX_VALUE) {
           // if (BuildConfig.DEBUG) Logger.log("probably not a real value: " + event.values[0]);
            return;
        } else {
            Sensor sensor = event.sensor;
            float[] values = event.values;
            int value = -1;

            if (values.length > 0) {
                value = (int) values[0];
            }
            if (sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                getSharedPreferences(nl.healthchallenge.android.applite.Main.class.getSimpleName(), Context.MODE_PRIVATE).edit().putInt("pedometer", value).commit();
                //Log.i(TAG, "Steps from service "+ String.valueOf(value));
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Thread thread = new Thread(new Runnable(){
                @Override
                public void run() {
                    try {


                        SharedPreferences prefs = getSharedPreferences(nl.healthchallenge.android.applite.Main.class.getSimpleName(), Context.MODE_MULTI_PROCESS);
                        Log.i(TAG, "Steps by timed service: "+ String.valueOf(prefs.getInt("pedometer", 0)));
                        String url = getApplicationContext().getString(R.string.app_url_alt);
                        url = url.concat("/main/externalPush?device=android");
                        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
                        url = url.concat("&device_id=").concat(registrationId);
                        url = url.concat("&timestamp=").concat(String.valueOf(System.currentTimeMillis() / 1000));
                        url = url.concat("&steps=").concat(String.valueOf(prefs.getInt("pedometer", 0)));
                        Log.i(TAG, url);
                        HttpClient client = new DefaultHttpClient();
                        HttpGet request = new HttpGet(url);
                        try {
                            HttpResponse response = client.execute(request);
                            // Check if server response is valid
                            StatusLine status = response.getStatusLine();
                            if (status.getStatusCode() != 200) {
                                throw new IOException("Invalid response from server: " + status.toString());
                            }

                            // Pull content stream from response
                            HttpEntity entity = response.getEntity();
                            InputStream inputStream = entity.getContent();

                            ByteArrayOutputStream content = new ByteArrayOutputStream();

                            // Read response into a buffered stream
                            int readBytes = 0;
                            byte[] sBuffer = new byte[512];
                            while ((readBytes = inputStream.read(sBuffer)) != -1) {
                                content.write(sBuffer, 0, readBytes);
                            }

                            String dataAsString = new String(content.toByteArray());

                            Log.i(TAG, dataAsString);

                        } catch (IOException e) {
                            Log.d("error", e.getLocalizedMessage());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            thread.start();
        }
        // restart service every hour to get the current step count
        ((AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE))
                .set(AlarmManager.RTC, System.currentTimeMillis() + 60000,//AlarmManager.INTERVAL_HOUR,
                        PendingIntent.getService(getApplicationContext(), 2,
                                new Intent(this, SensorListener.class),
                                PendingIntent.FLAG_UPDATE_CURRENT));

        return Service.START_STICKY;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        reRegisterSensor();
    }

    @Override
    public void onTaskRemoved(final Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // Restart service in 500 ms
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                .set(AlarmManager.RTC, System.currentTimeMillis() + 500, PendingIntent
                        .getService(this, 3, new Intent(this, SensorListener.class), 0));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
            sm.unregisterListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reRegisterSensor() {
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        try {
            sm.unregisterListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (BuildConfig.DEBUG) {
            Log.i(TAG, "step sensors: " + sm.getSensorList(Sensor.TYPE_STEP_COUNTER).size());
            if (sm.getSensorList(Sensor.TYPE_STEP_COUNTER).size() < 1) return; // emulator
            Log.i(TAG, "default: " + sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER).getName());
        }

        // enable batching with delay of max 5 min
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
                SensorManager.SENSOR_DELAY_NORMAL, 5 * 60000);
    }
}
