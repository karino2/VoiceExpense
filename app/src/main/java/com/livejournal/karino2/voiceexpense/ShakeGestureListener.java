package com.livejournal.karino2.voiceexpense;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import java.util.Date;

/**
 * Created by karino on 7/11/15.
 */
public class ShakeGestureListener implements SensorEventListener {
    public interface OnShakeListener {
        void onShake();
    }

    OnShakeListener listener;
    public ShakeGestureListener(OnShakeListener onShakeListener) {
        listener = onShakeListener;
    }


    long lastTick = -1;
    long lastFire = -1;
    float lastXMax;
    float lastXMin;
    float lastYMax;
    float lastYMin;
    float lastZMax;
    float lastZMin;
    long getNowTick() { return ((new Date())).getTime();}

    final long TICK_THRESHOLD_MSEC = 250;
    final float DISTANCE_THRESHOLD = 15;
    @Override
    public void onSensorChanged(SensorEvent event) {
        long nowMil = getNowTick();
        if(lastFire != -1 && (nowMil - lastFire < TICK_THRESHOLD_MSEC*4)) {
            // writeConsole("already fire");
            return;
        }

        if(lastTick == -1 || nowMil-lastTick > TICK_THRESHOLD_MSEC ) {
            lastXMin = lastXMax = event.values[0];
            lastYMin = lastYMax = event.values[1];
            lastZMin = lastZMax = event.values[2];

            lastTick = nowMil;
            // writeConsole("new sense, "+lastXMin + "," + lastXMax);
            return;
        }

        lastXMin = Math.min(lastXMin, event.values[0]);
        lastXMax = Math.max(lastXMax, event.values[0]);
        lastYMin = Math.min(lastYMin, event.values[1]);
        lastYMax = Math.max(lastYMax, event.values[1]);
        lastZMin = Math.min(lastZMin, event.values[2]);
        lastZMax = Math.max(lastZMax, event.values[2]);

        lastTick = nowMil;
        // log("sense, "+lastXMin + "," + lastXMax);

        if(lastXMax - lastXMin > DISTANCE_THRESHOLD ||
                lastYMax - lastYMin > DISTANCE_THRESHOLD ||
                lastZMax - lastZMin > DISTANCE_THRESHOLD) {
            lastFire = nowMil;
            listener.onShake();
        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
