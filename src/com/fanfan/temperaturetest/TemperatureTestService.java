package com.fanfan.temperaturetest;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import java.io.*;

public class TemperatureTestService extends Service
{
    private static final String TAG = "TemperatureTestService";
    private TemperatureTestBinder mBinder   = null;
    private PowerManager.WakeLock mWakeLock = null;
    private Handler               mHandler  = null;
    private FloatWindow           mFloatWin = null;
    private String                mFileName = null;
    private boolean  mStarted;
    private int      mCurTime;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        // binder
        mBinder = new TemperatureTestBinder();

        // wake lock
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.setReferenceCounted(false);

        // float window
        mFloatWin = new FloatWindow(this);
        mFloatWin.create();
//      mFloatWin.showFloat();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopTemperatureTest();

        // hide float window
        mFloatWin.hideFloat();
        mFloatWin.destroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
//      return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    public class TemperatureTestBinder extends Binder {
        public TemperatureTestService getService(Handler h) {
            mHandler = h;
            return TemperatureTestService.this;
        }
    }

    public boolean isTestStarted() {
        return mStarted;
    }

    public void startTemperatureTest(String filename) {
        if (mStarted) return;
        try {
            mWakeLock.acquire();

            File file = new File(filename);
            file.delete();
            file.createNewFile();
            
            final String LOG_HEADER = " time  percent   voltage   current  temp_bat  temp_cpu  temp_gpu \n"
                                    + "-----------------------------------------------------------------\n";
            appendTextToFile(filename, LOG_HEADER);
            mStarted = true;
            mCurTime = 0;
            mFileName= filename;
            mTimerHandler.post(mTimerRunnable);

            if (mHandler != null) {
                Message msg = new Message();
                msg.what = TemperatureTestActivity.MSG_TEMPERATURE_STRING;
                msg.obj  = new String(LOG_HEADER);
                mHandler.sendMessage(msg);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void stopTemperatureTest() {
        mTimerHandler.removeCallbacks(mTimerRunnable);
        mStarted = false;

        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    public void onResume() {
        if (mFloatWin != null) mFloatWin.hideFloat();
    }

    public void onPause() {
        if (mFloatWin != null && mStarted) mFloatWin.showFloat();
    }

    public String getRecFileName() { return mFileName; }

    private static final int TIMER_DELAY = 10 * 1000;
    private Handler  mTimerHandler  = new Handler();
    private Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            mTimerHandler.postDelayed(this, TIMER_DELAY);
            float vol = 0, cur = 0, tempb = 0, tempc = 0, tempg = 0;
            int   cap = 0;
            BufferedReader br = null;

            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream("/sys/class/power_supply/battery/capacity")));
                cap  = Integer.parseInt(br.readLine()) ;
                br.close();

                br = new BufferedReader(new InputStreamReader(new FileInputStream("/sys/class/power_supply/battery/voltage_now")));
                vol  = Float.parseFloat(br.readLine()) / 1000000;
                br.close();

                br = new BufferedReader(new InputStreamReader(new FileInputStream("/sys/class/power_supply/battery/current_now")));
                cur  = Float.parseFloat(br.readLine()) / 1000;
                br.close();

                br = new BufferedReader(new InputStreamReader(new FileInputStream("/sys/class/power_supply/battery/temp")));
                tempb = Float.parseFloat(br.readLine()) / 10;
                br.close();

                br = new BufferedReader(new InputStreamReader(new FileInputStream("/sys/class/hwmon/hwmon1/device/temp1_input")));
                tempc = Float.parseFloat(br.readLine());
                br.close();

                br = new BufferedReader(new InputStreamReader(new FileInputStream("/sys/class/hwmon/hwmon1/device/temp2_input")));
                tempg = Float.parseFloat(br.readLine());
                br.close();
            } catch (Exception e) { e.printStackTrace(); }

            mCurTime+= 1;
            String str = String.format("%5d %8d %9.3f %9.2f %9.2f %9.2f %9.2f\n", mCurTime, cap, vol, cur, tempb, tempc, tempg);
            appendTextToFile(mFileName, str);

            if (mHandler != null) {
                Message msg = new Message();
                msg.what = TemperatureTestActivity.MSG_TEMPERATURE_STRING;
                msg.obj  = new String(str);
                mHandler.sendMessage(msg);
            }
        }
    };

    private static void appendTextToFile(String file, String text) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file, true);
            writer.write(text);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
}


