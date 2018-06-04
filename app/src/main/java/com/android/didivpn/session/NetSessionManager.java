package com.android.didivpn.session;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.SparseArray;

/**
 * Created by didi on 2018/5/31.
 */

public class NetSessionManager {

    private static final int MAX_SESSION_COUNT = 10000;

    private static final long SESSION_TIMEOUT_NS = 120 * 1000000000L;

    private static final long CYCLE_CHECK = SESSION_TIMEOUT_NS * 2;

    private SparseArray<NetSession> mSparseArray = new SparseArray<>(MAX_SESSION_COUNT);

    private Handler mCheckHandler;

    private HandlerThread mHandlerThread = new HandlerThread("check_timeout");

    private static class InstanceHolder {
        private static NetSessionManager This = new NetSessionManager();
    }

    private NetSessionManager() {
        mHandlerThread.start();
        mCheckHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                gc(true);
            }
        };

    }

    public static NetSessionManager getIns() {
        return InstanceHolder.This;
    }

    public NetSession create(short localPort, int destinationIP, short destinationPort, String destinationHost) {
        gc(false);
        NetSession netSession = new NetSession();
        netSession.setRemoteIP(destinationIP);
        netSession.setRemotePort(destinationPort);
        netSession.setRemoteHost(destinationHost);
        netSession.setLastUpdateNs(System.nanoTime());
        mSparseArray.put(localPort, netSession);

        if (mSparseArray.size() == 1) {
            mCheckHandler.sendEmptyMessageDelayed(0, CYCLE_CHECK);
        }
        return netSession;
    }

    public NetSession getByPort(short localPort) {
        return mSparseArray.get(localPort);
    }

    public void clear() {
        mSparseArray.clear();
        mCheckHandler.removeCallbacksAndMessages(null);
    }

    public void gc(Boolean force) {
        if (mSparseArray.size() >= MAX_SESSION_COUNT / 2 || force) {
            long now = System.nanoTime();
            for (int i = mSparseArray.size() - 1; i >= 0; i--) {
                NetSession session = mSparseArray.valueAt(i);
                if (now - session.getLastUpdateNs() >= SESSION_TIMEOUT_NS) {
                    mSparseArray.removeAt(i);
                }
            }
        }
    }

}
