///*
//** Copyright 2015, Mohamed Naufal
//**
//** Licensed under the Apache License, Version 2.0 (the "License");
//** you may not use this file except in compliance with the License.
//** You may obtain a copy of the License at
//**
//**     http://www.apache.org/licenses/LICENSE-2.0
//**
//** Unless required by applicable law or agreed to in writing, software
//** distributed under the License is distributed on an "AS IS" BASIS,
//** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//** See the License for the specific language governing permissions and
//** limitations under the License.
//*/
//
//package com.android.didivpn;
//
//import android.util.Log;
//
//import java.io.IOException;
//import java.net.InetAddress;
//import java.net.InetSocketAddress;
//import java.nio.ByteBuffer;
//import java.nio.channels.DatagramChannel;
//import java.nio.channels.SelectionKey;
//import java.nio.channels.Selector;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.concurrent.ConcurrentLinkedQueue;
//
//import com.android.didivpn.utils.LRUCache;
//import com.android.didivpn.utils.PacketHelper;
//
//public class UDPOutput implements Runnable
//{
//    private static final String TAG = UDPOutput.class.getSimpleName();
//
//    private LocalVPNService vpnService;
//    private ConcurrentLinkedQueue<PacketHelper> inputQueue;
//    private Selector selector;
//
//    private static final int MAX_CACHE_SIZE = 50;
//    private LRUCache<String, DatagramChannel> channelCache =
//            new LRUCache<>(MAX_CACHE_SIZE, new LRUCache.CleanupCallback<String, DatagramChannel>()
//            {
//                @Override
//                public void cleanup(Map.Entry<String, DatagramChannel> eldest)
//                {
//                    closeChannel(eldest.getValue());
//                }
//            });
//
//    public UDPOutput(ConcurrentLinkedQueue<PacketHelper> inputQueue, Selector selector, LocalVPNService vpnService)
//    {
//        this.inputQueue = inputQueue;
//        this.selector = selector;
//        this.vpnService = vpnService;
//    }
//
//    @Override
//    public void run()
//    {
//        Log.i(TAG, "Started");
//        try
//        {
//
//            Thread currentThread = Thread.currentThread();
//            while (true)
//            {
//                PacketHelper currentPacketHelper;
//                // TODO: Block when not connected
//                do
//                {
//                    currentPacketHelper = inputQueue.poll();
//                    if (currentPacketHelper != null)
//                        break;
//                    Thread.sleep(10);
//                } while (!currentThread.isInterrupted());
//
//                if (currentThread.isInterrupted())
//                    break;
//
//                InetAddress destinationAddress = currentPacketHelper.ip4.destinationAddress;
//                int destinationPort = currentPacketHelper.udp.destinationPort;
//                int sourcePort = currentPacketHelper.udp.sourcePort;
//
//                String ipAndPort = destinationAddress.getHostAddress() + ":" + destinationPort + ":" + sourcePort;
//                DatagramChannel outputChannel = channelCache.get(ipAndPort);
//                if (outputChannel == null) {
//                    outputChannel = DatagramChannel.open();
//                    vpnService.protect(outputChannel.socket());
//                    try
//                    {
//                        outputChannel.connect(new InetSocketAddress(destinationAddress, destinationPort));
//                    }
//                    catch (IOException e)
//                    {
//                        Log.e(TAG, "Connection error: " + ipAndPort, e);
//                        closeChannel(outputChannel);
//                        ByteBufferPool.release(currentPacketHelper.backingBuffer);
//                        continue;
//                    }
//                    outputChannel.configureBlocking(false);
//                    currentPacketHelper.swapSourceAndDestination();
//
//                    selector.wakeup();
//                    outputChannel.register(selector, SelectionKey.OP_READ, currentPacketHelper);
//
//                    channelCache.put(ipAndPort, outputChannel);
//                }
//
//                try
//                {
//                    ByteBuffer payloadBuffer = currentPacketHelper.backingBuffer;
//                    while (payloadBuffer.hasRemaining())
//                        outputChannel.write(payloadBuffer);
//                }
//                catch (IOException e)
//                {
//                    Log.e(TAG, "Network write error: " + ipAndPort, e);
//                    channelCache.remove(ipAndPort);
//                    closeChannel(outputChannel);
//                }
//                ByteBufferPool.release(currentPacketHelper.backingBuffer);
//            }
//        }
//        catch (InterruptedException e)
//        {
//            Log.i(TAG, "Stopping");
//        }
//        catch (IOException e)
//        {
//            Log.i(TAG, e.toString(), e);
//        }
//        finally
//        {
//            closeAll();
//        }
//    }
//
//    private void closeAll()
//    {
//        Iterator<Map.Entry<String, DatagramChannel>> it = channelCache.entrySet().iterator();
//        while (it.hasNext())
//        {
//            closeChannel(it.next().getValue());
//            it.remove();
//        }
//    }
//
//    private void closeChannel(DatagramChannel channel)
//    {
//        try
//        {
//            channel.close();
//        }
//        catch (IOException e)
//        {
//            // Ignore
//        }
//    }
//}
