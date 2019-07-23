package com.fanfan.temperaturetest;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.*;

public class TemperatureTestActivity extends Activity {
    private static final String TAG = "TemperatureTestActivity";

    private TemperatureTestService mTempServ = null;
    private ServiceConnection mTempServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder serv) {
            mTempServ = ((TemperatureTestService.TemperatureTestBinder)serv).getService(mHandler);
            mTempServ.onResume();
            mBtnStartStopTest.setText(mTempServ.isTestStarted()
                ? getString(R.string.btn_stop_test) + " (now is recording log to file " + mTempServ.getRecFileName() + ")"
                : getString(R.string.btn_start_test));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mTempServ = null;
        }
    };

    private Button   mBtnStartStopTest;
    private EditText mTxtTemperatureInfo;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mBtnStartStopTest   = (Button  )findViewById(R.id.btn_start_Stop);
        mTxtTemperatureInfo = (EditText)findViewById(R.id.txt_temp_info  );
        mBtnStartStopTest.setOnClickListener(mOnClickListener);
        mTxtTemperatureInfo.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);

        // start record service
        Intent i = new Intent(TemperatureTestActivity.this, TemperatureTestService.class);
        startService(i);

        // bind record service
        bindService(i, mTempServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        // unbind record service
        unbindService(mTempServiceConn);

        // stop record service
//      Intent i = new Intent(TemperatureTestActivity.this, TemperatureTestService.class);
//      stopService(i);

        super.onDestroy();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        if (mTempServ != null) {
            mBtnStartStopTest.setText(mTempServ.isTestStarted() ?
                R.string.btn_stop_test : R.string.btn_start_test);
        }
        if (mTempServ != null) mTempServ.onResume();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        if (mTempServ != null) mTempServ.onPause();
    }

    @Override
    public void onBackPressed() {
        if (true) {
            moveTaskToBack(true);
        } else {
            super.onBackPressed();
        }
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.btn_start_Stop:
                if (mTempServ.isTestStarted()) {
                    mBtnStartStopTest.setText(R.string.btn_start_test);
                    mTempServ.stopTemperatureTest();
                } else {
                    Date date = new Date(System.currentTimeMillis());
                    SimpleDateFormat df = new SimpleDateFormat("'temperature'_yyyyMMdd_HHmmss");
                    String filename = "/mnt/sdcard/" + df.format(date) + ".log";
                    mTxtTemperatureInfo.setText("");
                    mBtnStartStopTest.setText(getString(R.string.btn_stop_test) + " (now is recording log to file " + filename + ")");
                    mTempServ.startTemperatureTest(filename);
                }
                break;
            }
        }
    };

    public static final int MSG_TEMPERATURE_STRING = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_TEMPERATURE_STRING:
                {
                    String str = mTxtTemperatureInfo.getText().toString();
                    if (str.length() > 10 * 1024 * 1024) {
                        str = str.substring(0, 5 * 1024 * 1024);
                    }
                    str += (String)msg.obj;
                    mTxtTemperatureInfo.setText(str);
                }
                break;
            }
        }
    };
}



