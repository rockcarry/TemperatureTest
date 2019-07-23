package com.fanfan.temperaturetest;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.util.Log;

public class FloatWindow {
    private static final String TAG = "FloatWindow";

    private Context       mContext       = null;
    private WindowManager mWinMan        = null;
    private LinearLayout  mFloatLayout   = null;
    private LayoutParams  mLayoutParams  = null;
    private Button        mFloatButton   = null;
    private boolean       mDisplayed     = false;
    private int           mLastFloatPosX = -1;
    private int           mLastFloatPosY = -1;
    private boolean       mMoveFloatFlag = false;
    private boolean       mIsScreenOn    = true;
    private Handler       mHandler       = new Handler();

    public FloatWindow(Context context) {
        mContext     = context;
        mWinMan      = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        mFloatLayout = (LinearLayout )LayoutInflater.from(mContext).inflate(R.layout.float_win, null);
        mFloatButton = (Button       )mFloatLayout.findViewById(R.id.float_btn);

        mLayoutParams  = new LayoutParams();
        mLayoutParams.type    = LayoutParams.TYPE_PHONE;
        mLayoutParams.format  = PixelFormat.RGBA_8888;
        mLayoutParams.flags   = LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        mLayoutParams.x       = 0;
        mLayoutParams.y       = 0;
        mLayoutParams.width   = WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParams.height  = WindowManager.LayoutParams.WRAP_CONTENT;

        mFloatButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean ret = false;
                switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mLastFloatPosX = (int) event.getRawX();
                    mLastFloatPosY = (int) event.getRawY();
                    mMoveFloatFlag = false;
                    break;
                case MotionEvent.ACTION_UP:
                    ret = mMoveFloatFlag;
                    break;
                case MotionEvent.ACTION_MOVE:
                    int x = (int) event.getRawX();
                    int y = (int) event.getRawY();
                    if (!(  x >= mLastFloatPosX && x < mLastFloatPosX + mFloatLayout.getMeasuredWidth()
                         && y >= mLastFloatPosY && y < mLastFloatPosY + mFloatLayout.getMeasuredHeight() ) )
                    {
                        mMoveFloatFlag = true;
                    }
                    if (mMoveFloatFlag) {
                        mLayoutParams.x = x - mFloatLayout.getMeasuredWidth () / 2;
                        mLayoutParams.y = y - mFloatLayout.getMeasuredHeight() / 2 - 25;
                        mWinMan.updateViewLayout(mFloatLayout, mLayoutParams);
                    }
                    break;
                }
                return ret;
            }
        });

        mFloatButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideFloat();
                Intent i = new Intent(mContext, TemperatureTestActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (false) {
                    mContext.startActivity(i);
                } else {
                    PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, i, 0);
                    try { pendingIntent.send(); } catch (Exception e) { e.printStackTrace(); }
                }
            }
        });
    }

    public void create() {
        // register system event receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON );
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mSystemEventReceiver, filter);
    }

    public void destroy() {
        // unregister system event receiver
        mContext.unregisterReceiver(mSystemEventReceiver);
    }

    public void showFloat() {
        mHandler.removeCallbacks(mShowFloatRunnable);
        mHandler.postDelayed(mShowFloatRunnable, 200);
    }

    public void hideFloat() {
        mHandler.removeCallbacks(mShowFloatRunnable);
        if (mDisplayed) {
            mWinMan.removeView(mFloatLayout);
            mDisplayed = false;
        }
    }

    private Runnable mShowFloatRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mIsScreenOn) {
                return;
            }

            if (mDisplayed) {
                mWinMan.updateViewLayout(mFloatLayout, mLayoutParams);
            } else {
                mWinMan.addView(mFloatLayout, mLayoutParams);
            }
            mDisplayed = true;
        }
    };

    private BroadcastReceiver mSystemEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                mIsScreenOn = true;
            }
            else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                mIsScreenOn = false;
            }
        }
    };
}

