/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.emm.system.service;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

public class MainActivity extends Activity {
    private static final int ACTIVATION_REQUEST = 47;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ComponentName receiver = new ComponentName(this, ServiceDeviceAdminReceiver.class);
        startDeviceAdminPrompt(receiver);
    }

    /**
     * Start device admin activation request.
     *
     * @param cdmDeviceAdmin - Device admin component.
     */
    private void startDeviceAdminPrompt(ComponentName cdmDeviceAdmin) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (!devicePolicyManager.isAdminActive(cdmDeviceAdmin)) {
            Intent deviceAdminIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            deviceAdminIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cdmDeviceAdmin);
            deviceAdminIntent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                       getResources().getString(R.string.device_admin_enable_alert));
            startActivityForResult(deviceAdminIntent, ACTIVATION_REQUEST);
        } else {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVATION_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                finish();
                Log.i("onActivityResult", "Administration enabled!");
            } else {
                Log.i("onActivityResult", "Administration enable FAILED!");
            }
        }
    }

    public class DeviceBootReceiver extends BroadcastReceiver {
        private static final String TAG = "DeviceBootReceiver";

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onReceive(final Context context, Intent intent) {
            Log.i(TAG, "disableAdminPopup" + " for Applications");
            DevicePolicyManager manager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

            ComponentName componentAgent = new ComponentName("org.wso2.emm.agent", "org.wso2.emm.agent.services.AgentDeviceAdminReceiver");
            if(!manager.isAdminActive(componentAgent))
                manager.setActiveAdmin(componentAgent, true);

            ComponentName componentService = new ComponentName("org.wso2.emm.system.service", "org.wso2.emm.system.service.ServiceDeviceAdminReceiver");
            if(!manager.isAdminActive(componentService))
                manager.setActiveAdmin(componentService, true);

            if (!manager.isDeviceOwnerApp("org.wso2.emm.system.service")){
                Log.i(TAG, "isDeviceOwnerApp" + " setting owner");
                try {
                    Runtime.getRuntime().exec("dpm set-device-owner org.wso2.emm.system.service/.ServiceDeviceAdminReceiver");
                } catch  (IOException e) {
                    Log.e(TAG, "Shell command execution failed." + e);
                }
            }
            else {
                Log.i(TAG, "isDeviceOwnerApp" + " already owner");
            }
        }
    }

}
