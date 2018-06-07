package org.mobilburger.learnwords;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import org.mobilburger.database.DBHelper;
import org.mobilburger.utils.DrawableClickListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import rx.Observer;


public class LockScreenActivityService extends Service {
    private final int LOCK_OPEN_OFFSET_VALUE = 50;
    private Context mContext = null;
    private LayoutInflater mInflater = null;
    private View mLockscreenView = null;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mParams;
    private TableLayout mTableLayout = null;
    private RelativeLayout mStatusForgruondDummyView = null;
    private TextView mWordTextView = null;
    private TextView mTranslateTextView = null,mCompleteText = null;
    private boolean mIsLockEnable = false;
    private boolean mIsSoftkeyEnable = false;
    private int mDeviceWidth = 0;
    //private LockScreenUtil mLockScreenUtil = null;
    private BroadcastReceiver mIncomingCallReceiver = null,mConfigurationChangeReceiver = null;
    private String currentDictId,currentWordId,direction,mCurrentTranslateRandom;
    ///////////////default values
    private Boolean emptyAnswerIsWrongDef,emptyAnswerIsWrong,useAdapitiveTipsDef,useAdapitiveTips,tipsUsed;
    private int wordsPerLesson,wordsInStudy,rightPercent,wrongPercent,wrongAnswersToSkip,adapitiveTipsLimit;
    private int wordsPerLessonCurrent,wordsSkip,currentCompletely;
    private Cursor mCursor;
    private String mCurrentWord,mCurrentTranslate,wordsPerLessonDef,wordsInStudyDef,rightPercentDef,wrongPercentDef,wrongAnswersToSkipDef;
    private SQLiteOpenHelper  mSqLiteOpenHelper;
    List<Button> mClickedButtons;
    HashMap<String,ArrayList<Button>> mButtons;
    private Button skipButton,tipButton;
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_STOP_SERVICE = 3;
    Messenger mActivityMessenger =null;
    Messenger mActivityBinder = null;
    private float mWidth;
    private Vibrator mVibrator;

    private Animation shakeanimation,alphaIncreaseAnimation,alphaDecreaseAnimation;

