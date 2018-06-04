package com.android.didivpn.tunnel;

import android.net.VpnService;
import android.support.annotation.CallSuper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Created by didi on 2018/5/31.
 */

public abstract class Tunnel implements TunnelController, TunnelEvent {

    protected VpnService mDIDIVPNService;
    /**
     * 管道核心通道
     */
    protected SocketChannel mChannel;
    /**
     * 目标服务器地址信息
     */
    protected InetSocketAddress mDestAddr;
    /**
     * 选择器
     */
    protected Selector mSelector;
    /**
     * 对称通道
     * 如果核心通道是与本地的通道,那么这个对称通道将是和服务器的通道
     * <p>
     * 如果核心通道是远程通道,那么这个对称通道将会是和本地的通道.
     */
    protected Tunnel mMappingTunnel;
    /**
     * 是否已经结束
     */
    private boolean isDisconnect;
    /**
     * 核心通道一次没有读取完成
     */
    private ByteBuffer mSendRemainingBuf;

    public Tunnel(Selector selector, VpnService service) {
        mSelector = selector;
        mDIDIVPNService = service;
    }

    /**
     * 进行核心管道链接
     *
     * @throws IOException
     */
    @Override
    public void connect() throws IOException {
        if (mDestAddr == null) {
            return;
        }
        if (mChannel == null) {
            mChannel = SocketChannel.open();
            mChannel.configureBlocking(false);
        }
        if (mDIDIVPNService.protect(mChannel.socket())) {
            mChannel.connect(mDestAddr);
            mChannel.register(mSelector, SelectionKey.OP_CONNECT, this);
        } else {
            throw new IOException("VPN protect socket failed.");
        }
    }

    /**
     * 全部断开连接,包括核心管道和映射管道
     */
    @Override
    public void disconnect() {
        if (isDisconnect) {
            return;
        }
        isDisconnect = true;
        if (mChannel != null) {
            try {
                mChannel.close();
            } catch (IOException e) {
            }
        }

        if (mMappingTunnel != null) {
            mMappingTunnel.mMappingTunnel.disconnect();
        }

        if (mSendRemainingBuf != null) {
            mSendRemainingBuf.clear();
            mSendRemainingBuf = null;
        }

        onDisconnect();
    }

    /**
     * 写入数据到核心通道中
     *
     * @param buffer 数据源
     * @return true表示数据全部写完, false表示还有剩余数据要等下次Writable写入
     */
    @Override
    public boolean writeToCoreChannel(ByteBuffer buffer) throws IOException {
        if (buffer == null || !buffer.hasRemaining()) {
            return true;
        }
        while (buffer.hasRemaining()) {
            int bytesSent = mChannel.write(buffer);
            //数据未发送
            if (bytesSent == 0) {
                break;
            }
        }
        if (buffer.hasRemaining()) {
            if (mSendRemainingBuf != null) {
                mSendRemainingBuf.clear();
            }
            mSendRemainingBuf = ByteBuffer.allocate(buffer.remaining());
            mSendRemainingBuf.put(buffer);
            mSendRemainingBuf.flip();
            mChannel.register(mSelector, SelectionKey.OP_WRITE, this);
            return false;
        } else {
            return true;
        }
    }

    /**
     * 绑定映射管道
     *
     * @param mappingTunnel 需要绑定的映射管道
     */
    @Override
    public void bindTunnel(Tunnel mappingTunnel) {
        mMappingTunnel = mappingTunnel;
        mMappingTunnel.setMappingTunnel(this);
    }

    /**
     * 建立稳定的核心链接
     */
    public final void onConnectable() {
        try {
            if (mChannel.finishConnect()) {
                onConnected();
            } else {
                throw new RuntimeException("can't finish");
            }
        } catch (Exception e) {
            e.printStackTrace();
            disconnect();
        }
    }

    /**
     * 通知管道准备读取操作
     *
     * @throws IOException
     */
    private void prepareReadChannel() throws IOException {
        if (mChannel.isBlocking()) {
            mChannel.configureBlocking(false);
        }
        mChannel.register(mSelector, SelectionKey.OP_READ, this);
    }

    @Override
    @CallSuper
    public void onConnected() throws Exception {
        //核心通道准备读取
        prepareReadChannel();
        //映射通道准备读取
        mMappingTunnel.prepareReadChannel();
    }

    public final void onReadable(SelectionKey key) throws Exception {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(20480);
            int bytesRead = mChannel.read(buffer);
            if (bytesRead > 0) {
                //收到后的拦截器,可以对buffer内容进行修改
                ByteBuffer afterModify = onAfterReceived(buffer.array());
                if (afterModify != null) {
                    buffer.clear();
                    buffer = afterModify;
                }
                buffer.flip();
                //如果buffer中还有内容
                if (buffer.hasRemaining()) {
                    afterModify = mMappingTunnel.onBeforeSend(buffer.array());
                    if (afterModify != null) {
                        buffer.clear();
                        buffer = afterModify;
                        buffer.flip();
                    }
                    if (!mMappingTunnel.writeToCoreChannel(buffer)) {
                        //数据没有写完,下次还需要Writable再写,所以要取消下次的读取透传.
                        key.cancel();
                    }
                }
            } else if (bytesRead < 0) {//读取到流末尾
                disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
    }

    /**
     * 该方法的调用仅出现在映射写入未写完,后续还需要片段写.
     *
     * @param key
     * @throws Exception
     */
    public void onWritable(SelectionKey key) throws IOException {
        if (mSendRemainingBuf == null || !mSendRemainingBuf.hasRemaining()) {
            key.cancel();
            //因为仅出现在映射管道,所以仅需要调用映射的映射准备读取就好
            mMappingTunnel.prepareReadChannel();
            return;
        }

        ByteBuffer afterModify = onBeforeSend(mSendRemainingBuf.array());
        if (afterModify != null) {
            mSendRemainingBuf.clear();
            mSendRemainingBuf = afterModify;
            mSendRemainingBuf.flip();
        }

        if (writeToCoreChannel(mSendRemainingBuf)) {
            //这次将剩余的传输完毕
            key.cancel();
            //因为仅出现在映射管道,所以仅需要调用映射的映射准备读取就好
            mMappingTunnel.prepareReadChannel();
        }
    }

    public VpnService getDIDIVPNService() {
        return mDIDIVPNService;
    }

    public void setDIDIVPNService(VpnService DIDIVPNService) {
        mDIDIVPNService = DIDIVPNService;
    }

    public SocketChannel getChannel() {
        return mChannel;
    }

    public void setChannel(SocketChannel channel) {
        mChannel = channel;
    }

    public InetSocketAddress getDestAddr() {
        return mDestAddr;
    }

    public void setDestAddr(InetSocketAddress destAddr) {
        mDestAddr = destAddr;
    }

    public Selector getSelector() {
        return mSelector;
    }

    public void setSelector(Selector selector) {
        mSelector = selector;
    }

    public Tunnel getMappingTunnel() {
        return mMappingTunnel;
    }

    public void setMappingTunnel(Tunnel mappingTunnel) {
        mMappingTunnel = mappingTunnel;
    }
}
