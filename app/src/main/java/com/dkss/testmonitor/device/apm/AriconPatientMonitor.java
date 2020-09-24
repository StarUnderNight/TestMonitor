package com.dkss.testmonitor.device.apm;

import com.dkss.testmonitor.device.BasicDevice;
import com.dkss.testmonitor.server.ServerInfo;
import com.dkss.testmonitor.util.DkssUtil;
import com.dkss.testmonitor.util.Payload;
import com.dkss.testmonitor.util.Protocol;
import com.dkss.testmonitor.util.SocketUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.interfaces.DSAKey;
import java.util.ArrayList;
import java.util.HashMap;


public class AriconPatientMonitor extends BasicDevice {
    private Socket socket;
    private ArrayList<byte[]> bufQueue;
    private byte[] boxTypePayload;
    private byte[] boxIDPayload ;
    private byte[] devTypePayload ;
    private ServerInfo info;

    public AriconPatientMonitor(Socket socket,ServerInfo info,byte[][] header) {
        this.socket = socket;
        this.info = info;
        this.bufQueue = new ArrayList<>();
        this.boxTypePayload = header[0];
        this.boxIDPayload = header[1];
        this.devTypePayload = header[2];
    }


    @Override
    public void run() {
        long lastTime = System.currentTimeMillis();
        try {
            BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
            byte[] netFlagCmd = { 0x02, 0x00, 0x00, 0x09 };
            byte[] buf = new byte[4096];

            while (true) {
                long curTime = System.currentTimeMillis();
                if (curTime - lastTime > 5000) {
                    lastTime = curTime;
                    bos.write(netFlagCmd);
                    bos.flush();
                }
                int len = bis.read(buf);
                if (len < 0) {
                    break;
                }
                byte[] data = new byte[len];
                System.arraycopy(buf, 0, data, 0, len);

                dealPacket(data);

                flushBuf(info,bufQueue);
            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            try {
                socket.close();
            } catch (IOException e1) {

                e1.printStackTrace();
            }
        }
    }

    private void dealPacket(byte[] packet) {
        int offset = 0;
        int modLen = 0;
        int packetLen = packet.length;
        byte[] timePayload = DkssUtil.getTimePayload();
        Payload payload = new Payload();
        payload.add(boxTypePayload, boxIDPayload, devTypePayload, timePayload);

        byte[] buf = null;

        while (offset < packetLen) {
            System.out.println("modLen = "+modLen+";offset = "+offset);
            modLen = 2 + DkssUtil.byteToShortSmall(packet, offset);

            switch (packet[offset + 3]) {
                case 0x01:
                    System.out.println("收到0x01");
                    break;
                case 0x03:
                    System.out.println("病人信息");
                    break;
                case 0x04:
                    System.out.println("收到模块信息包");
                    break;
                case 0x05:
                    System.out.println("收到0x05");
                    break;
                case 0x0b:
                    System.out.println("解析ECG");
                    if (!parseEcg(packet, offset, payload)) {
                        System.out.println("ECG解析false");
                        buf = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION, DkssUtil.DKSS_CMD_SEND_DATA,
                                payload.getNum(), payload.getData());
                        addBuf(bufQueue,buf);

                        payload.clear();
                        payload.add(boxTypePayload, boxIDPayload, devTypePayload, timePayload);

                        if (!parseEcg(packet, offset, payload)) {
                            System.err.println("ECG模块中的数据超出协议规定的数据包长度，需要修改协议");
                        }
                    }

                    break;
                case 0x0c:
                    System.out.println("解析SpO2");
                    if (!parseSpO2(packet, offset, payload)) {
                        System.out.println("SpO2解析false");
                        buf = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION, DkssUtil.DKSS_CMD_SEND_DATA,
                                payload.getNum(), payload.getData());
                        addBuf(bufQueue,buf);

                        payload.clear();
                        payload.add(boxTypePayload, boxIDPayload, devTypePayload, timePayload);

                        if (!parseEcg(packet, offset, payload)) {
                            System.err.println("SpO2模块中的数据超出协议规定的数据包长度，需要修改协议");
                        }
                    }
                    break;
                case 0x0d:
                    System.out.println("解析NIBP");
                    if (!parseNIBP(packet, offset, payload)) {
                        System.out.println("NIBP解析false");
                        buf = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION, DkssUtil.DKSS_CMD_SEND_DATA,
                                payload.getNum(), payload.getData());
                        addBuf(bufQueue,buf);

                        payload.clear();
                        payload.add(boxTypePayload, boxIDPayload, devTypePayload, timePayload);

                        if (!parseEcg(packet, offset, payload)) {
                            System.err.println("NIBP模块中的数据超出协议规定的数据包长度，需要修改协议");
                        }
                    }
                    break;
                case 0x0e:
                    System.out.println("解析Temp");
                    if (!parseTemp(packet, offset, payload)) {
                        System.out.println("Temp解析false");
                        buf = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION, DkssUtil.DKSS_CMD_SEND_DATA,
                                payload.getNum(), payload.getData());
                        addBuf(bufQueue,buf);

                        payload.clear();
                        payload.add(boxTypePayload, boxIDPayload, devTypePayload, timePayload);

                        if (!parseEcg(packet, offset, payload)) {
                            System.err.println("Temp模块中的数据超出协议规定的数据包长度，需要修改协议");
                        }
                    }
                    break;
                case 0x0f:
                    System.out.println("解析Fetus");
                    if (!parseFetus(packet, offset, payload)) {
                        System.out.println("Fetus解析false");
                        buf = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION, DkssUtil.DKSS_CMD_SEND_DATA,
                                payload.getNum(), payload.getData());
                        addBuf(bufQueue,buf);

