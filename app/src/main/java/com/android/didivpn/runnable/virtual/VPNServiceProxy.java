package com.android.didivpn.runnable.virtual;

import android.os.ParcelFileDescriptor;

import com.android.didivpn.packet.TCP;
import com.android.didivpn.packet.UDP;
import com.android.didivpn.packet.ip.IPv4;
import com.android.didivpn.utils.ByteBufferPool;
import com.android.didivpn.utils.LogUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 虚拟网卡的读入,应用发送的请求会先被读到这里
 * Created by didi on 2018/5/30.
 */

public class VPNServiceProxy implements Runnable {

    private ParcelFileDescriptor mParcelFileDescriptor;

    public VPNServiceProxy(ParcelFileDescriptor parcelFileDescriptor) {
        mParcelFileDescriptor = parcelFileDescriptor;
    }

    @Override
    public void run() {
        FileChannel inputChannel = new FileInputStream(mParcelFileDescriptor.getFileDescriptor()).getChannel();
        FileChannel outputChannel = new FileOutputStream(mParcelFileDescriptor.getFileDescriptor()).getChannel();

        while (!Thread.interrupted()) {
            ByteBuffer byteBuffer = ByteBufferPool.acquire();

            try {
                int bytesNum = inputChannel.read(byteBuffer);
                if (bytesNum > 0) {
                    byteBuffer.flip();
                    IPv4 iPv4 = new IPv4(byteBuffer);
                    if (TCP.isTCP(iPv4.getProtocol())) {

                    } else if (UDP.isUDP(iPv4.getProtocol())) {
                        //TODO: 待完成
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
