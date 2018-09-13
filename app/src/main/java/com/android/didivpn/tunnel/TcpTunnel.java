package com.android.didivpn.tunnel;

import android.net.VpnService;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * Created by didi on 2018/5/31.
 */

public class TcpTunnel extends Tunnel {

    public TcpTunnel(Selector selector, VpnService service) {
        super(selector, service);
    }


    @Override
    public ByteBuffer onAfterReceived(byte[] byteBuffer) {
        return null;
    }

    @Override
    public ByteBuffer onBeforeSend(byte[] byteBuffer) {
        return null;
    }

    @Override
    public void onDisconnect() {

    }

}
