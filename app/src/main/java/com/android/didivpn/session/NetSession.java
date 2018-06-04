package com.android.didivpn.session;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Created by didi on 2018/5/31.
 */

public class NetSession {
    //=============================真实远端数据===============================
    /**
     * 真实的远端服务器地址
     */
    private int mRemoteIP;
    /**
     * 真实远端服务器端口
     */
    private short mRemotePort;
    /**
     * 真实的远端服务器Host地址
     */
    private String mRemoteHost;
    /**
     * 最后更新的纳秒数
     */
    private long mLastUpdateNs;

    NetSession() {

    }

    public long getLastUpdateNs() {
        return mLastUpdateNs;
    }

    public NetSession setLastUpdateNs(long lastUpdateNs) {
        mLastUpdateNs = lastUpdateNs;
        return this;
    }

    public int getRemoteIP() {
        return mRemoteIP;
    }

    public NetSession setRemoteIP(int remoteIP) {
        mRemoteIP = remoteIP;
        return this;
    }

    public short getRemotePort() {
        return mRemotePort;
    }

    public NetSession setRemotePort(short remotePort) {
        mRemotePort = remotePort;
        return this;
    }

    public String getRemoteHost() {
        return mRemoteHost;
    }

    public NetSession setRemoteHost(String remoteHost) {
        mRemoteHost = remoteHost;
        return this;
    }
}
