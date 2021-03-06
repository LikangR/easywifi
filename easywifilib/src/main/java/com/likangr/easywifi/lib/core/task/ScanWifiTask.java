package com.likangr.easywifi.lib.core.task;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.likangr.easywifi.lib.EasyWifi;
import com.likangr.easywifi.lib.core.guid.UserActionGuideToast;
import com.likangr.easywifi.lib.util.IntentManager;


/**
 * @author likangren
 */
public final class ScanWifiTask extends SpecificWifiTask {

    private long mScanWifiTimeout;
    private int mScanWifiWay;
    private boolean mIsAutoSwitchToThroughSystemWifi;

    private boolean mIsHasTriedThroughSystemWifi = false;

    public ScanWifiTask() {
    }


    public ScanWifiTask(long scanWifiTimeout,
                        int scanWifiWay,
                        boolean isAutoSwitchToThroughSystemWifi,
                        WifiTaskCallback wifiTaskCallback) {
        super(wifiTaskCallback);
        mScanWifiTimeout = scanWifiTimeout;
        mScanWifiWay = scanWifiWay;
        mIsAutoSwitchToThroughSystemWifi = isAutoSwitchToThroughSystemWifi;
    }


    private ScanWifiTask(Parcel in) {
        super(in);
        mScanWifiTimeout = in.readLong();
        mScanWifiWay = in.readInt();
        mIsAutoSwitchToThroughSystemWifi = in.readByte() == 1;
    }


    public long getScanWifiTimeout() {
        return mScanWifiTimeout;
    }

    public void setScanWifiTimeout(long scanWifiTimeout) {
        mScanWifiTimeout = scanWifiTimeout;
    }

    public int getScanWifiWay() {
        return mScanWifiWay;
    }

    public void setScanWifiWay(int scanWifiWay) {
        mScanWifiWay = scanWifiWay;
    }

    public boolean isIsAutoSwitchToThroughSystemWifi() {
        return mIsAutoSwitchToThroughSystemWifi;
    }

    public void setIsAutoSwitchToThroughSystemWifi(boolean isAutoSwitchToThroughSystemWifi) {
        mIsAutoSwitchToThroughSystemWifi = isAutoSwitchToThroughSystemWifi;
    }


    @Override
    public void checkIsValid() {

        if (mScanWifiTimeout < 0) {
            throw new IllegalArgumentException("ScanWifiTimeout must more than 0!");
        }

        if (mScanWifiWay != EasyWifi.SCAN_WIFI_WAY_THROUGH_WIFI_SETTING && mScanWifiWay != EasyWifi.SCAN_WIFI_WAY_INITIATIVE) {
            throw new IllegalArgumentException("ScanWifiWay must be one of EasyWifi.SCAN_WIFI_WAY_THROUGH_WIFI_SETTING or EasyWifi.SCAN_WIFI_WAY_INITIATIVE");
        }

    }

    @Override
    protected void onEnvironmentPrepared() {
        scanWifi(false);
    }