    private static final int PERMISSION_REQUEST = 1;

    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mActivityBinder = msg.replyTo;
                    break;
                case MSG_UNREGISTER_CLIENT:
                    //mActivityMessenger.remove(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    public static float pxFromDp(final Context context, final float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {


    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSqLiteOpenHelper = DBHelper.getOpenHelper(getApplicationContext());
        mContext = this;
        //mLockScreenUtil = new LockScreenUtil(mContext);
        mIncomingCallReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
                    tm.listen(new PhoneStateListener() {
                        @Override
                        public void onCallStateChanged(int state, String incomingNumber) {

                            if (state == 1) {
                                dettachLockScreenView();
                            }
                        }
                    },PhoneStateListener.LISTEN_CALL_STATE);
                } catch (Exception e) {

                }
            }
        };
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mActivityMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mActivityBinder = null;
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //1 or 0
        currentDictId = intent.getStringExtra(DBHelper.CN_ID_DICT);
        emptyAnswerIsWrongDef = Boolean.valueOf(intent.getStringExtra(DBHelper.CN_EMPTY_ANSWER_IS_WRONG));
        useAdapitiveTipsDef = Boolean.valueOf(intent.getStringExtra(DBHelper.CN_USE_TIPS));
        wordsPerLessonDef = intent.getStringExtra(DBHelper.CN_WORDS_PER_LESSON);
        wordsInStudyDef = intent.getStringExtra(DBHelper.CN_WORDS_STUDY);
        rightPercentDef = intent.getStringExtra(DBHelper.CN_RIGHT_ANSWER_PERCENT);
        wrongPercentDef = intent.getStringExtra(DBHelper.CN_WRONG_ANSWER_PERCENT);
        wrongAnswersToSkipDef = intent.getStringExtra(DBHelper.CN_WRONG_ANSWERS_TO_SKIP);
        adapitiveTipsLimit = 50;
        direction = intent.getStringExtra(DBHelper.CN_DIRECTION);

        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED );
        registerReceiver(mIncomingCallReceiver,filter);

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED );
        registerReceiver(mConfigurationChangeReceiver,filter);

        mActivityMessenger = new Messenger(new IncomingHandler());

        if (null != mWindowManager ) {
            if (null != mLockscreenView && isAttachedToWindow()) {
                mWindowManager.removeView(mLockscreenView);
            }
            mWindowManager = null;
            mParams = null;
            mInflater = null;
            mLockscreenView = null;
        }


        initState();
        initView();
        attachLockScreenView();


        MobileAds.initialize(mContext, "ca-app-pub-3650741793114344~7420664021");

        AdView mAdView = mLockscreenView.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);


        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {

        if (null != mWindowManager ) {
            if (null != mLockscreenView && isAttachedToWindow()) {
                mWindowManager.removeView(mLockscreenView);
            }
            mWindowManager = null;
            mParams = null;
            mInflater = null;
            mLockscreenView = null;
        }


        if (null != mIncomingCallReceiver) {
            unregisterReceiver(mIncomingCallReceiver);
        }
        if (null != mConfigurationChangeReceiver) {
            unregisterReceiver(mConfigurationChangeReceiver);
        }
        mIncomingCallReceiver = null;
        mActivityMessenger = null;
        mConfigurationChangeReceiver = null;

    }


    private void initState() {

        mIsLockEnable = LockScreenUtil.getInstance(mContext).isStandardKeyguardState();
        if (mIsLockEnable) {
            mParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                    );
        } else {
            mParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mIsLockEnable && mIsSoftkeyEnable) {
                mParams.flags = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            } else {
                mParams.flags = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
            }
        } else {
            mParams.flags = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        }

        if (null == mWindowManager) {
            mWindowManager = ((WindowManager) mContext.getSystemService(WINDOW_SERVICE));
        }

    }

    private void initView() {
        if (null == mInflater) {
            mInflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        if (null == mLockscreenView) {
            mLockscreenView = mInflater.inflate(R.layout.view_lockscreen, null);
            mLockscreenView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        }
    }


    private void attachLockScreenView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(mContext)) {
                Intent permissionActivityIntent = new Intent(mContext, PermissionActivity.class);
                permissionActivityIntent.setAction(PermissionActivity.PERMISSION_OVERLAY);
                permissionActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(permissionActivityIntent);

                LockScreenUtil.getInstance(mContext).createAsyncSubject()
                    .subscribe(new Observer<Boolean>() {

                        @Override
                        public void onNext(Boolean value) {
                            addLockScreenView();

                        }

                        @Override
                        public void onError(Throwable e) {

                        }
                        @Override
                        public void onCompleted() {
                        }

                    });
            }
            else {
                addLockScreenView();
            }
        }
        else {
        addLockScreenView();
        }

    }
    View.OnAttachStateChangeListener onAttachStateChangeListener = new View.OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(View v) {
            settingLockView();
            mLockscreenView.addOnLayoutChangeListener(onLayoutChangeListener);
        }

        @Override
        public void onViewDetachedFromWindow(View v) {

        }
    };

    View.OnLayoutChangeListener onLayoutChangeListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

            if (mTableLayout.getChildCount() > 0 && bottom != oldBottom) {
                removeAllButtons();
                mButtons = new HashMap<>();
                redrawButtons(mCurrentTranslateRandom);
            }
        }
    };

    private void addLockScreenView() {

        if (null != mWindowManager && null != mLockscreenView && null != mParams) {
            mLockscreenView.addOnAttachStateChangeListener(onAttachStateChangeListener);
            mWindowManager.addView(mLockscreenView, mParams);

        }
    }

    private boolean dettachLockScreenView() {
        if (null != mWindowManager && null != mLockscreenView && isAttachedToWindow()) {
            mLockscreenView.removeOnLayoutChangeListener(onLayoutChangeListener);
            mLockscreenView.removeOnAttachStateChangeListener(onAttachStateChangeListener);

            mWindowManager.removeView(mLockscreenView);
            mWindowManager = null;
            mLockscreenView = null;
           /* if (mActivityBinder != null) {

                try {
                    Message msg = Message.obtain(null, MSG_STOP_SERVICE);
                    mActivityBinder.send(msg);
                } catch (RemoteException e) {
                }
            }*/
            stopSelf();
            return true;
        } else {
            return false;
        }
    }

    private void initWord(Boolean newWorld) {


        mLockscreenView.requestLayout();

        if (mCursor.getCount() == 0) {
            dettachLockScreenView();
            return;
        }

        if (newWorld) {
            if (!mCursor.moveToNext()) {
                mCursor.moveToFirst();
            }
        }
        currentWordId = mCursor.getString(mCursor.getColumnIndex(DBHelper.CN_ID));
        currentCompletely = mCursor.getInt(mCursor.getColumnIndex(DBHelper.CN_COMPLETELY));

        if (direction.equals(DBHelper.Direction.WORD_TO_TRANSLATE.toString())) {
            mCurrentWord = mCursor.getString(mCursor.getColumnIndex(DBHelper.CN_WORD));
            mCurrentTranslate = mCursor.getString(mCursor.getColumnIndex(DBHelper.CN_TRANSLATE));
        } else {
            mCurrentWord = mCursor.getString(mCursor.getColumnIndex(DBHelper.CN_TRANSLATE));
            mCurrentTranslate = mCursor.getString(mCursor.getColumnIndex(DBHelper.CN_WORD));
        }
        mWordTextView.setText(mCurrentWord);
        mTranslateTextView.setText("");

        mTranslateTextView.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        removeAllButtons();

        mTranslateTextView.setOnTouchListener(new DrawableClickListener.RightDrawableClickListener(mTranslateTextView) {
            @Override
            public boolean onDrawableClick() {
                clearLastButton();

                return true;
            }
        });



        tipsUsed = false;


        //if (useAdapitiveTips && currentCompletely < adapitiveTipsLimit ) {
        //  int blindQuantity = 0;
        //   int letterQuantity = mCurrentTranslate.length() * adapitiveTipsLimit / 100;//сколько букв из слова учавствуют в подсказках всего
        //   blindQuantity = (int) (letterQuantity - Math.min(Math.ceil((double) letterQuantity * (double)currentCompletely / (double)adapitiveTipsLimit), letterQuantity));

        //   mTranslateTextView.setText(TextUtils.substring(mCurrentTranslate,0,blindQuantity));
        //String mCurrentTranslateRandom = randomize(TextUtils.substring(mCurrentTranslate, blindQuantity, mCurrentTranslate.length()));
        // }
        mCurrentTranslate = removeInsignificantCharacters(mCurrentTranslate);
        mCurrentTranslateRandom = randomize(mCurrentTranslate);

        mClickedButtons = new ArrayList<>();
        mButtons = new HashMap<>();

        redrawButtons(mCurrentTranslateRandom);

        setComplete(wordsPerLessonCurrent+"/"+wordsPerLesson);

    }

    String removeInsignificantCharacters(String translate) {
        int startBracketIndex = -1;
        int finishBracketIndex = -1;
        for (int i = 0 ; i<translate.length();i++) {
            char letter = translate.charAt(i);
            if (String.valueOf(letter).equalsIgnoreCase("(")) {
                startBracketIndex = i;
            }
            if (String.valueOf(letter).equalsIgnoreCase(")")) {
                finishBracketIndex = i;
            }
        }

        if (startBracketIndex >= 0 && finishBracketIndex >= 0) {
            String subString = translate.substring(0,startBracketIndex);
            String subString1 = translate.substring(finishBracketIndex,translate.length());
            subString.concat(subString1);
            return subString;
        }
        return translate;
    }

    void redrawButtons(String mCurrentTranslateRandom) {

        int buttonWidth = getResources().getDimensionPixelSize(R.dimen.button_width);
        int buttonHeight = getResources().getDimensionPixelSize(R.dimen.button_width);

        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(buttonWidth,buttonHeight);

        mWidth = mTableLayout.getMeasuredWidth();
        int numberInRow = (int)(Math.floor(mWidth/buttonWidth));

        TableRow row = new TableRow(getApplicationContext());
        row.setGravity(Gravity.CENTER);
        mTableLayout.addView(row);

        int i;
        for (i = 0 ; i<mCurrentTranslateRandom.length();i++) {

            char letter = mCurrentTranslateRandom.charAt(i);
            Button letterButton = (Button) mInflater.inflate(R.layout.letter_button, null);
            letterButton.setTag(i);
            int clickedButtonIndex = getClickedButtonIndex(i);
            if (clickedButtonIndex >= 0) {
                setButtonToClickedList(clickedButtonIndex,letterButton);
            }
            letterButton.setLayoutParams(layoutParams);
            letterButton.setText(String.valueOf(letter));

            letterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    CharSequence text = mTranslateTextView.getText();
                    CharSequence letter = ((TextView) v).getText();
                    mTranslateTextView.setText(TextUtils.concat(text, letter));
                    addButtonToClickedList((Button) v);
                    mVibrator.vibrate(100);
                    checkResult(false);

                }
            });


            if (!mButtons.containsKey(String.valueOf(letter).toLowerCase())) {
                mButtons.put(String.valueOf(letter).toLowerCase(),new ArrayList<Button>());
            }
            mButtons.get(String.valueOf(letter).toLowerCase()).add(letterButton);

            if (i%numberInRow == 0) {
                row = new TableRow(getApplicationContext());
                row.setGravity(Gravity.CENTER);
                mTableLayout.addView(row);
            }
            row.addView(letterButton);

        }
        if (useAdapitiveTips) {
            if (i%numberInRow == 0) {
                row = new TableRow(getApplicationContext());
                row.setGravity(Gravity.CENTER);
                mTableLayout.addView(row);
            }
            //char letter = '<?>';
            tipButton =  (Button) mInflater.inflate(R.layout.tip_letter_button,null);
            tipButton.setTag(i);
            tipButton.setText("?");
            tipButton.setLayoutParams(layoutParams);
            tipButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    tipsUsed = true;

                    CharSequence text = mTranslateTextView.getText();


                    //сначала проверим если был введен неправильный вариант
                    for (int i = 0; i< text.length(); i++) {
                        if (String.valueOf(text.charAt(i)).equalsIgnoreCase(String.valueOf(mCurrentTranslate.charAt(i)))) {
                            continue;
                        }
                        //убираем все буквы из нажатых начинач с индекса первого расхождения
                        clearButtonsBeginAt(i);
                        //перезависываем обновленный текст
                        text = mTranslateTextView.getText();
                        break;
                    }

                    char letter = mCurrentTranslate.charAt(text.length());
                    for (Button letterButton:mButtons.get(String.valueOf(letter).toLowerCase())) {
                        if (mClickedButtons.contains(letterButton)) continue;
                        addButtonToClickedList(letterButton);
                        break;
                    }
                    mTranslateTextView.setText(TextUtils.concat(text,String.valueOf(letter)));
                    checkResult(false);

                }
            });
            row.addView(tipButton);
        }

    }

    int getClickedButtonIndex(int index) {

        for (int i = 0;i < mClickedButtons.size();i++) {
            int buttonIndex = (int) mClickedButtons.get(i).getTag();
            if (buttonIndex == index) return i;

        }
        return -1;
    }

    void setButtonToClickedList(int index,Button button) {

        mClickedButtons.set(index,button);
        button.setVisibility(View.INVISIBLE);
        if (mClickedButtons.size() == mCurrentTranslate.length() && tipButton != null) {
            tipButton.setVisibility(View.INVISIBLE);
        }
    }
    void addButtonToClickedList(Button button) {

        mClickedButtons.add(button);
        button.setVisibility(View.INVISIBLE);
        if (mClickedButtons.size() == mCurrentTranslate.length() && tipButton != null) {
            tipButton.setVisibility(View.INVISIBLE);
        }
    }
    void checkResult(Boolean skip) {

        try {
            String translateText = mTranslateTextView.getText().toString();
            if (skip) {
                setResult(!emptyAnswerIsWrong);
                wordsSkip++;
                //если wrongAnswersToSkip>0 тогда проверяем на количество нажатых скипов
                if (wrongAnswersToSkip > 0 && wordsSkip == wrongAnswersToSkip) {
                    dettachLockScreenView();
                    return;
                }
            } else if (translateText.length() == mCurrentTranslate.length()) {

                Boolean tipWasNotUsed = !useAdapitiveTips || (useAdapitiveTips && !tipsUsed);

                if (setResult(mCurrentTranslate.equalsIgnoreCase(translateText))) {
                    mTranslateTextView.setBackgroundColor(getResources().getColor(R.color.correct_world));
                    if (tipWasNotUsed) wordsPerLessonCurrent++;
                }   else {
                    mTranslateTextView.setText(mCurrentTranslate);//вставляем правильное слово
                    mTranslateTextView.setBackgroundColor(getResources().getColor(R.color.incorrect_world));
                    mTranslateTextView.startAnimation(shakeanimation);
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            initWord(true);
                        }
                    }, 1000);
                    return;
                }

            } else {

                int nextCharIndex = translateText.length();

                //проверяем на правильность ввода.ведь пробелы надо ставить только когда 'все идет по плану (с)'
                CharSequence rightText = mCurrentTranslate.subSequence(0,nextCharIndex);
                if (rightText.equals(translateText)) {
                    //проверим если следующий символ пробел то добавим его в  список нажатых
                    char letter = mCurrentTranslate.charAt(nextCharIndex);
                    if (String.valueOf(letter).equalsIgnoreCase(" ")) {

                        for (Button letterButton : mButtons.get(String.valueOf(letter).toLowerCase())) {
                            if (mClickedButtons.contains(letterButton)) continue;
                            addButtonToClickedList(letterButton);
                            break;
                        }
                        mTranslateTextView.setText(TextUtils.concat(translateText,String.valueOf(letter)));
                        checkResult(false);
                    }
                }
                return;
            }


            setComplete(wordsPerLessonCurrent+"/"+wordsPerLesson);

            if (wordsPerLessonCurrent < wordsPerLesson) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        initWord(true);
                    }
                }, 1000);
            } else {
                //закрываем окно
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dettachLockScreenView();
                    }
                }, 1000);

            }
        } catch (Exception e) {
            dettachLockScreenView();
        }
        //просто ввод не обрабатываем
    }

    public Boolean setResult(boolean result) {
        addResult(result);
        return result;
    }

    private void clearButtonsBeginAt(int index) {

        if (mClickedButtons.size() == 0) return;
        while (index < mClickedButtons.size() && index >= 0) {
            Button lastButton = mClickedButtons.remove(index);
            lastButton.setVisibility(View.VISIBLE);
            //lastButton.startAnimation(alphaIncreaseAnimation);
            CharSequence text = mTranslateTextView.getText();
            mTranslateTextView.setText(text.subSequence(0, text.length() - 1));
            if (mClickedButtons.size() != mCurrentTranslate.length() && tipButton != null ) {
                tipButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private void clearLastButton() {

        clearButtonsBeginAt(mClickedButtons.size()-1);
    }

    private void removeAllButtons() {

       /* for (int i = 0; i < mTableLayout.getChildCount();i++) {
            TableRow tableRow = (TableRow) mTableLayout.getChildAt(i);
            for (int j = 0; j < tableRow.getChildCount();j++) {
                tableRow.removeViewAt(j);
            }
            mTableLayout.removeViewAt(i);
        }*/
        mTableLayout.removeAllViews();
    }

    private void setComplete(String value) {
        mCompleteText.setText(value);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private boolean isAttachedToWindow() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1 )
            return mLockscreenView.isShown();
        else
            return mLockscreenView.isAttachedToWindow();
    }


    private void settingLockView() {
        AdView mAdView = mLockscreenView.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mTableLayout = (TableLayout) mLockscreenView.findViewById(R.id.grid_buttons);
        mWordTextView = (TextView) mLockscreenView.findViewById(R.id.word_text);
        mTranslateTextView = (TextView) mLockscreenView.findViewById(R.id.translate_text);
        mCompleteText = (TextView) mLockscreenView.findViewById(R.id.complete_text);
        skipButton = (Button) mLockscreenView.findViewById(R.id.button_skip);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mTranslateTextView.setText(mCurrentTranslate);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkResult(true);
                    }
                }, 1000);

            }
        });
        mWidth = mTableLayout.getMeasuredWidth();

        shakeanimation = AnimationUtils.loadAnimation(this, R.anim.incorrect_shake);
        //alphaIncreaseAnimation = AnimationUtils.loadAnimation(this, R.anim.increase_alpha);
        //alphaDecreaseAnimation  = AnimationUtils.loadAnimation(this, R.anim.decrease_alpha);


        SharedPreferences preferences = getSharedPreferences("preference", Activity.MODE_PRIVATE);

        emptyAnswerIsWrong = preferences.getBoolean(currentDictId + DBHelper.CN_EMPTY_ANSWER_IS_WRONG, emptyAnswerIsWrongDef);
        useAdapitiveTips = preferences.getBoolean(currentDictId + DBHelper.CN_USE_TIPS, useAdapitiveTipsDef);
        wordsPerLesson = Integer.valueOf(preferences.getString(currentDictId + DBHelper.CN_WORDS_PER_LESSON, wordsPerLessonDef));
        wordsInStudy = Integer.valueOf(preferences.getString(currentDictId + DBHelper.CN_WORDS_STUDY, wordsInStudyDef));
        rightPercent = Integer.valueOf(preferences.getString(currentDictId + DBHelper.CN_RIGHT_ANSWER_PERCENT, rightPercentDef));
        wrongPercent = Integer.valueOf(preferences.getString(currentDictId + DBHelper.CN_WRONG_ANSWER_PERCENT, wrongPercentDef));
        wrongAnswersToSkip = Integer.valueOf(preferences.getString(currentDictId + DBHelper.CN_WRONG_ANSWERS_TO_SKIP, wrongAnswersToSkipDef));
        mStatusForgruondDummyView = (RelativeLayout) mLockscreenView.findViewById(R.id.lockscreen_forground_status_dummy);
        //setBackGroundLockView();

        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        mDeviceWidth = displayMetrics.widthPixels;
        int val = LockScreenUtil.getInstance(mContext).getStatusBarHeight();
        RelativeLayout.LayoutParams backgroundParam = (RelativeLayout.LayoutParams) mStatusForgruondDummyView.getLayoutParams();
        backgroundParam.height = displayMetrics.heightPixels + val;
        mStatusForgruondDummyView.setLayoutParams(backgroundParam);

        wordsPerLessonCurrent = 0;
        wordsSkip = 0;

        getWordsStack();
    }
    public String randomize(String s){

        String newString = "";
        String oldString = s;

        while (oldString.length() > 0) {

            Random random = new Random();
            int index = random.nextInt(oldString.length());

            char letter = oldString.charAt(index);
            newString = newString + letter;

            StringBuilder sb = new StringBuilder(oldString);
            sb.deleteCharAt(index);
            oldString = sb.toString();
        }

        return newString;

    }

    private void getWordsStack() {

        @SuppressLint("StaticFieldLeak") AsyncTask<Void, Integer, Cursor> getWords =  new AsyncTask<Void, Integer, Cursor>() {

            @Override
            protected Cursor doInBackground(Void... params) {

                SQLiteDatabase db = mSqLiteOpenHelper.getReadableDatabase();

                //return db.rawQuery("select * from words",null);
                return db.rawQuery("select * from ("+DBHelper.getWordsQuery(currentDictId,direction,"w.lock = 0")+") where completely < 100 order by date ASC,wrong DESC,right ASC limit "+Integer.toString(wordsInStudy),null);
            }

            @Override
            protected void onProgressUpdate(Integer... progress) {
            }

            @Override
            protected void onPostExecute(Cursor cursor) {

                mCursor = cursor;
                initWord(true);
              }

            @Override
            protected void onCancelled() {

            }
        };

        getWords.execute();
    }

    private void addResult(Boolean result) {

        @SuppressLint("StaticFieldLeak") AsyncTask<Boolean, Integer, Boolean> addResult =  new AsyncTask<Boolean, Integer, Boolean>() {

            @Override
            protected Boolean doInBackground(Boolean... params) {

                Boolean result = params[0];
                ContentValues values = new ContentValues();
                values.put(DBHelper.CN_ID_DICT,currentDictId);
                values.put(DBHelper.CN_ID_WORD,currentWordId);
                values.put(DBHelper.CN_COMPLETELY,result?rightPercent:-wrongPercent);
                values.put(DBHelper.CN_DIRECTION,direction);

                SQLiteDatabase db = mSqLiteOpenHelper.getWritableDatabase();
                return db.insert(DBHelper.TB_STATS,null,values) > 0;
            }

            @Override
            protected void onProgressUpdate(Integer... progress) {
            }

            @Override
            protected void onPostExecute(Boolean result) {

            }

            @Override
            protected void onCancelled() {

            }
        };

        addResult.execute(result);
    }

}
