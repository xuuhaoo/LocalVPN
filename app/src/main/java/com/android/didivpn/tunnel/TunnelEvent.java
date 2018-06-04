package com.android.didivpn.tunnel;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;

/**
 * Created by didi on 2018/5/31.
 */

public interface TunnelEvent {
    /**
     * 收到数据后的拦截器
     *
     * @param byteBuffer 数据源
     * @return 修改后的数据, 如果为Null, 则表明没有修改
     */
    ByteBuffer onAfterReceived(byte[] byteBuffer);

    /**
     * 发送数据前的拦截器
     *
     * @param byteBuffer 数据源
     * @return 修改后的数据, 如果为Null, 则表示数据没有修改
     */
    ByteBuffer onBeforeSend(byte[] byteBuffer);

    void onConnected() throws Exception;

    void onDisconnect();
}
