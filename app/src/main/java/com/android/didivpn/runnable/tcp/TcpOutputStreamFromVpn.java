package com.android.didivpn.runnable.tcp;

import com.android.didivpn.packet.TCP;
import com.android.didivpn.packet.ip.IPv4;
import com.android.didivpn.utils.QueueHelper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Created by didi on 2018/5/30.
 */

public class TcpOutputStreamFromVpn implements Runnable {

    private Selector mSelector;

    public TcpOutputStreamFromVpn() {
        try {
            mSelector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            //获取待发送IP包
            IPv4 iPv4 = null;
            try {
                iPv4 = QueueHelper.getIns().takeTcpToNet();
            } catch (InterruptedException e) {
                //ignore
            }

            if (iPv4 == null) {
                continue;
            }

            //获取目的地IP地址
            InetAddress address = null;
            try {
                address = iPv4.getDestAddressAsInetAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            if (address == null) {
                continue;
            }

            SocketChannel channel = SocketChannel.open(address);

            TCP tcp = new TCP(iPv4);
            byte[] bytes = tcp.getDataBytes();
            if (bytes.length == 0) {
                continue;
            }

            ByteBuffer tcpPayload = ByteBuffer.allocate(bytes.length);

        }
    }
}