    private void scanWifi(boolean isTryThroughSystemWifi) {
        boolean requestStartScanResult = false;
        boolean isNeedSwitchToThroughSystemWifi = false;

        if (!isTryThroughSystemWifi && EasyWifi.SCAN_WIFI_WAY_INITIATIVE == mScanWifiWay) {
            //Take the initiative to call
            requestStartScanResult = EasyWifi.getWifiManager().startScan();
            if (!requestStartScanResult && mIsAutoSwitchToThroughSystemWifi) {
                isNeedSwitchToThroughSystemWifi = true;
            }
        }

        if (isTryThroughSystemWifi || isNeedSwitchToThroughSystemWifi || EasyWifi.SCAN_WIFI_WAY_THROUGH_WIFI_SETTING == mScanWifiWay) {

            requestStartScanResult = true;
            IntentManager.gotoRequestSystemWifiScanActivity();
        }

        if (requestStartScanResult) {

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

            registerAutoReleaseReceiver(new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {

                    boolean isSuccess = true;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED,
                                false)) {
                            isSuccess = false;
                        }
                    }

                    if (isSuccess) {
                        callOnTaskSuccess();
                    } else {
                        callOnTaskFail(EasyWifi.FAIL_REASON_SCAN_WIFI_UNKNOWN);
                    }
                }
            }, intentFilter, mScanWifiTimeout, EasyWifi.FAIL_REASON_SCAN_WIFI_TIMEOUT);

        } else {
            callOnTaskFail(EasyWifi.FAIL_REASON_SCAN_WIFI_REQUEST_NOT_BE_SATISFIED);
        }
    }

    @Override
    protected void initPrepareEnvironment(PrepareEnvironmentTask prepareEnvironmentTask) {
        if (PrepareEnvironmentTask.isAboveLollipopMr1()) {
            prepareEnvironmentTask.setIsNeedLocationPermission(true);
            prepareEnvironmentTask.setIsNeedEnableLocation(true);
        }
        if (!EasyWifi.isWifiEnabled()) {
            prepareEnvironmentTask.setIsNeedWifiPermission(true);
            prepareEnvironmentTask.setIsNeedSetWifiEnabled(true);
            prepareEnvironmentTask.setWifiEnabled(true);
            prepareEnvironmentTask.setSetWifiEnabledTimeout(EasyWifi.TIME_OUT_SET_WIFI_ENABLED_DEFAULT);
        }
    }

    @Override
    public synchronized void callOnTaskFail(int failReason) {
        if (failReason == EasyWifi.FAIL_REASON_SCAN_WIFI_TIMEOUT ||
                failReason == EasyWifi.FAIL_REASON_SCAN_WIFI_UNKNOWN) {
            if (!mIsHasTriedThroughSystemWifi &&
                    EasyWifi.SCAN_WIFI_WAY_INITIATIVE == mScanWifiWay &&
                    mIsAutoSwitchToThroughSystemWifi) {
                unregisterAutoReleaseReceiver();
                mIsHasTriedThroughSystemWifi = true;
                scanWifi(true);
            } else {
                super.callOnTaskFail(failReason);
            }
        } else {
            super.callOnTaskFail(failReason);
        }
    }

    public static final Creator<ScanWifiTask> CREATOR = new Creator<ScanWifiTask>() {
        @Override
        public ScanWifiTask createFromParcel(Parcel in) {
            return new ScanWifiTask(in);
        }

        @Override
        public ScanWifiTask[] newArray(int size) {
            return new ScanWifiTask[size];
        }
    };


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(mScanWifiTimeout);
        dest.writeInt(mScanWifiWay);
        dest.writeByte((byte) (mIsAutoSwitchToThroughSystemWifi ? 1 : 0));
    }

    @Override
    public String toString() {
        return "ScanWifiTask{" +
                "mScanWifiTimeout=" + mScanWifiTimeout +
                ", mScanWifiWay=" + mScanWifiWay +
                ", mIsAutoSwitchToThroughSystemWifi=" + mIsAutoSwitchToThroughSystemWifi +
                ", mWifiTaskCallback=" + mWifiTaskCallback +
                ", mRunningCurrentStep=" + mRunningCurrentStep +
                ", mLastRunningCurrentStep=" + mLastRunningCurrentStep +
                ", mFailReason=" + mFailReason +
                ", mCurrentStatus=" + mCurrentStatus +
                ", mIsResumeTask=" + mIsResumeTask +
                '}';
    }

    public static class RequestSystemWifiScanActivity extends AppCompatActivity {

        private static final long WAIT_INVOKE_STOP_TIMEOUT = 2000;
        private static final long SETTING_WIFI_PAGE_SHOW_TIME = 500;

        private Runnable mFinishRunnable = new Runnable() {
            @Override
            public void run() {
                UserActionGuideToast.dismiss();
                startActivity(new Intent(RequestSystemWifiScanActivity.this, RequestSystemWifiScanActivity.class));
                //for compat:
                ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                activityManager.moveTaskToFront(RequestSystemWifiScanActivity.this.getTaskId(), 0);
            }
        };

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            IntentManager.gotoWifiSettings(this);
            UserActionGuideToast.show(this, "正在扫描wifi",
                    "即将返回刚才的页面", Toast.LENGTH_SHORT);

            EasyWifi.getHandler().postDelayed(mFinishRunnable, WAIT_INVOKE_STOP_TIMEOUT);
        }


        @Override
        protected void onStop() {
            super.onStop();
            EasyWifi.getHandler().removeCallbacks(mFinishRunnable);
            EasyWifi.getHandler().postDelayed(mFinishRunnable, SETTING_WIFI_PAGE_SHOW_TIME);
        }

        @Override
        protected void onNewIntent(Intent intent) {
            super.onNewIntent(intent);
            finish();
        }

    }
}
