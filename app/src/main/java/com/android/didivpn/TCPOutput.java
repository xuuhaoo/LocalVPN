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
//import java.nio.channels.SelectionKey;
//import java.nio.channels.Selector;
//import java.nio.channels.SocketChannel;
//import java.util.Random;
//import java.util.concurrent.ConcurrentLinkedQueue;
//
//import com.android.didivpn.utils.ByteBufferPool;
//import com.android.didivpn.utils.PacketHelper;
//import com.android.didivpn.utils.PacketHelper.TCPHeader;
//import com.android.didivpn.TCB.Status;
//
//public class TCPOutput implements Runnable {
//    private static final String TAG = TCPOutput.class.getSimpleName();
//
//    private LocalVPNService vpnService;
//    private ConcurrentLinkedQueue<PacketHelper> inputQueue;
//    private ConcurrentLinkedQueue<ByteBuffer> outputQueue;
//    private Selector selector;
//
//    private Random random = new Random();
//
//    public TCPOutput(ConcurrentLinkedQueue<PacketHelper> inputQueue, ConcurrentLinkedQueue<ByteBuffer> outputQueue,
//                     Selector selector, LocalVPNService vpnService) {
//        this.inputQueue = inputQueue;
//        this.outputQueue = outputQueue;
//        this.selector = selector;
//        this.vpnService = vpnService;
//    }
//
//    @Override
//    public void run() {
//        Log.i(TAG, "Started");
//        try {
//
//            Thread currentThread = Thread.currentThread();
//            while (true) {
//                PacketHelper currentPacketHelper;
//                // TODO: Block when not connected
//                do {
//                    currentPacketHelper = inputQueue.poll();
//                    if (currentPacketHelper != null)
//                        break;
//                    Thread.sleep(10);
//                } while (!currentThread.isInterrupted());
//
//                if (currentThread.isInterrupted())
//                    break;
//
//                ByteBuffer payloadBuffer = currentPacketHelper.backingBuffer;
//                currentPacketHelper.backingBuffer = null;
//                ByteBuffer responseBuffer = ByteBufferPool.acquire();
//
//                InetAddress destinationAddress = currentPacketHelper.ip4.destinationAddress;
//
//                TCPHeader tcpHeader = currentPacketHelper.tcp;
//                int destinationPort = tcpHeader.destinationPort;
//                int sourcePort = tcpHeader.sourcePort;
//
//                String ipAndPort = destinationAddress.getHostAddress() + ":" +
//                        destinationPort + ":" + sourcePort;
//                TCB tcb = TCB.getTCB(ipAndPort);
//                if (tcb == null)
//                    initializeConnection(ipAndPort, destinationAddress, destinationPort,
//                            currentPacketHelper, tcpHeader, responseBuffer);
//                else if (tcpHeader.isSYN())
//                    processDuplicateSYN(tcb, tcpHeader, responseBuffer);
//                else if (tcpHeader.isRST())
//                    closeCleanly(tcb, responseBuffer);
//                else if (tcpHeader.isFIN())
//                    processFIN(tcb, tcpHeader, responseBuffer);
//                else if (tcpHeader.isACK())
//                    processACK(tcb, tcpHeader, payloadBuffer, responseBuffer);
//
//                // XXX: cleanup later
//                if (responseBuffer.position() == 0)
//                    ByteBufferPool.release(responseBuffer);
//                ByteBufferPool.release(payloadBuffer);
//            }
//        } catch (InterruptedException e) {
//            Log.i(TAG, "Stopping");
//        } catch (IOException e) {
//            Log.e(TAG, e.toString(), e);
//        } finally {
//            TCB.closeAll();
//        }
//    }
//
//    private void initializeConnection(String ipAndPort, InetAddress destinationAddress, int destinationPort,
//                                      PacketHelper currentPacketHelper, TCPHeader tcpHeader, ByteBuffer responseBuffer)
//            throws IOException {
//        currentPacketHelper.swapSourceAndDestination();
//        if (tcpHeader.isSYN()) {
//            SocketChannel outputChannel = SocketChannel.open();
//            outputChannel.configureBlocking(false);
//            vpnService.protect(outputChannel.socket());
//
//            TCB tcb = new TCB(ipAndPort, random.nextInt(Short.MAX_VALUE + 1), tcpHeader.sequenceNumber, tcpHeader.sequenceNumber + 1,
//                    tcpHeader.acknowledgementNumber, outputChannel, currentPacketHelper);
//            TCB.putTCB(ipAndPort, tcb);
//
//            try {
//                outputChannel.connect(new InetSocketAddress(destinationAddress, destinationPort));
//                if (outputChannel.finishConnect()) {
//                    tcb.status = Status.SYN_RECEIVED;
//                    // TODO: Set MSS for receiving larger packets from the device
//                    currentPacketHelper.updateTCPBuffer(responseBuffer, (byte) (TCPHeader.SYN | TCPHeader.ACK),
//                            tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
//                    tcb.mySequenceNum++; // SYN counts as a byte
//                } else {
//                    tcb.status = Status.SYN_SENT;
//                    selector.wakeup();
//                    tcb.selectionKey = outputChannel.register(selector, SelectionKey.OP_CONNECT, tcb);
//                    return;
//                }
//            } catch (IOException e) {
//
//                Log.e(TAG, "Connection error: " + ipAndPort, e);
//                currentPacketHelper.updateTCPBuffer(responseBuffer, (byte) TCPHeader.RST, 0, tcb.myAcknowledgementNum, 0);
//                TCB.closeTCB(tcb);
//            }
//        } else {
//            currentPacketHelper.updateTCPBuffer(responseBuffer, (byte) TCPHeader.RST,
//                    0, tcpHeader.sequenceNumber + 1, 0);
//        }
//        outputQueue.offer(responseBuffer);
//    }
//
//    private void processDuplicateSYN(TCB tcb, TCPHeader tcpHeader, ByteBuffer responseBuffer) {
//        synchronized (tcb) {
//            if (tcb.status == Status.SYN_SENT) {
//                tcb.myAcknowledgementNum = tcpHeader.sequenceNumber + 1;
//                return;
//            }
//        }
//        sendRST(tcb, 1, responseBuffer);
//    }
//
//    private void processFIN(TCB tcb, TCPHeader tcpHeader, ByteBuffer responseBuffer) {
//        synchronized (tcb) {
//            PacketHelper referencePacketHelper = tcb.mReferencePacketHelper;
//            tcb.myAcknowledgementNum = tcpHeader.sequenceNumber + 1;
//            tcb.theirAcknowledgementNum = tcpHeader.acknowledgementNumber;
//
//            if (tcb.waitingForNetworkData) {
//                tcb.status = Status.CLOSE_WAIT;
//                referencePacketHelper.updateTCPBuffer(responseBuffer, (byte) TCPHeader.ACK,
//                        tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
//            } else {
//                tcb.status = Status.LAST_ACK;
//                referencePacketHelper.updateTCPBuffer(responseBuffer, (byte) (TCPHeader.FIN | TCPHeader.ACK),
//                        tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
//                tcb.mySequenceNum++; // FIN counts as a byte
//            }
//        }
//        outputQueue.offer(responseBuffer);
//    }
//
//    private void processACK(TCB tcb, TCPHeader tcpHeader, ByteBuffer payloadBuffer, ByteBuffer responseBuffer) throws IOException {
//        int payloadSize = payloadBuffer.limit() - payloadBuffer.position();
//
//        synchronized (tcb) {
//            SocketChannel outputChannel = tcb.channel;
//            if (tcb.status == Status.SYN_RECEIVED) {
//                tcb.status = Status.ESTABLISHED;
//
//                selector.wakeup();
//                tcb.selectionKey = outputChannel.register(selector, SelectionKey.OP_READ, tcb);
//                tcb.waitingForNetworkData = true;
//            } else if (tcb.status == Status.LAST_ACK) {
//                closeCleanly(tcb, responseBuffer);
//                return;
//            }
//
//            if (payloadSize == 0) return; // Empty ACK, ignore
//
//            if (!tcb.waitingForNetworkData) {
//                selector.wakeup();
//                tcb.selectionKey.interestOps(SelectionKey.OP_READ);
//                tcb.waitingForNetworkData = true;
//            }
//
//            // Forward to remote server
//            try {
//                while (payloadBuffer.hasRemaining())
//                    outputChannel.write(payloadBuffer);
//            } catch (IOException e) {
//                Log.e(TAG, "Network write error: " + tcb.ipAndPort, e);
//                sendRST(tcb, payloadSize, responseBuffer);
//                return;
//            }
//
//            // TODO: We don't expect out-of-order packets, but verify
//            tcb.myAcknowledgementNum = tcpHeader.sequenceNumber + payloadSize;
//            tcb.theirAcknowledgementNum = tcpHeader.acknowledgementNumber;
//            PacketHelper referencePacketHelper = tcb.mReferencePacketHelper;
//            referencePacketHelper.updateTCPBuffer(responseBuffer, (byte) TCPHeader.ACK, tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
//        }
//        outputQueue.offer(responseBuffer);
//    }
//
//    private void sendRST(TCB tcb, int prevPayloadSize, ByteBuffer buffer) {
//        tcb.mReferencePacketHelper.updateTCPBuffer(buffer, (byte) TCPHeader.RST, 0, tcb.myAcknowledgementNum + prevPayloadSize, 0);
//        outputQueue.offer(buffer);
//        TCB.closeTCB(tcb);
//    }
//
//    private void closeCleanly(TCB tcb, ByteBuffer buffer) {
//        ByteBufferPool.release(buffer);
//        TCB.closeTCB(tcb);
//    }
//}
