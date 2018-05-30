package com.android.didivpn.runnable;

import android.os.ParcelFileDescriptor;

import com.android.didivpn.packet.TCP;
import com.android.didivpn.packet.UDP;
import com.android.didivpn.packet.ip.IPv4;
import com.android.didivpn.utils.QueueHelper;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by didi on 2018/5/30.
 */

public class VirtualWrite implements Runnable {

    private ParcelFileDescriptor mParcelFileDescriptor;

    public VirtualWrite(ParcelFileDescriptor parcelFileDescriptor) {
        mParcelFileDescriptor = parcelFileDescriptor;
    }

    @Override
    public void run() {
        FileChannel channel = new FileInputStream(mParcelFileDescriptor.getFileDescriptor()).getChannel();

        while (!Thread.interrupted()) {
            IPv4 iPv4 = null;
            try {
                iPv4 = QueueHelper.getIns().takeTcpToVpn();
            } catch (InterruptedException e) {
                //ignore
            }

            if (iPv4 == null) {
                continue;
            }
            try {
                ByteBuffer byteBuffer = iPv4.getByteBuffer();
                if (byteBuffer == null) {
                    continue;
                }
                while (byteBuffer.hasRemaining()) {
                    channel.write(byteBuffer);
                }
                byteBuffer.clear();
                byteBuffer = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
