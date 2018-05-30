/*
** Copyright 2015, Mohamed Naufal
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package com.android.didivpn;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.didivpn.runnable.virtual.VirtualRead;
import com.android.didivpn.runnable.virtual.VirtualWrite;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class LocalVPNService extends VpnService {
    private static final String TAG = LocalVPNService.class.getSimpleName();
    private static final String VPN_ADDRESS = "10.0.0.2"; // Only IPv4 support for now
    private static final String VPN_ROUTE = "0.0.0.0"; // Intercept everything

    public static final String BROADCAST_VPN_STATE = "xyz.hexene.localvpn.VPN_STATE";

    private static boolean isRunning = false;

    private ParcelFileDescriptor mParcelFileDescriptor = null;

    private ExecutorService threadPool;

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        initVPN();
        threadPool = Executors.newCachedThreadPool();
        threadPool.submit(new VirtualRead(mParcelFileDescriptor));
        threadPool.submit(new VirtualWrite(mParcelFileDescriptor,this));

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_VPN_STATE).putExtra("running", true));
        Log.i(TAG, "Started");
    }

    private void initVPN() {
        if (mParcelFileDescriptor == null) {
            Builder builder = new Builder();
            builder.addAddress(VPN_ADDRESS, 32);
            builder.addRoute(VPN_ROUTE, 0);
            builder.setSession(getString(R.string.app_name));
            mParcelFileDescriptor = builder.establish();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public static boolean isRunning() {
        return isRunning;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        threadPool.shutdownNow();
        Log.i(TAG, "Stopped");
    }

}
