package com.android.didivpn.utils;

import android.util.Log;

import com.android.didivpn.packet.Packet;


/**
 * Created by didi on 2018/5/30.
 */

public class LogUtils {
    public static final String TAG = "VPN_DIDI";

    public static void log(Packet packet) {
        Log.i(TAG, packet.toString());
    }

    public static void logPayload(Packet packet) {
        byte[] payload = packet.getDataBytes();
        if (payload.length == 0) {
            Log.i(TAG, "payload is empty!");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < payload.length; i++) {
            sb.append((char) payload[i]);
        }

        Log.i(TAG, "payload: " + sb.toString());
    }
}
