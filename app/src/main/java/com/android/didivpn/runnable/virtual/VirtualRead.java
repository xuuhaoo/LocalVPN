package com.android.didivpn.runnable.virtual;

import android.os.ParcelFileDescriptor;

import com.android.didivpn.packet.TCP;
import com.android.didivpn.packet.UDP;
import com.android.didivpn.packet.ip.IPv4;
import com.android.didivpn.utils.ByteBufferPool;
import com.android.didivpn.utils.LogUtils;
import com.android.didivpn.utils.QueueHelper;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 虚拟网卡的读入,应用发送的请求会先被读到这里
 * Created by didi on 2018/5/30.
 */

public class VirtualRead implements Runnable {

    private ParcelFileDescriptor mParcelFileDescriptor;

    public VirtualRead(ParcelFileDescriptor parcelFileDescriptor) {
        mParcelFileDescriptor = parcelFileDescriptor;
    }

    @Override
    public void run() {
        FileChannel channel = new FileInputStream(mParcelFileDescriptor.getFileDescriptor()).getChannel();

        while (!Thread.interrupted()) {
            ByteBuffer byteBuffer = ByteBufferPool.acquire();

            try {
                int bytesNum = channel.read(byteBuffer);
                if (bytesNum > 0) {
                    byteBuffer.flip();
                    IPv4 iPv4 = new IPv4(byteBuffer);
                    if (TCP.isTCP(iPv4.getProtocol())) {
                        QueueHelper.getIns().offerTcpToNet(iPv4);
//                        LogUtils.log(new TCP(iPv4));
                        LogUtils.logPayload(new TCP(iPv4));
                    } else if (UDP.isUDP(iPv4.getProtocol())) {
                        QueueHelper.getIns().offerUdpToNet(iPv4);
//                        LogUtils.log(new UDP(iPv4));
                        LogUtils.logPayload(new UDP(iPv4));
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
