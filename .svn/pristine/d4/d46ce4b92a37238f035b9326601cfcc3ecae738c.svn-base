package org.mobilburger.learnwords;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.mobilburger.database.DBHelper;



public class LockScreenActivity extends Activity {

    private RelativeLayout mLockscreenMainLayout = null;
    private LockScreenUtil mLockScreenUtil = null;
    Messenger mService = null;
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    private static final int PERMISSION_REQUEST = 1;

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LockScreenActivityService.MSG_STOP_SERVICE:
                    finish();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @TargetApi(23)
    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        //mMainHandler = new SendMassgeHandler();
        //mLockScreenUtil = new LockScreenUtil(this);

        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int ui = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(ui);

        getWindow().setType(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {

            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST);
        } else {

            initLockScreenUi();
            setLockGuard();
        }

    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                initLockScreenUi();
                setLockGuard();
            } else {
                Toast.makeText(this, getString(R.string.error_no_permission_allowed), Toast.LENGTH_SHORT).show();
            }
        }
    }



    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        initLockScreenUi();
    }


    private void initLockScreenUi() {

/*        setContentView(R.layout.activity_lockscreen);
        mLockscreenMainLayout = (RelativeLayout) findViewById(R.id.lockscreen_main_layout);
        mLockscreenMainLayout.getBackground().setAlpha(100);*/
    }

/*    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }*/

    private void setLockGuard() {
/*
        boolean isLockEnable = false;
        if (!mLockScreenUtil.isStandardKeyguardState()) {
            isLockEnable = false;
        } else {
            isLockEnable = true;
        }
*/

        Intent startLockscreenIntent = new Intent(this, LockScreenActivityService.class);
        startLockscreenIntent.putExtra(DBHelper.CN_ID_DICT,getIntent().getStringExtra(DBHelper.CN_ID_DICT));
        startLockscreenIntent.putExtra(DBHelper.CN_DIRECTION,getIntent().getStringExtra(DBHelper.CN_DIRECTION));
        startLockscreenIntent.putExtra(DBHelper.CN_EMPTY_ANSWER_IS_WRONG,getIntent().getStringExtra(DBHelper.CN_EMPTY_ANSWER_IS_WRONG));
        startLockscreenIntent.putExtra(DBHelper.CN_WORDS_PER_LESSON,getIntent().getStringExtra(DBHelper.CN_WORDS_PER_LESSON));
        startLockscreenIntent.putExtra(DBHelper.CN_WORDS_STUDY,getIntent().getStringExtra(DBHelper.CN_WORDS_STUDY));
        startLockscreenIntent.putExtra(DBHelper.CN_RIGHT_ANSWER_PERCENT,getIntent().getStringExtra(DBHelper.CN_RIGHT_ANSWER_PERCENT));
        startLockscreenIntent.putExtra(DBHelper.CN_WRONG_ANSWER_PERCENT,getIntent().getStringExtra(DBHelper.CN_WRONG_ANSWER_PERCENT));
        startLockscreenIntent.putExtra(DBHelper.CN_WRONG_ANSWERS_TO_SKIP,getIntent().getStringExtra(DBHelper.CN_WRONG_ANSWERS_TO_SKIP));

        startService(startLockscreenIntent);
        finish();
        //bindService(startLockscreenIntent,mConnection,BIND_AUTO_CREATE);
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            try {
                Message msg = Message.obtain(null, LockScreenActivityService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            finish();
        }
    };


    @Override
    protected void onResume() {
        super.onResume();


    }


    @Override
    protected void onDestroy() {
        //unbindService(mConnection);
        mService = null;
        mConnection = null;
        super.onDestroy();
    }


    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

}
