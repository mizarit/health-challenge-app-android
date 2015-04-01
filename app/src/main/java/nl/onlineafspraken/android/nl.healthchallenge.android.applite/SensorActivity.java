package nl.healthchallenge.android.applite;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.webkit.WebView;

public class SensorActivity extends Activity implements SensorEventListener
{
    // step counter related
    private SensorManager mSensorManager;
    private Sensor mStepCounterSensor;
    private Sensor mStepDetectorSensor;
    static final String TAG = "HealthChallengeSensor";
    WebView myWebView;

    public Sensor initSensors()
    {
        // init step counter
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mStepCounterSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        mStepDetectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        return mStepCounterSensor;
    }

    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        float[] values = event.values;
        int value = -1;

        if (values.length > 0) {
            value = (int) values[0];
        }

        myWebView = (WebView) findViewById(R.id.webview);

        if (sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            //textView.setText("Step Counter Detected : " + value);
            Log.i(TAG, "Step Counter Detected : " + value);
            String javascript = "javascript:totalSteps("+value+");";
            myWebView.loadUrl(javascript);
        } else if (sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            // For test only. Only allowed value is 1.0 i.e. for step taken
            //textView.setText("Step Detector Detected : " + value);
            Log.i(TAG, "Step Detector Detected : " + value);
            String javascript = "javascript:countStep();";
            myWebView.loadUrl(javascript);
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            Log.i(TAG, "Step Counter accuracy changed: " + accuracy);
        } else if (sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            Log.i(TAG, "Step Detector accuracy changed: " + accuracy);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mStepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mStepDetectorSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // do not stop counting when app is closed
        //mSensorManager.unregisterListener(this, mStepCounterSensor);
        //mSensorManager.unregisterListener(this, mStepDetectorSensor);
    }
}