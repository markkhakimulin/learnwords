package org.mobilburger.learnwords;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.Button;

import org.mobilburger.database.DBHelper;

import java.util.Timer;


public class LockScreenService extends Service {

    private Context mContext;
    private BroadcastReceiver mLockscreenReceiver = null;
    private String currentDictId,direction,wordsPerLessonDef,wordsInStudyDef,righPercentDef,wrongPercentDef,emptyAnswerIsWrongDef,wrongAnswersToSkip,useTipsDef;
    private LockScreenUtil lockScreenUtil;
    NotificationManager notificationManager;
    private WindowManager mWindowManager;
    private Intent startLockscreenActIntent;
    private SharedPreferences preferences;
    private CountDownTimer pauseDownTimer;
    private Boolean isPause;
    private Boolean isStoping = false;

    private void stateRecever(boolean isStartRecever) {
        if (isStartRecever) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            registerReceiver(mLockscreenReceiver, filter);

        } else {
            if (null != mLockscreenReceiver) {
                unregisterReceiver(mLockscreenReceiver);
            }

        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        isStoping = false;
        notificationManager = (NotificationManager) this.getSystemService(this.NOTIFICATION_SERVICE) ;
        notificationManager.cancelAll();
        //lockScreenUtil = new LockScreenUtil(mContext);
        mLockscreenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (null != context) {
                    if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                        stopService(new Intent(mContext, LockScreenActivityService.class));
                        TelephonyManager tManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                        boolean isPhoneIdle = tManager.getCallState() == TelephonyManager.CALL_STATE_IDLE;
                        if (isPhoneIdle && !isPause) {
                            startLockscreenActivity();
                        }
                    }
                }
            }
        };

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        preferences = getSharedPreferences("preference", Activity.MODE_PRIVATE);

        isPause = false;

        String dict_id = preferences.getString("current_dict_id",null);
        if (dict_id == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        currentDictId = dict_id;
        direction = DBHelper.Direction.WORD_TO_TRANSLATE.toString();
        emptyAnswerIsWrongDef = Boolean.toString(getResources().getBoolean(R.bool.empty_answer_is_wrong));
        useTipsDef = Boolean.toString(getResources().getBoolean(R.bool.use_tips));
        wordsPerLessonDef = Integer.toString(getResources().getInteger(R.integer.words_per_lesson));
        wordsInStudyDef = Integer.toString(getResources().getInteger(R.integer.words_study));
        righPercentDef = Integer.toString(getResources().getInteger(R.integer.right_answer_percent));
        wrongPercentDef = Integer.toString(getResources().getInteger(R.integer.wrong_answer_percent));
        wrongAnswersToSkip =  Integer.toString(getResources().getInteger(R.integer.wrong_answers_to_skip));
        stateRecever(true);
        if (intent != null) {
            startLockscreenActivity();
        }
        startForeground();
        return START_REDELIVER_INTENT;
    }

    private void startForeground() {


        showStopNotification();

        try {
            PendingIntent.getBroadcast(this, 0, new Intent("StartService"), PendingIntent.FLAG_UPDATE_CURRENT).send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    void showStopNotification() {

        notificationManager.cancelAll();

        Intent notificationIntent = new Intent(this,MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        registerReceiver(pauseServiceReceiver, new IntentFilter("PauseService8"));
        registerReceiver(pauseServiceReceiver, new IntentFilter("PauseService1"));
        registerReceiver(stopServiceReceiver, new IntentFilter("PreStopService"));

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),0,notificationIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        registerReceiver(startServiceReceiver, new IntentFilter("StopService"));

        PendingIntent stopServiceIntent = PendingIntent.getBroadcast(this, 0, new Intent("StopService"), PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent pauseServiceIntent8 = PendingIntent.getBroadcast(this, 0, new Intent("PauseService8"), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pauseServiceIntent1 = PendingIntent.getBroadcast(this, 0, new Intent("PauseService1"), PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setTicker(getResources().getString(R.string.app_name))
                .setContentText(getString(R.string.running))
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(R.drawable.stop,getString(R.string.action_stop_service),stopServiceIntent)
                .addAction(R.drawable.pause_1h,getString(R.string.action_pause_service_1),pauseServiceIntent1)
                .addAction(R.drawable.pause_8h,getString(R.string.action_pause_service_8),pauseServiceIntent8)
                .build();
        startForeground(101,notification);
    }
    void showPauseNotification(String title) {

        notificationManager.cancelAll();

        Intent notificationIntent = new Intent(mContext,MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),0,notificationIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(mContext)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setTicker(getResources().getString(R.string.app_name))
                .setContentText(title)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.start,getString(R.string.action_resume_service),PendingIntent.getService(mContext,0,new Intent(mContext,LockScreenService.class),PendingIntent.FLAG_ONE_SHOT))
                .build();
        notificationManager.notify(101,notification);
    }

    protected BroadcastReceiver stopServiceReceiver= new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            isStoping = true;
        }
    };

    protected BroadcastReceiver startServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            notificationManager.cancelAll();
            Intent notificationIntent = new Intent(context,MainActivity.class);
            notificationIntent.setAction(Intent.ACTION_MAIN);
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),0,notificationIntent,PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new NotificationCompat.Builder(context)
                    .setContentTitle(getResources().getString(R.string.app_name))
                    .setTicker(getResources().getString(R.string.app_name))
                    .setContentText(getString(R.string.stopped))
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentIntent(pendingIntent)
                    .addAction(R.drawable.start,getString(R.string.action_start_service),PendingIntent.getService(context,0,new Intent(context,LockScreenService.class),PendingIntent.FLAG_ONE_SHOT))
                    .build();
            notificationManager.notify(102,notification);

            isStoping = true;

            stopSelf();
        }
    };



    protected BroadcastReceiver pauseServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int count = 0;

            if (intent.getAction().equalsIgnoreCase("PauseService1")) {
                count = 3600000;
            }

            if (intent.getAction().equalsIgnoreCase("PauseService8")) {
                count = 28800000;
            }

            if (count > 0) {
                pauseDownTimer = new CountDownTimer(count, 60000) {

                    public void onTick(long millisUntilFinished) {
                        isPause = true;
                    }

                    public void onFinish() {
                        isPause = false;
                        pauseDownTimer = null;
                        showStopNotification();
                    }

                }.start();

                String title = count == 3600000?getString(R.string.paused1):getString(R.string.paused8);
                showPauseNotification(title);
            }
        }
    };


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stateRecever(false);
        notificationManager.cancel(101);
        mLockscreenReceiver = null;
        unregisterReceiver(startServiceReceiver);
        startServiceReceiver = null;
        unregisterReceiver(pauseServiceReceiver);
        pauseServiceReceiver = null;
        unregisterReceiver(stopServiceReceiver);
        stopServiceReceiver = null;
        stopService(startLockscreenActIntent);
        startLockscreenActIntent = null;
        pauseDownTimer = null;


        if (!isStoping) {
            Intent broadcastIntent = new Intent("org.mobilburger.learnwords.LockScreenServiceRestart");
            sendBroadcast(broadcastIntent);
        }

    }

    private void startLockscreenActivity() {



        //if (lockScreenUtil.isServiceRunning(LockScreenActivityService.class)) return;

        direction = (direction.equals(DBHelper.Direction.WORD_TO_TRANSLATE.toString())?
                DBHelper.Direction.TRANSLATE_TO_WORD.toString():
                DBHelper.Direction.WORD_TO_TRANSLATE.toString());

        //Intent startLockscreenActIntent = new Intent(this, LockScreenActivity.class);
        startLockscreenActIntent = new Intent(this, LockScreenActivityService.class);
        startLockscreenActIntent.putExtra(DBHelper.CN_ID_DICT,currentDictId);
        startLockscreenActIntent.putExtra(DBHelper.CN_EMPTY_ANSWER_IS_WRONG,emptyAnswerIsWrongDef);
        startLockscreenActIntent.putExtra(DBHelper.CN_USE_TIPS,useTipsDef);
        startLockscreenActIntent.putExtra(DBHelper.CN_WORDS_PER_LESSON,wordsPerLessonDef);
        startLockscreenActIntent.putExtra(DBHelper.CN_WORDS_STUDY,wordsInStudyDef);
        startLockscreenActIntent.putExtra(DBHelper.CN_RIGHT_ANSWER_PERCENT,righPercentDef);
        startLockscreenActIntent.putExtra(DBHelper.CN_WRONG_ANSWER_PERCENT,wrongPercentDef);
        startLockscreenActIntent.putExtra(DBHelper.CN_WRONG_ANSWERS_TO_SKIP,wrongAnswersToSkip);
        startLockscreenActIntent.putExtra(DBHelper.CN_DIRECTION,direction);
        //startLockscreenActIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startService(startLockscreenActIntent);


        //startActivity(startLockscreenActIntent);
    }

}
