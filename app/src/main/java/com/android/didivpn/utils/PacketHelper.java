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
//package com.android.didivpn.utils;
//
//import java.net.UnknownHostException;
//import java.nio.ByteBuffer;
//
//import com.android.didivpn.packet.TCP;
//import com.android.didivpn.packet.UDP;
//import com.android.didivpn.packet.ip.IPv4;
//import com.android.didivpn.utils.BitUtils;
//
//import static com.android.didivpn.packet.ip.IPv4.IP4_FIXED_HEADER_LENGTH;
//
///**
// * Representation of an IP Packet
// */
//public class PacketHelper {
//
//    public IPv4 ip4;
//    public TCP tcp;
//    public UDP udp;
//    public ByteBuffer backingBuffer;
//
//    private boolean isTCP;
//    private boolean isUDP;
//
//    public PacketHelper(ByteBuffer buffer) {
//        buffer.position(0);
//        this.ip4 = new IPv4(buffer);
//        if (TCP.isTCP(ip4.getProtocol())) {
//            this.tcp = new TCP(ip4);
//            this.isTCP = true;
//        } else if (UDP.isUDP(ip4.getProtocol())) {
//            this.udp = new UDP(ip4);
//            this.isUDP = true;
//        }
////        this.backingBuffer = buffer;
//    }
//
//    @Override
//    public String toString() {
//        final StringBuilder sb = new StringBuilder("PacketHelper{");
//        sb.append("ip4=").append(ip4);
//        if (isTCP) {
//            sb.append(", tcp=").append(tcp);
//        } else if (isUDP) {
//            sb.append(", udp=").append(udp);
//        }
//        sb.append('}');
//        return sb.toString();
//    }
//
//    public boolean isTCP() {
//        return isTCP;
//    }
//
//    public boolean isUDP() {
//        return isUDP;
//    }
//
//    public void swapSourceAndDestination() throws UnknownHostException {
//        int newSourceAddress = ip4.getDestAddress();
//        ip4.setDestAddress(ip4.getSrcAddress());
//        ip4.setSrcAddress(newSourceAddress);
//
//        if (isUDP) {
//            int newSourcePort = udp.destinationPort;
//            udp.destinationPort = udp.sourcePort;
//            udp.sourcePort = newSourcePort;
//        } else if (isTCP) {
//            int newSourcePort = tcp.destinationPort;
//            tcp.destinationPort = tcp.sourcePort;
//            tcp.sourcePort = newSourcePort;
//        }
//    }
//
//    public void updateTCPBuffer(ByteBuffer buffer, byte flags, long sequenceNum, long ackNum, int payloadSize) {
//        buffer.position(0);
//        fillHeader(buffer);
//        backingBuffer = buffer;
//
//        tcp.flags = flags;
//        backingBuffer.put(IP4_FIXED_HEADER_LENGTH + 13, flags);
//
//        tcp.sequenceNumber = sequenceNum;
//        backingBuffer.putInt(IP4_FIXED_HEADER_LENGTH + 4, (int) sequenceNum);
//
//        tcp.acknowledgementNumber = ackNum;
//        backingBuffer.putInt(IP4_FIXED_HEADER_LENGTH + 8, (int) ackNum);
//
//        // Reset header size, since we don't need options
//        byte dataOffset = (byte) (TCP_HEADER_SIZE << 2);
//        tcp.dataOffsetAndReserved = dataOffset;
//        backingBuffer.put(IP4_FIXED_HEADER_LENGTH + 12, dataOffset);
//
//        try {
//            updateTCPChecksum(payloadSize);
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        }
//
//        int ip4TotalLength = IP4_FIXED_HEADER_LENGTH + TCP_HEADER_SIZE + payloadSize;
//        backingBuffer.putShort(2, (short) ip4TotalLength);
//        ip4.setTotalLength(ip4TotalLength);
//
//        updateIP4Checksum();
//    }
//
//    public void updateUDPBuffer(ByteBuffer buffer, int payloadSize) {
//        buffer.position(0);
//        fillHeader(buffer);
//        backingBuffer = buffer;
//
//        int udpTotalLength = UDP_HEADER_SIZE + payloadSize;
//        backingBuffer.putShort(IP4_FIXED_HEADER_LENGTH + 4, (short) udpTotalLength);
//        udp.length = udpTotalLength;
//
//        // Disable UDP checksum validation
//        backingBuffer.putShort(IP4_FIXED_HEADER_LENGTH + 6, (short) 0);
//        udp.checksum = 0;
//
//        int ip4TotalLength = IP4_FIXED_HEADER_LENGTH + udpTotalLength;
//        backingBuffer.putShort(2, (short) ip4TotalLength);
//        ip4.setTotalLength(ip4TotalLength);
//
//        updateIP4Checksum();
//    }
//
//    private void updateIP4Checksum() {
//        ByteBuffer buffer = backingBuffer.duplicate();
//        buffer.position(0);
//
//        // Clear previous checksum
//        buffer.putShort(10, (short) 0);
//
//        int ipLength = ip4.getInternetHeaderLength();
//        int sum = 0;
//        while (ipLength > 0) {
//            sum += BitUtils.getUnsignedShort(buffer.getShort());
//            ipLength -= 2;
//        }
//        while (sum >> 16 > 0)
//            sum = (sum & 0xFFFF) + (sum >> 16);
//
//        sum = ~sum;
//        ip4.setHeaderChecksum((short) sum);
//        backingBuffer.putShort(10, (short) sum);
//    }
//
//    private void updateTCPChecksum(int payloadSize) throws UnknownHostException {
//        int sum = 0;
//        int tcpLength = TCP_HEADER_SIZE + payloadSize;
//
//        // Calculate pseudo-header checksum
//        ByteBuffer buffer = ByteBuffer.wrap(ip4.getSrcAddressAsInetAddress().getAddress());
//        sum = BitUtils.getUnsignedShort(buffer.getShort()) + BitUtils.getUnsignedShort(buffer.getShort());
//
//        buffer = ByteBuffer.wrap(ip4.getDestAddressAsInetAddress().getAddress());
//        sum += BitUtils.getUnsignedShort(buffer.getShort()) + BitUtils.getUnsignedShort(buffer.getShort());
//
//        sum += TransportProtocol.TCP.getNumber() + tcpLength;
//
//        buffer = backingBuffer.duplicate();
//        // Clear previous checksum
//        buffer.putShort(IP4_FIXED_HEADER_LENGTH + 16, (short) 0);
//
//        // Calculate TCP segment checksum
//        buffer.position(IP4_FIXED_HEADER_LENGTH);
//        while (tcpLength > 1) {
//            sum += BitUtils.getUnsignedShort(buffer.getShort());
//            tcpLength -= 2;
//        }
//        if (tcpLength > 0)
//            sum += BitUtils.getUnsignedByte(buffer.get()) << 8;
//
//        while (sum >> 16 > 0)
//            sum = (sum & 0xFFFF) + (sum >> 16);
//
//        sum = ~sum;
//        tcp.checksum = sum;
//        backingBuffer.putShort(IP4_FIXED_HEADER_LENGTH + 16, (short) sum);
//    }
//
//    private void fillHeader(ByteBuffer buffer) {
//        ip4.fillHeaderToBuffer(buffer);
//        if (isUDP)
//            udp.fillHeader(buffer);
//        else if (isTCP)
//            tcp.fillHeader(buffer);
//    }
//
//}
