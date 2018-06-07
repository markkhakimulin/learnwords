package org.mobilburger.learnwords;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.view.View;
import android.view.ViewTreeObserver;

import rx.subjects.AsyncSubject;
import rx.subjects.PublishSubject;

public class LockScreenUtil {
    private Context mContext = null;
    public static final String ISLOCK = "ISLOCK";
    public static final String ISSOFTKEY = "ISSOFTKEY";

    public static final int MSG_WORDS_LOADED = 1;

    private static LockScreenUtil mLockscreenUtilInstance;

    private PublishSubject<Boolean> permissionCheckSubject;

    public static LockScreenUtil getInstance(Context context) {
        if (mLockscreenUtilInstance == null) {
            if (null != context) {
                mLockscreenUtilInstance = new LockScreenUtil(context);
            }
            else {
                mLockscreenUtilInstance = new LockScreenUtil();
            }
        }
        return mLockscreenUtilInstance;
    }

    private LockScreenUtil() {
        mContext = null;
    }

    private LockScreenUtil(Context context) {
        mContext = context;
    }
    public boolean isStandardKeyguardState() {
        boolean isStandardKeyguqrd = false;
        KeyguardManager keyManager =(KeyguardManager) mContext.getSystemService(mContext.KEYGUARD_SERVICE);
        if (null != keyManager) {
            isStandardKeyguqrd = keyManager.isKeyguardSecure();
        }

        return isStandardKeyguqrd;
    }

    public boolean isSoftKeyAvail(Context context) {
        final boolean[] isSoftkey = {false};
        final View activityRootView = ((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int rootViewHeight = activityRootView.getRootView().getHeight();
                int viewHeight = activityRootView.getHeight();
                int heightDiff = rootViewHeight - viewHeight;
                if (heightDiff > 100) { // 99% of the time the height diff will be due to a keyboard.
                    isSoftkey[0] = true;
                }
            }
        });
        return isSoftkey[0];
    }

    public int getStatusBarHeight(){
        int result=0;
        int resourceId= mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if(resourceId >0)
            result = mContext.getResources().getDimensionPixelSize(resourceId);

        return result;
    }
    public PublishSubject<Boolean> createAsyncSubject() {
        permissionCheckSubject = PublishSubject.create();
        return permissionCheckSubject;
    }

    public PublishSubject<Boolean> getPermissionCheckSubject() {
      /*  if (null == permissionCheckSubject) {
            permissionCheckSubject = AsyncSubject.create();
        }
*/
        return permissionCheckSubject;
    }

    public boolean isScreenOnAndNotLocked(Context context) {

        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode()) {
            return false;
        }
        return true;
    }

    public boolean isServiceRunning(Class<?> serviceClass) {
        return isServiceRunning(mContext,serviceClass);
    }

    private boolean isServiceRunning(Context context,Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
