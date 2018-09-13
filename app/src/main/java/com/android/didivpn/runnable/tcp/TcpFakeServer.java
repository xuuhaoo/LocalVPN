package com.android.didivpn.runnable.tcp;

import android.net.VpnService;

import com.android.didivpn.session.NetSession;
import com.android.didivpn.session.NetSessionManager;
import com.android.didivpn.tunnel.TcpTunnel;
import com.android.didivpn.tunnel.Tunnel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Created by didi on 2018/5/30.
 */

public class TcpFakeServer implements Runnable {

    private VpnService mVpnService;

    private Selector mSelector;

    private ServerSocketChannel mServerSocketChannel;

    private int mServerPort = 0;

    public TcpFakeServer(VpnService vpnService) throws IOException {
        mVpnService = vpnService;
        mSelector = Selector.open();
        mServerSocketChannel = ServerSocketChannel.open();
        mServerSocketChannel.configureBlocking(false);
        mServerSocketChannel.socket().bind(new InetSocketAddress(0));//随机分配本地端口
        mServerSocketChannel.register(mSelector, SelectionKey.OP_ACCEPT);
        mServerPort = mServerSocketChannel.socket().getLocalPort();//获取分配到的本地端口
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                if (mSelector.select() > 0) {
                    Iterator<SelectionKey> it = mSelector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove();
                        if (key.isValid()) {
                            try {
                                if (key.isAcceptable()) {
                                    acceptChannel(mServerSocketChannel);
                                } else if (key.isConnectable()) {
                                    Tunnel tunnel = getTunnel(key);
                                    tunnel.onConnectable();
                                } else if (key.isReadable()) {
                                    Tunnel tunnel = getTunnel(key);
                                    tunnel.onReadable(key);
                                } else if (key.isWritable()) {
                                    Tunnel tunnel = getTunnel(key);
                                    tunnel.onWritable(key);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeLocalServer();
            }
        }
    }

    /**
     * 代理服务器接收到请求
     *
     * @param serverSocketChannel
     */
    private void acceptChannel(ServerSocketChannel serverSocketChannel) {
        Tunnel localTunnel = new TcpTunnel(mSelector, mVpnService);
        try {
            //获取到本地的管道
            SocketChannel localChannel = serverSocketChannel.accept();
            localTunnel.setChannel(localChannel);
            //获取请求远程的地址:端口
            InetSocketAddress remoteAddr = getDestAddr(localChannel);
            if (remoteAddr != null) {
                //开启远程核心通道
                SocketChannel channel = SocketChannel.open();
                channel.configureBlocking(false);
                //生成与远程的远程管道
                Tunnel remoteTunnel = new TcpTunnel(mSelector, mVpnService);
                remoteTunnel.setChannel(channel);
                remoteTunnel.setDestAddr(remoteAddr);
                localTunnel.bindTunnel(remoteTunnel);
                //连接远程通道
                remoteTunnel.connect();
            } else {
                //没有解析到远端地址
                localTunnel.disconnect();
            }

        } catch (Exception e) {
            e.printStackTrace();
            localTunnel.disconnect();
        }
    }

    /**
     * 获取到目的地地址从socket信道中
     *
     * @param socketChannel
     * @return
     */
    private InetSocketAddress getDestAddr(SocketChannel socketChannel) {
        short port = (short) socketChannel.socket().getPort();
        NetSession netSession = NetSessionManager.getIns().getByPort(port);
        if (netSession != null) {
            return new InetSocketAddress(socketChannel.socket().getInetAddress(), netSession.getRemotePort());
        }
        return null;
    }

    private Tunnel getTunnel(SelectionKey key) {
        if (key != null && key.isValid()) {
            return (Tunnel) key.attachment();
        }
        return null;
    }

    /**
     * 关闭代理服务器
     */
    private void closeLocalServer() {
        if (mSelector != null) {
            try {
                mSelector.close();
            } catch (IOException e) {
            } finally {
                mSelector = null;
            }
        }

        if (mServerSocketChannel != null) {
            try {
                mServerSocketChannel.close();
            } catch (IOException e) {
            } finally {
                mServerSocketChannel = null;
            }
        }
    }

    /**
     * 获取代理服务器端口号
     *
     * @return
     */
    public int getServerPort() {
        return mServerPort;
    }


}