                        payload.clear();
                        payload.add(boxTypePayload, boxIDPayload, devTypePayload, timePayload);

                        if (!parseEcg(packet, offset, payload)) {
                            System.err.println("Fetus模块中的数据超出协议规定的数据包长度，需要修改协议");
                        }
                    }
                    break;

                default:
                    System.err.println("unknown type "+String.format("%02x",packet[offset + 3])+";"+(offset+3));
                    break;
            }
            offset += modLen;
        }
        buf = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData());
        addBuf(bufQueue,buf);

    }

    private boolean parsePatient(byte[] packet, int offset, int modLen, Payload payload){
        return true;
    }

    private boolean parseEcg(byte[] packet, int offset, Payload payload) {

        int lead1 = (packet[offset+8]>>4);
        int lead2 = (packet[offset+10]>>4);
        System.out.println("导联 :"+lead1+","+lead2);

        short hr = DkssUtil.byteToShortSmall(packet,offset+12);
        short hrH = DkssUtil.byteToShortSmall(packet,offset+14);
        short hrL = DkssUtil.byteToShortSmall(packet,offset+16);

        short rr = DkssUtil.byteToShortSmall(packet,offset+20);
        short rrH = DkssUtil.byteToShortSmall(packet,offset+22);
        short rrL = DkssUtil.byteToShortSmall(packet,offset+24);

//        short st1 = DkssUtil.byteToShortSmall(packet,offset+28);
//        short st1H = DkssUtil.byteToShortSmall(packet,offset+30);
//        short st1L = DkssUtil.byteToShortSmall(packet,offset+32);
//
//        short st2 = DkssUtil.byteToShortSmall(packet,offset+36);
//        short st2H = DkssUtil.byteToShortSmall(packet,offset+38);
//        short st2L = DkssUtil.byteToShortSmall(packet,offset+40);
//
//        short pvc = DkssUtil.byteToShortSmall(packet,offset+44);
//        short pvcH = DkssUtil.byteToShortSmall(packet,offset+46);
//        short pvcL = DkssUtil.byteToShortSmall(packet,offset+48);

        short ecgWaveNum = 256;
        int rrWaveNum = 128;
        byte[] ecgWaveDataI = new byte[ecgWaveNum];
        byte[] ecgWaveDataII = new byte[ecgWaveNum];
        byte[] ecgWaveDataIII = new byte[ecgWaveNum];
        byte[] ecgWaveDataAVR = new byte[ecgWaveNum];
        byte[] ecgWaveDataAVL = new byte[ecgWaveNum];
        byte[] ecgWaveDataAVF = new byte[ecgWaveNum];
        byte[] ecgWaveDataV4 = new byte[ecgWaveNum];


        byte[] rrWaveData = new byte[rrWaveNum];
        //读取ecg波形数据
        for( int i=0;i<ecgWaveNum;i++){
            ecgWaveDataII[i] = packet[offset+52+i];
            ecgWaveDataI[i] = packet[offset+308+i];
            ecgWaveDataV4[i] = packet[offset+692+i];
            ecgWaveDataIII[i] = (byte)(ecgWaveDataII[i] - ecgWaveDataI[i] + 128);
            ecgWaveDataAVR[i] = (byte)(-(ecgWaveDataI[i] + ecgWaveDataII[i])/2 + 256);
            ecgWaveDataAVL[i] = (byte)((ecgWaveDataI[i] - ecgWaveDataIII[i])/2+128);
            ecgWaveDataAVF[i] = (byte)((ecgWaveDataII[i] + ecgWaveDataIII[i])/2 );
        }

        for(int i=0;i<rrWaveNum;i++){
            rrWaveData[i] = packet[offset+564+i];
        }

        boolean ret = payload.add(
                DkssUtil.constructPayload(Protocol.ID_ECG_HR,hr),
                DkssUtil.constructPayload(Protocol.ID_ECG_HR_H,hrH),
                DkssUtil.constructPayload(Protocol.ID_ECG_HR_L,hrL),
                DkssUtil.constructPayload(Protocol.ID_RESP_RR,rr),
                DkssUtil.constructPayload(Protocol.ID_RESP_RR_H,rrH),
                DkssUtil.constructPayload(Protocol.ID_RESP_RR_L,rrL),
                DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_NUM,ecgWaveNum),
                DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_I,ecgWaveDataI),
                DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_II,ecgWaveDataII),
                DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_III,ecgWaveDataIII),
                DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_AVR,ecgWaveDataAVR),
                DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_AVL,ecgWaveDataAVL),
                DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_AVF,ecgWaveDataAVF),
                DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_V4,ecgWaveDataV4),
                DkssUtil.constructPayload(Protocol.ID_RESP_WAVE,rrWaveData)
        );

        return ret;
    }


    private boolean parseSpO2(byte[] packet,int offset,Payload payload){

        short spo2 = DkssUtil.byteToShortSmall(packet,offset+8);
        short spo2H = DkssUtil.byteToShortSmall(packet,offset+10);
        short spo2L = DkssUtil.byteToShortSmall(packet,offset+12);

        short pr = DkssUtil.byteToShortSmall(packet,offset+16);
        short prH = DkssUtil.byteToShortSmall(packet,offset+18);
        short prL = DkssUtil.byteToShortSmall(packet,offset+20);


        int spo2WaveNum = 86;
        byte[] spo2WaveData = new byte[spo2WaveNum];
        for(int i=0;i<spo2WaveNum;i++){
            spo2WaveData[i] = packet[offset+24+i];
        }

        boolean ret = payload.add(
                DkssUtil.constructPayload(Protocol.ID_SpO2,spo2),
                DkssUtil.constructPayload(Protocol.ID_SpO2_H,spo2H),
                DkssUtil.constructPayload(Protocol.ID_SpO2_L,spo2L),
                DkssUtil.constructPayload(Protocol.ID_PR,pr),
                DkssUtil.constructPayload(Protocol.ID_PR_H,prH),
                DkssUtil.constructPayload(Protocol.ID_PR_L,prL),
                DkssUtil.constructPayload(Protocol.ID_SpO2_WAVE_BO,spo2WaveData)
        );
        return ret;
    }

    private boolean parseNIBP(byte[] packet,int offset,Payload payload){

        short sys = DkssUtil.byteToShortSmall(packet,offset+20);
        short sysH = DkssUtil.byteToShortSmall(packet,offset+22);
        short sysL = DkssUtil.byteToShortSmall(packet,offset+24);
        short mean = DkssUtil.byteToShortSmall(packet,offset+28);
        short meanH = DkssUtil.byteToShortSmall(packet,offset+30);
        short meanL = DkssUtil.byteToShortSmall(packet,offset+32);
        short dia = DkssUtil.byteToShortSmall(packet,offset+36);
        short diaH = DkssUtil.byteToShortSmall(packet,offset+38);
        short diaL = DkssUtil.byteToShortSmall(packet,offset+40);

        boolean ret = payload.add(
                DkssUtil.constructPayload(Protocol.ID_NIBP_SYS,sys),
                DkssUtil.constructPayload(Protocol.ID_NIBP_SYS_H,sysH),
                DkssUtil.constructPayload(Protocol.ID_NIBP_SYS_L,sysL),
                DkssUtil.constructPayload(Protocol.ID_NIBP_MEAN,mean),
                DkssUtil.constructPayload(Protocol.ID_NIBP_MEAN_H,meanH),
                DkssUtil.constructPayload(Protocol.ID_NIBP_MEAN_L,meanL),
                DkssUtil.constructPayload(Protocol.ID_NIBP_DIA,dia),
                DkssUtil.constructPayload(Protocol.ID_NIBP_DIA_H,diaH),
                DkssUtil.constructPayload(Protocol.ID_NIBP_DIA_L,diaL)
        );
        return ret;
    }

    private boolean parseTemp(byte[] packet,int offset,Payload payload){

        short t1 = DkssUtil.byteToShortSmall(packet,offset+8);
        short t1H = DkssUtil.byteToShortSmall(packet,offset+10);
        short t1L = DkssUtil.byteToShortSmall(packet,offset+12);
        short t2 = DkssUtil.byteToShortSmall(packet,offset+16);
        short t2H = DkssUtil.byteToShortSmall(packet,offset+18);
        short t2L = DkssUtil.byteToShortSmall(packet,offset+20);

        boolean ret = payload.add(
                DkssUtil.constructPayload(Protocol.ID_TEMP_T1,t1),
                DkssUtil.constructPayload(Protocol.ID_TEMP_T1_H,t1H),
                DkssUtil.constructPayload(Protocol.ID_TEMP_T1_L,t1L),
                DkssUtil.constructPayload(Protocol.ID_TEMP_T2,t2),
                DkssUtil.constructPayload(Protocol.ID_TEMP_T2_H,t2H),
                DkssUtil.constructPayload(Protocol.ID_TEMP_T2_L,t2L)
        );
        return ret;
    }

    private boolean parseFetus(byte[] packet,int offset,Payload payload){

        return true;
    }


}

