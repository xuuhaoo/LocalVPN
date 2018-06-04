package com.android.didivpn.tunnel;

import android.net.VpnService;

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
    public void onWriteable(SelectionKey key) throws Exception {

    }

    @Override
    public void onDisconnect() {

    }

}
