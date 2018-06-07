package org.mobilburger.learnwords;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.widget.Toast;

public class PermissionActivity extends Activity{

    LockScreenUtil mLockScreenUtil = null;
    private static final int PERMISSION_REQUEST = 1;
    public static final String PERMISSION_OVERLAY = "PERMISSION_OVERLAY";
    public static final String PERMISSION_PHONE_STATE = "PERMISSION_PHONE_STATE";

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getAction() == PERMISSION_OVERLAY) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 123);
        } else
        if (getIntent().getAction() == PERMISSION_PHONE_STATE) {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},
                        PERMISSION_REQUEST);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case 123:
                if (Settings.canDrawOverlays(this)) {
                    LockScreenUtil.getInstance(this).getPermissionCheckSubject().onNext(true);
                }
                break;
        }
        finish();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LockScreenUtil.getInstance(this).getPermissionCheckSubject().onNext(true);
                finish();

            } else {
                Toast.makeText(this, getString(R.string.error_no_permission_allowed), Toast.LENGTH_SHORT).show();
            }
        }
    }

}
