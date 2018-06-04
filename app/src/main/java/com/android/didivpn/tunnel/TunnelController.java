package com.android.didivpn.tunnel;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by didi on 2018/5/31.
 */

public interface TunnelController {
    void connect() throws IOException;

    void disconnect();

    /**
     * 写入数据到核心通道中
     *
     * @param buffer 数据源
     * @return true表示数据全部写完, false表示还有剩余数据要等下次Writable写入
     */
    boolean writeToCoreChannel(ByteBuffer buffer) throws IOException;

    void bindTunnel(Tunnel tunnel);

}
