package com.likang.easywifi.demo;

import android.content.DialogInterface;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.likang.easywifi.lib.EasyWifi;
import com.likang.easywifi.lib.core.task.CheckIsAlreadyConnectedTask;
import com.likang.easywifi.lib.core.task.ConnectToWifiTask;
import com.likang.easywifi.lib.core.task.GetConnectionInfoTask;
import com.likang.easywifi.lib.core.task.GetWifiPermissionTask;
import com.likang.easywifi.lib.core.task.ScanWifiTask;
import com.likang.easywifi.lib.core.task.SetWifiEnabledTask;
import com.likang.easywifi.lib.core.task.WifiTask;
import com.likang.easywifi.lib.core.task.WifiTaskCallback;
import com.likang.easywifi.lib.util.Logger;
import com.likang.easywifi.lib.util.StringUtils;
import com.likang.easywifi.lib.util.ToastUtil;
import com.likang.easywifi.lib.util.WifiEncryptionScheme;
import com.likang.easywifi.lib.util.WifiUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = "MainActivity";
    private static final String SAVE_INSTANCE_STATE_KEY_WIFI_TASK = "wifi_task";
    private TextView mTvConnectionInfo;
    private ListView mLvWifiList;
    private ProgressBar mPbWait;
    private ArrayList<String> mWifiSsidList = new ArrayList<>();
    private List<ScanResult> mScanResults;
    private ArrayAdapter<String> mWifiListAdapter;

    private WifiTaskCallback mGetWifiPermissionTaskCallback = new WifiTaskCallback<GetWifiPermissionTask>() {
        @Override
        public void onTaskStartRun(GetWifiPermissionTask wifiTask) {
            Logger.d(TAG, "onTaskStartRun");
        }

        @Override
        public void onTaskRunningCurrentStep(GetWifiPermissionTask wifiTask) {
            Logger.d(TAG, "onTaskRunningCurrentStep,currentStep=" + wifiTask.getRunningCurrentStep());
        }

        @Override
        public void onTaskSuccess(GetWifiPermissionTask wifiTask) {
            Logger.d(TAG, "onTaskSuccess");
            ToastUtil.showShort(MainActivity.this, "获取wifi操作权限成功");
        }

        @Override
        public void onTaskFail(GetWifiPermissionTask wifiTask) {
            int failReason = wifiTask.getFailReason();
            Logger.d(TAG, "onTaskRunningCurrentStep,failReason=" + failReason);
            if (failReason == EasyWifi.FAIL_REASON_WIFI_MODULE_NOT_EXIST) {
                ToastUtil.showShort(MainActivity.this, "获取wifi操作权限失败，您的设备没有wifi模块");
            } else if (failReason == EasyWifi.FAIL_REASON_NOT_HAS_WIFI_PERMISSION) {
                ToastUtil.showShort(MainActivity.this, "获取wifi操作权限失败,用户拒绝");
            }
        }

    };

    private WifiTaskCallback mOnSetWifiEnabledCallback = new WifiTaskCallback<SetWifiEnabledTask>() {
        @Override
        public void onTaskStartRun(SetWifiEnabledTask wifiTask) {
            Logger.d(TAG, "onTaskStartRun enabled=" + wifiTask.isEnabled());
            setPbVisible(true);
        }

        @Override
        public void onTaskRunningCurrentStep(SetWifiEnabledTask wifiTask) {
            Logger.d(TAG, "onTaskRunningCurrentStep enabled=" + wifiTask.isEnabled() + ",currentStep=" + wifiTask.getRunningCurrentStep());
        }

        @Override
        public void onTaskSuccess(SetWifiEnabledTask wifiTask) {
            Logger.d(TAG, "onTaskSuccess enabled=" + wifiTask.isEnabled());
            setPbVisible(false);
            ToastUtil.showShort(MainActivity.this, (wifiTask.isEnabled() ? "开启" : "关闭") + "wifi成功");
        }

        @Override
        public void onTaskFail(SetWifiEnabledTask wifiTask) {
            int failReason = wifiTask.getFailReason();
            Logger.d(TAG, "onTaskRunningCurrentStep enabled=" + wifiTask.isEnabled() + ",failReason=" + failReason);
            setPbVisible(false);
            if (failReason == EasyWifi.FAIL_REASON_WIFI_MODULE_NOT_EXIST) {
                ToastUtil.showShort(MainActivity.this, (wifiTask.isEnabled() ? "开启" : "关闭") + "wifi失败，您的设备没有wifi模块");
            } else if (failReason == EasyWifi.FAIL_REASON_NOT_HAS_WIFI_PERMISSION) {
                ToastUtil.showShort(MainActivity.this, (wifiTask.isEnabled() ? "开启" : "关闭") + "wifi失败，没有wifi操作权限");
            } else if (failReason == EasyWifi.FAIL_REASON_SET_WIFI_ENABLED_TIMEOUT) {
                ToastUtil.showShort(MainActivity.this, (wifiTask.isEnabled() ? "开启" : "关闭") + "wifi失败，操作超时");
            } else if (failReason == EasyWifi.FAIL_REASON_SET_WIFI_ENABLED_REQUEST_NOT_BE_SATISFIED) {
                ToastUtil.showShort(MainActivity.this, (wifiTask.isEnabled() ? "开启" : "关闭") + "wifi失败，系统不允许" + (wifiTask.isEnabled() ? "开启" : "关闭") + "wifi");
            }

        }

    };


    private WifiTaskCallback mOnScanWifiCallback = new WifiTaskCallback<ScanWifiTask>() {
        @Override
        public void onTaskStartRun(ScanWifiTask wifiTask) {
            Logger.d(TAG, "onTaskStartRun");
            setPbVisible(true);
        }

        @Override
        public void onTaskRunningCurrentStep(ScanWifiTask wifiTask) {
            Logger.d(TAG, "onTaskRunningCurrentStep currentStep=" + wifiTask.getRunningCurrentStep());
        }

        @Override
        public void onTaskSuccess(ScanWifiTask wifiTask) {
            Logger.d(TAG, "onTaskSuccess");
            setPbVisible(false);
            List<ScanResult> scanResults = EasyWifi.getScanResults();
            updateWifiSsidListFromScanResults(scanResults);
            ToastUtil.showShort(MainActivity.this, "wifi扫描成功");
        }

        @Override
        public void onTaskFail(ScanWifiTask wifiTask) {
            int failReason = wifiTask.getFailReason();
            Logger.d(TAG, "onTaskFail failReason=" + failReason);
            setPbVisible(false);


            if (failReason == EasyWifi.FAIL_REASON_LOCATION_MODULE_NOT_EXIST) {
                ToastUtil.showShort(MainActivity.this, "wifi扫描失败，您的设备没有定位服务");
            } else if (failReason == EasyWifi.FAIL_REASON_LOCATION_MODULE_DISABLE) {
                ToastUtil.showShort(MainActivity.this, "wifi扫描失败，您的定位服务没有开启");
            } else if (failReason == EasyWifi.FAIL_REASON_NOT_HAS_LOCATION_PERMISSIONS) {
                ToastUtil.showShort(MainActivity.this, "wifi扫描失败，没有开启定位服务的权限");
            } else if (failReason == EasyWifi.FAIL_REASON_WIFI_MODULE_NOT_EXIST) {
                ToastUtil.showShort(MainActivity.this, "wifi扫描失败，您的设备没有wifi模块");
            } else if (failReason == EasyWifi.FAIL_REASON_NOT_HAS_WIFI_PERMISSION) {
                ToastUtil.showShort(MainActivity.this, "wifi扫描失败，没有wifi操作权限");
            } else if (failReason == EasyWifi.FAIL_REASON_SET_WIFI_ENABLED_TIMEOUT) {
                ToastUtil.showShort(MainActivity.this, "wifi扫描失败，开启wifi超时");
            } else if (failReason == EasyWifi.FAIL_REASON_SET_WIFI_ENABLED_REQUEST_NOT_BE_SATISFIED) {
                ToastUtil.showShort(MainActivity.this, "wifi扫描失败，系统不允许开启wifi");
            } else if (failReason == EasyWifi.FAIL_REASON_SCAN_WIFI_UNKNOWN) {
                ToastUtil.showShort(MainActivity.this, "wifi扫描失败，未知原因");
            } else if (failReason == EasyWifi.FAIL_REASON_SCAN_WIFI_TIMEOUT) {
                ToastUtil.showShort(MainActivity.this, "wifi扫描失败，扫描超时");
            } else if (failReason == EasyWifi.FAIL_REASON_SCAN_WIFI_REQUEST_NOT_BE_SATISFIED) {
                ToastUtil.showShort(MainActivity.this, "wifi扫描失败，系统不允许扫描");
            }
        }

    };
    private WifiTaskCallback mOnGetConnectionInfoCallback = new WifiTaskCallback<GetConnectionInfoTask>() {
        @Override
        public void onTaskStartRun(GetConnectionInfoTask wifiTask) {
            Logger.d(TAG, "onTaskStartRun");
        }

        @Override
        public void onTaskRunningCurrentStep(GetConnectionInfoTask wifiTask) {
            Logger.d(TAG, "onTaskRunningCurrentStep currentStep=" + wifiTask.getRunningCurrentStep());
        }

        @Override
        public void onTaskSuccess(GetConnectionInfoTask wifiTask) {
            WifiInfo wifiInfo = wifiTask.getWifiInfo();
            Logger.d(TAG, "onTaskSuccess wifiInfo=" + wifiInfo);
            mTvConnectionInfo.setText(String.format("connection info: %s %s %s", StringUtils.removeQuotationMarks(wifiInfo.getSSID()), wifiInfo.getBSSID(), wifiInfo.getSupplicantState()));
            ToastUtil.showShort(MainActivity.this, "获取当前wifi信息成功");
        }

        @Override
        public void onTaskFail(GetConnectionInfoTask wifiTask) {
            int failReason = wifiTask.getFailReason();
            Logger.d(TAG, "onTaskFail failReason=" + failReason);
            mTvConnectionInfo.setText("connection info: unknown");

            if (failReason == EasyWifi.FAIL_REASON_LOCATION_MODULE_NOT_EXIST) {
                ToastUtil.showShort(MainActivity.this, "获取当前wifi信息失败，您的设备没有定位服务");
            } else if (failReason == EasyWifi.FAIL_REASON_LOCATION_MODULE_DISABLE) {
                ToastUtil.showShort(MainActivity.this, "获取当前wifi信息失败，定位服务没有开启");
            } else if (failReason == EasyWifi.FAIL_REASON_NOT_HAS_LOCATION_PERMISSIONS) {
                ToastUtil.showShort(MainActivity.this, "获取当前wifi信息失败，没有开启定位服务的权限");
            }
        }

    };
    private WifiTaskCallback mOnConnectToWifiCallback = new WifiTaskCallback<ConnectToWifiTask>() {
        @Override
        public void onTaskStartRun(ConnectToWifiTask wifiTask) {
            Logger.d(TAG, "onTaskStartRun");
            setPbVisible(true);
        }

        @Override
        public void onTaskRunningCurrentStep(ConnectToWifiTask wifiTask) {
            Logger.d(TAG, "onTaskRunningCurrentStep currentStep=" + wifiTask.getRunningCurrentStep());
        }

        @Override
        public void onTaskSuccess(ConnectToWifiTask wifiTask) {
            Logger.d(TAG, "onTaskSuccess");
            setPbVisible(false);
            updateConnectionInfo();
            ToastUtil.showShort(MainActivity.this, "连接成功");
        }

        @Override
        public void onTaskFail(ConnectToWifiTask wifiTask) {
            int failReason = wifiTask.getFailReason();
            Logger.d(TAG, "onTaskFail failReason=" + failReason);
            setPbVisible(false);
            updateConnectionInfo();

            if (failReason == EasyWifi.FAIL_REASON_LOCATION_MODULE_NOT_EXIST) {
                ToastUtil.showShort(MainActivity.this, "连接失败，您的设备没有定位服务");
            } else if (failReason == EasyWifi.FAIL_REASON_LOCATION_MODULE_DISABLE) {
                ToastUtil.showShort(MainActivity.this, "连接失败，定位服务没有开启");
            } else if (failReason == EasyWifi.FAIL_REASON_NOT_HAS_LOCATION_PERMISSIONS) {
                ToastUtil.showShort(MainActivity.this, "连接失败，没有开启定位服务的权限");
            } else if (failReason == EasyWifi.FAIL_REASON_WIFI_MODULE_NOT_EXIST) {
                ToastUtil.showShort(MainActivity.this, "连接失败，您的设备没有wifi模块");
            } else if (failReason == EasyWifi.FAIL_REASON_NOT_HAS_WIFI_PERMISSION) {
                ToastUtil.showShort(MainActivity.this, "连接失败，没有wifi操作权限");
            } else if (failReason == EasyWifi.FAIL_REASON_SET_WIFI_ENABLED_TIMEOUT) {
                ToastUtil.showShort(MainActivity.this, "连接失败，开启wifi超时");
            } else if (failReason == EasyWifi.FAIL_REASON_SET_WIFI_ENABLED_REQUEST_NOT_BE_SATISFIED) {
                ToastUtil.showShort(MainActivity.this, "连接失败，系统不允许开启wifi");
            } else if (failReason == EasyWifi.FAIL_REASON_CONNECT_TO_WIFI_MUST_THROUGH_SYSTEM_WIFI_SETTING) {
                ToastUtil.showShort(MainActivity.this, "连接失败，需要您在系统WIFI设置中连接");
            } else if (failReason == EasyWifi.FAIL_REASON_CONNECT_TO_WIFI_ARGUMENTS_ERROR) {
                ToastUtil.showShort(MainActivity.this, "连接失败，参数有误");
            } else if (failReason == EasyWifi.FAIL_REASON_CONNECT_TO_WIFI_UNKNOWN) {
                ToastUtil.showShort(MainActivity.this, "连接失败，未知原因");
            } else if (failReason == EasyWifi.FAIL_REASON_CONNECT_TO_WIFI_IS_POOR_LINK) {
                ToastUtil.showShort(MainActivity.this, "连接失败，网络质量太差");
            } else if (failReason == EasyWifi.FAIL_REASON_CONNECT_TO_WIFI_NOT_OBTAINED_IP_ADDR) {
                ToastUtil.showShort(MainActivity.this, "连接失败，获取不到ip");
            } else if (failReason == EasyWifi.FAIL_REASON_CONNECT_TO_WIFI_ERROR_AUTHENTICATING) {
                ToastUtil.showShort(MainActivity.this, "连接失败，密码错误请重新输入");
                showPwdDialog(null, wifiTask.getWifiConfiguration(), WifiEncryptionScheme.getEncryptionSchemeByWifiConfiguration(wifiTask.getWifiConfiguration()));
            } else if (failReason == EasyWifi.FAIL_REASON_CONNECT_TO_WIFI_TIMEOUT) {
                ToastUtil.showShort(MainActivity.this, "连接失败，连接超时");
            } else if (failReason == EasyWifi.FAIL_REASON_CONNECT_TO_WIFI_REQUEST_NOT_BE_SATISFIED) {
                ToastUtil.showShort(MainActivity.this, "连接失败，系统不允许连接");
            }
        }

    };
    private WifiTaskCallback mOnCheckIsAlreadyConnectedCallback = new WifiTaskCallback<CheckIsAlreadyConnectedTask>() {
        @Override
        public void onTaskStartRun(CheckIsAlreadyConnectedTask wifiTask) {
            Logger.d(TAG, "onTaskStartRun");
            setPbVisible(true);
        }

        @Override
        public void onTaskRunningCurrentStep(CheckIsAlreadyConnectedTask wifiTask) {
            Logger.d(TAG, "onTaskRunningCurrentStep currentStep=" + wifiTask.getRunningCurrentStep());
        }

        @Override
        public void onTaskSuccess(CheckIsAlreadyConnectedTask wifiTask) {
            boolean isAlreadyConnected = wifiTask.getIsAlreadyConnected();
            Logger.d(TAG, "onTaskSuccess isAlreadyConnected=" + isAlreadyConnected + ",ssid=" + wifiTask.getSsid() + ",bssid" + wifiTask.getBssid());
            final ScanResult scanResult = wifiTask.getScanResult();
            setPbVisible(false);

            if (!isAlreadyConnected) {

                WifiConfiguration configuredWifiConfiguration = EasyWifi.getConfiguredWifiConfiguration(scanResult);
                if (configuredWifiConfiguration != null) {

                    boolean isNeedUpdateWifiConfiguration = false;
                    String newEncryptionScheme = WifiEncryptionScheme.getEncryptionSchemeByScanResult(scanResult);
                    if (newEncryptionScheme.equals(WifiEncryptionScheme.getEncryptionSchemeByWifiConfiguration(configuredWifiConfiguration))
                            || WifiUtils.configuredWifiPasswordIsWrong(configuredWifiConfiguration)) {
                        isNeedUpdateWifiConfiguration = true;
                    }

                    if (isNeedUpdateWifiConfiguration) {

                        if (WifiUtils.isNeedPassword(scanResult)) {
                            showPwdDialog(null, configuredWifiConfiguration, newEncryptionScheme);
                        } else {
                            ConnectToWifiTask connectToWifiTask = new ConnectToWifiTask(configuredWifiConfiguration,
                                    true, null,
                                    WifiEncryptionScheme.ENCRYPTION_SCHEME_NONE,
                                    EasyWifi.TIME_OUT_CONNECT_TO_WIFI_DEFAULT,
                                    mOnConnectToWifiCallback);
                            EasyWifi.executeTask(connectToWifiTask);
                        }

                    } else {
                        ConnectToWifiTask connectToWifiTask = new ConnectToWifiTask(configuredWifiConfiguration,
                                EasyWifi.TIME_OUT_CONNECT_TO_WIFI_DEFAULT,
                                mOnConnectToWifiCallback);
                        EasyWifi.executeTask(connectToWifiTask);
                    }

                } else {
                    if (WifiUtils.isNeedPassword(scanResult)) {
                        showPwdDialog(scanResult, null, null);
                    } else {
                        ConnectToWifiTask connectToWifiTask = new ConnectToWifiTask(scanResult,
                                EasyWifi.TIME_OUT_CONNECT_TO_WIFI_DEFAULT,
                                mOnConnectToWifiCallback);
                        EasyWifi.executeTask(connectToWifiTask);
                    }

                }

            } else {
                ToastUtil.showShort(MainActivity.this, "无需再次连接" + wifiTask.getSsid());
            }
        }

        @Override
        public void onTaskFail(CheckIsAlreadyConnectedTask wifiTask) {
            int failReason = wifiTask.getFailReason();
            Logger.d(TAG, "onTaskFail failReason=" + failReason);
            setPbVisible(false);

            if (failReason == EasyWifi.FAIL_REASON_LOCATION_MODULE_NOT_EXIST) {
                ToastUtil.showShort(MainActivity.this, "判断是否早已连接失败，您的设备没有定位服务");
            } else if (failReason == EasyWifi.FAIL_REASON_LOCATION_MODULE_DISABLE) {
                ToastUtil.showShort(MainActivity.this, "判断是否早已连接失败，定位服务没有开启");
            } else if (failReason == EasyWifi.FAIL_REASON_NOT_HAS_LOCATION_PERMISSIONS) {
                ToastUtil.showShort(MainActivity.this, "判断是否早已连接失败，没有开启定位服务的权限");
            }
        }

    };

    private void showPwdDialog(final ScanResult scanResult, final WifiConfiguration configuredWifiConfiguration, final String encryptionScheme) {
        final EditText editText = new EditText(MainActivity.this);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this).setTitle(String.format("请输入%s的密码",
                scanResult != null ? scanResult.SSID : configuredWifiConfiguration.SSID)).setView(editText)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String password = editText.getText().toString().trim();

                        if (configuredWifiConfiguration != null) {

                            ConnectToWifiTask connectToWifiTask = new ConnectToWifiTask(configuredWifiConfiguration,
                                    true, password,
                                    encryptionScheme,
                                    EasyWifi.TIME_OUT_CONNECT_TO_WIFI_DEFAULT,
                                    mOnConnectToWifiCallback);
                            EasyWifi.executeTask(connectToWifiTask);

                        } else {
                            ConnectToWifiTask connectToWifiTask = new ConnectToWifiTask(
                                    scanResult,
                                    password,
                                    EasyWifi.TIME_OUT_CONNECT_TO_WIFI_DEFAULT,
                                    mOnConnectToWifiCallback);
                            EasyWifi.executeTask(connectToWifiTask);
                        }

                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        setPbVisible(false);
                    }
                });
        builder.create().show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTvConnectionInfo = findViewById(R.id.tv_connection_info);
        mLvWifiList = findViewById(R.id.lv_wifi_list);
        mPbWait = findViewById(R.id.pb_wait);

        mWifiListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mWifiSsidList);
        mLvWifiList.setAdapter(mWifiListAdapter);
        mLvWifiList.setOnItemClickListener(this);

        updateConnectionInfo();

        resumeTaskIfNeed(savedInstanceState);

    }

    private void resumeTaskIfNeed(Bundle savedInstanceState) {
        if (savedInstanceState != null) {

            ArrayList<WifiTask> taskList = savedInstanceState.getParcelableArrayList(SAVE_INSTANCE_STATE_KEY_WIFI_TASK);

            for (WifiTask wifiTask : taskList) {

                if (wifiTask instanceof GetWifiPermissionTask) {
                    wifiTask.setWifiTaskCallback(mGetWifiPermissionTaskCallback);
                } else if (wifiTask instanceof SetWifiEnabledTask) {
                    wifiTask.setWifiTaskCallback(mOnSetWifiEnabledCallback);
                } else if (wifiTask instanceof ScanWifiTask) {
                    wifiTask.setWifiTaskCallback(mOnScanWifiCallback);
                } else if (wifiTask instanceof ConnectToWifiTask) {
                    wifiTask.setWifiTaskCallback(mOnConnectToWifiCallback);
                } else if (wifiTask instanceof CheckIsAlreadyConnectedTask) {
                    wifiTask.setWifiTaskCallback(mOnCheckIsAlreadyConnectedCallback);
                } else if (wifiTask instanceof GetConnectionInfoTask) {
                    wifiTask.setWifiTaskCallback(mOnGetConnectionInfoCallback);
                }
                Logger.d(TAG, "resume WifiTask=" + wifiTask);
                EasyWifi.executeTask(wifiTask);
            }
        }
    }

    public void openWifi(View view) {
        setWifiEnabled(true);
    }

    public void closeWifi(View view) {
        setWifiEnabled(false);
    }


    private void setWifiEnabled(final boolean enabled) {
        SetWifiEnabledTask setWifiEnabledTask = new SetWifiEnabledTask(enabled,
                EasyWifi.TIME_OUT_SET_WIFI_ENABLED_DEFAULT,
                mOnSetWifiEnabledCallback);
        EasyWifi.executeTask(setWifiEnabledTask);
    }


    public void scanWifi(View view) {
        ScanWifiTask scanWifiTask = new ScanWifiTask(EasyWifi.TIME_OUT_SCAN_WIFI_DEFAULT,
                EasyWifi.SCAN_WIFI_WAY_INITIATIVE,
                true,
                mOnScanWifiCallback);

        EasyWifi.executeTask(scanWifiTask);
    }

    private void updateWifiSsidListFromScanResults(List<ScanResult> scanResults) {
        mScanResults = scanResults;
        mWifiSsidList.clear();
        for (ScanResult scanResult : mScanResults) {
            mWifiSsidList.add(scanResult.SSID + " " + scanResult.BSSID);
        }
        mWifiListAdapter.notifyDataSetChanged();
    }

    private void updateConnectionInfo() {
        GetConnectionInfoTask getConnectionInfoTask = new GetConnectionInfoTask(mOnGetConnectionInfoCallback);
        EasyWifi.executeTask(getConnectionInfoTask);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        ScanResult scanResult = mScanResults.get(position);

        CheckIsAlreadyConnectedTask checkIsAlreadyConnectedTask = new CheckIsAlreadyConnectedTask(scanResult, mOnCheckIsAlreadyConnectedCallback);
        EasyWifi.executeTask(checkIsAlreadyConnectedTask);
    }


    private void setPbVisible(boolean visible) {
        mPbWait.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVE_INSTANCE_STATE_KEY_WIFI_TASK, EasyWifi.getCurrentTasks());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EasyWifi.cancelAllTasks();
    }
}
