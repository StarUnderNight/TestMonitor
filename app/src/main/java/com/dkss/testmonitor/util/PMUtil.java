package com.dkss.testmonitor.util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

public class PMUtil {

    //ff d0 00 08 00 fe 00 09 34 01 02 03 04 0b 32 33 50
    public static HashMap<String, Byte> parsePacketD0(byte[] data) {
        return null;
    }

    public static void parsePacketD1(byte[] data) {

    }

    public static void parsePacketD3_7(byte[] data) {

    }

    public static void parsePacketD4(byte[] data) {
        int offset = 19;
        byte[] temp = new byte[2];
        temp[0] = data[17];
        temp[1] = data[18];
        int packetLen = DkssUtil.byteToShort(temp, 0);


    }

    public static void sendUDP(String ip,int port,byte[] data) {

        try {
            InetAddress addr = InetAddress.getByName(ip);
            DatagramPacket packet = new DatagramPacket(data,data.length,addr,port);
            DatagramSocket socket;
            socket = new DatagramSocket();
            socket.send(packet);
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static DatagramPacket receiveUDP(int port, int soTimeout) {
        DatagramSocket socket = null;
        DatagramPacket packet = null;

        try {
            byte[] buf = new byte[4096];
            packet = new DatagramPacket(buf, buf.length);
            socket = new DatagramSocket(port);
            socket.setSoTimeout(soTimeout);
            socket.receive(packet);
            socket.close();

        } catch (SocketException e) {
            e.printStackTrace();
            if(socket !=null){
                socket.close();
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            if(socket !=null){
                socket.close();
            }
            return null;
        }
        return packet;
    }


    public static int createPort() {
        int i = 0;
        DatagramSocket socket = null;
        for (i = 50000; i < 60000; i++) {
            try {
                socket = new DatagramSocket(i);
            } catch (Exception e) {

            }
            socket.close();
            return i;
        }

        return 0;

    }

    public static byte[] portToByte(int port) {

        byte[] ret = new byte[3];
        ret[2] = (byte) (port & 0x7f);
        ret[1] = (byte) ((port >> 7) & 0x7f);
        ret[0] = (byte) ((port >> 14) & 0x7f);
        return ret;
    }

    public static byte[] getTimeSyn() {
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date data = new Date(System.currentTimeMillis());
        int year = data.getYear();
        int month = data.getMonth();
        int day = data.getDay();
        int hour = data.getHours();
        int min = data.getMinutes();
        int sec = data.getSeconds();

        byte[] time = new byte[9];
        time[0] = (byte)(year>>7);
        time[1] = (byte)(year&0x7f);
        time[2] = (byte)(month);
        time[3] = (byte)(day);
        time[4] = (byte)(hour);
        time[5] = (byte)(min);
        time[6] = (byte)(sec);
        return time;

    }

    public static String getLocalBroadCast() {
        String broadCastIp = null;
        try {
            Enumeration<?> netInterfaces = (Enumeration<?>) NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) netInterfaces.nextElement();
                if (!netInterface.isLoopback() && netInterface.isUp()) {
                    List<InterfaceAddress> interfaceAddresses = netInterface.getInterfaceAddresses();
                    for (InterfaceAddress interfaceAddress : interfaceAddresses) {
                        // 只有 IPv4 网络具有广播地址，因此对于 IPv6 网络将返回 null。
                        if (interfaceAddress.getBroadcast() != null) {
                            broadCastIp = interfaceAddress.getBroadcast().getHostAddress();

                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return broadCastIp;
    }

}
