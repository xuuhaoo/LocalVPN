package com.android.didivpn.utils;

import com.android.didivpn.packet.ip.IPv4;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by didi on 2018/5/30.
 */

public class QueueHelper {

    private static class InstanceHolder {
        private static QueueHelper INSTANCE = new QueueHelper();
    }

    public static QueueHelper getIns() {
        return InstanceHolder.INSTANCE;
    }

    private static LinkedBlockingQueue<IPv4> mTcpVpnToNetQueue = new LinkedBlockingQueue<>();

    private static LinkedBlockingQueue<IPv4> mUdpVpnToNetQueue = new LinkedBlockingQueue<>();

    private static LinkedBlockingQueue<IPv4> mTcpNetToVpnQueue = new LinkedBlockingQueue<>();


    public void offerTcpToNet(IPv4 iPv4) {
        mTcpVpnToNetQueue.offer(iPv4);
    }

    public void offerUdpToNet(IPv4 iPv4) {
        mUdpVpnToNetQueue.offer(iPv4);
    }

    public void offerTcpToVpn(IPv4 iPv4) {
        mTcpNetToVpnQueue.offer(iPv4);
    }

    public IPv4 takeTcpToNet() throws InterruptedException {
        return mTcpVpnToNetQueue.take();
    }

    public IPv4 takeUdpToNet() throws InterruptedException {
        return mUdpVpnToNetQueue.take();
    }

    public IPv4 takeTcpToVpn() throws InterruptedException {
        return mTcpNetToVpnQueue.take();
    }


}
