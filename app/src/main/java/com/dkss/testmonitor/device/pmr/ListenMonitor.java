package com.dkss.testmonitor.device.pmr;


import com.dkss.testmonitor.device.BasicDevice;
import com.dkss.testmonitor.server.ServerInfo;
import com.dkss.testmonitor.util.PMUtil;
import com.dkss.testmonitor.util.DkssUtil;
import com.dkss.testmonitor.util.Payload;
import com.dkss.testmonitor.util.Protocol;
import com.dkss.testmonitor.util.SocketUtil;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.HashMap;

public class ListenMonitor extends BasicDevice {

    private ArrayList<byte[]> bufQueue = new ArrayList<>();

    private static final int BROAD_PORT = 8002;

    private   byte[] boxIDPayload ;
    private   byte[] boxTypePayload ;
    private   byte[] devTypePayload ;
    private  byte[] timePayload;

    private ServerInfo info;

    private int listenPort = 0;
    volatile boolean isGetData = false;
    volatile HashMap<String,Object> pmrInfoMap = new HashMap<>();

    ListenMonitor(ServerInfo info, byte[][] header){
        boxTypePayload = header[0];
        boxIDPayload = header[1];
        devTypePayload = header[2];
        timePayload = null;
        this.info = info;
        listenPort = PMUtil.createPort();
    }


    @Override
    public void run() {



        int requestNo = 0;
        byte[] portByteArr = PMUtil.portToByte(listenPort);
        byte[] requestByte = new byte[]{(byte) 0xff, (byte) 0xda, (byte) 0x7f, 0, 0x00, 0, 0x00, 0x06,
                0x02, 0x08, 0x01, portByteArr[0], portByteArr[1], portByteArr[2]};
        byte[] timeSynByte = DkssUtil.mergeByte(new byte[]{(byte) 0xff, (byte) 0xda, (byte) 0x7f, 0, 0x00, 0, 0x00, 0x0c,
                0x02, 0x08, 0x0d}, PMUtil.getTimeSyn());
        byte[] cfgByte = new byte[]{(byte) 0xff, (byte) 0xda, (byte) 0x7f, 0, 0x00, 0, 0x00, 0x03,
                0x02, 0x08, 0x04};

        while(true){

            DatagramPacket dp = PMUtil.receiveUDP(listenPort, 5000);

            if(dp == null){
                String pIP = (String)pmrInfoMap.get("pIP");
                byte cMachineNO = (byte)pmrInfoMap.get("cMachineNO");
                byte pMachineNO = (byte)pmrInfoMap.get("pMachineNO");


                if (requestNo >= 16) {
                    requestNo = 0;
                }
                byte NO = (byte) (requestNo << 3);

                requestByte[3] = cMachineNO;
                requestByte[5] = pMachineNO;
                requestByte[9] = NO;

                cfgByte[3] = cMachineNO;
                cfgByte[5] = pMachineNO;
                cfgByte[9] = NO;

                timeSynByte[3] = cMachineNO;
                timeSynByte[5] = pMachineNO;
                timeSynByte[9] = NO;
                byte[] temp = PMUtil.getTimeSyn();
                for(int i=0;i<temp.length;i++){
                    timeSynByte[i+11] = temp[i];
                }

                requestNo++;

                PMUtil.sendUDP(pIP, BROAD_PORT, requestByte);
                PMUtil.sendUDP(pIP, BROAD_PORT, timeSynByte);
                PMUtil.sendUDP(pIP, BROAD_PORT, cfgByte);

                continue;
            }
            isGetData = true;

            byte[] data = new byte[dp.getLength()];
            System.arraycopy(dp.getData(),0,data,0,data.length);
            parsePacketD1(data);
            flushBuf(info,bufQueue);
        }

    }

    private void parsePacketD1(byte[] data) {

        Payload payload = new Payload();
        timePayload = DkssUtil.getTimePayload();
        int packetLen = data[7]*128 +data[8]+9;
        int modLen = -1;   //各个模块的模块总长度
        int offset = 10;

        if(data.length!=packetLen){
            System.err.println("D1数据包长度检验不通过");
            return;
        }

        payload.add(boxTypePayload,boxIDPayload,devTypePayload,timePayload);

        while (offset<packetLen) {

            modLen = data[offset+2]*128+data[offset+3]+4;
            byte[] buf = null;
            // ECG数据块
            if (data[offset] == (byte)0xe0) {

                if(!parseEcg(data,offset,modLen,payload)){
                    System.out.println("ECG解析false");
                    DkssUtil.printByte(payload.getData());
                    buf = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData());
                    addBuf(bufQueue,buf);

                    payload.clear();
                    payload.add(boxTypePayload,boxIDPayload,devTypePayload,timePayload);

                    if(!parseEcg(data,offset,modLen,payload)){
                        System.err.println("ECG模块中的数据超出协议规定的数据包长度，需要修改协议");
                    }
                }
            }else if(data[offset] == (byte)0xe1){  //SpO2
                if(!parseSpO2(data,offset,modLen,payload)){
                    buf = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData());
                    addBuf(bufQueue,buf);

                    payload.clear();
                    payload.add(boxTypePayload,boxIDPayload,devTypePayload,timePayload);

                    if(!parseSpO2(data,offset,modLen,payload)){
                        System.err.println("SpO2模块中的数据超出协议规定的数据包长度，需要修改协议");
                    }
                }
            }else if(data[offset] == (byte)0xe2){   //Pulse
                if(!parsePulse(data,offset,modLen,payload)){
                    System.out.println("Pulse解析false");
                    buf = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData());
                    addBuf(bufQueue,buf);

                    payload.clear();
                    payload.add(boxTypePayload,boxIDPayload,devTypePayload,timePayload);

                    if(!parsePulse(data,offset,modLen,payload)){
                        System.err.println("Pulse模块中的数据超出协议规定的数据包长度，需要修改协议");
                    }
                }
            }else if(data[offset] == (byte)0xe3){  //RESP
                if(!parseResp(data,offset,modLen,payload)){
                    System.out.println("Resp解析false");
                    buf = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData());
                    addBuf(bufQueue,buf);

                    payload.clear();
                    payload.add(boxTypePayload,boxIDPayload,devTypePayload,timePayload);

                    if(!parseResp(data,offset,modLen,payload)){
                        System.err.println("RESP模块中的数据超出协议规定的数据包长度，需要修改协议");
                    }
                }
            }else if(data[offset] == (byte)0xe4){   //Temp
                if(!parseTemp(data,offset,modLen,payload)){
                    System.out.println("Temp解析false");
                    buf = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData());
                    addBuf(bufQueue,buf);

                    payload.clear();
                    payload.add(boxTypePayload,boxIDPayload,devTypePayload,timePayload);

                    if(!parseTemp(data,offset,modLen,payload)){
                        System.err.println("Temp模块中的数据超出协议规定的数据包长度，需要修改协议");
                    }
                }
            }else if(data[offset] == (byte)0xe5){   //NIBP

                if(!parseNIBP(data,offset,modLen,payload)){
                    System.out.println("NIBP解析false");
                    buf = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData());
                    addBuf(bufQueue,buf);

                    payload.clear();
                    payload.add(boxTypePayload,boxIDPayload,devTypePayload,timePayload);

                    if(!parseNIBP(data,offset,modLen,payload)){
                        System.err.println("NIBP模块中的数据超出协议规定的数据包长度，需要修改协议");
                    }
                }
            }else if(data[offset] == (byte)0xe6){   //IBP

                if(!parseIBP(data,offset,modLen,payload)){
                    System.out.println("IBP解析false");
                    buf = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData());
                    addBuf(bufQueue,buf);

                    payload.clear();
                    payload.add(boxTypePayload,boxIDPayload,devTypePayload,timePayload);

                    if(!parseIBP(data,offset,modLen,payload)){
                        System.err.println("IBP模块中的数据超出协议规定的数据包长度，需要修改协议");
                    }
                }
            }else if(data[offset] == (byte)0xe7){

            }else if(data[offset] == (byte)0xe8){

            }else if(data[offset] == (byte)0xe9){

            }else if(data[offset] == (byte)0xea){

            }else if(data[offset] == (byte)0xeb){

            }else if(data[offset] == (byte)0xfc){

            }else if(data[offset] == (byte)0xfd){

            }else if(data[offset] == (byte)0xfe){
                modLen = data[offset+1]*128+data[offset+2]+3;
                System.out.println("出现FE模块");
            }

            offset +=modLen;
        }

        byte[] buf = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData());
        addBuf(bufQueue,buf);
    }

    private boolean parseEcg(byte[] data, int offset, int modLen, Payload payload){

        ArrayList<Short> ecgShortListI = new ArrayList<>();
        ArrayList<Short> ecgShortListII = new ArrayList<>();
        ArrayList<Short> ecgShortListIII = new ArrayList<>();
        ArrayList<Short> ecgShortListAVR = new ArrayList<>();
        ArrayList<Short> ecgShortListAVL = new ArrayList<>();
        ArrayList<Short> ecgShortListAVF = new ArrayList<>();
        ArrayList<Short> ecgShortListV4 = new ArrayList<>();
        ArrayList<Short> ecgShortListV1 = new ArrayList<>();
        ArrayList<Short> ecgShortListV2 = new ArrayList<>();
        ArrayList<Short> ecgShortListV3 = new ArrayList<>();
        ArrayList<Short> ecgShortListV5 = new ArrayList<>();
        ArrayList<Short> ecgShortListV6 = new ArrayList<>();

        //判断几导
        int lead = 0;
        int waveSize = 0;
        int fixedLen = 9;

        //处理固定参数的数据
        short ecgHr =  (short)((data[offset+6]&0x03)*128+data[offset+8]);
        byte[] ecgHrPayload = DkssUtil.constructPayload(Protocol.ID_ECG_HR,ecgHr);

        switch ((data[offset + 5] & 0x03)){
            case 0x00:
                lead = 5;
                waveSize = (modLen-fixedLen)/6;
                break;
            case 0x02:
                lead = 3;
                waveSize = (modLen-fixedLen)/3;
                break;
            case 0x01:
                lead = 12;
                waveSize = (modLen-fixedLen)/16;
                break;
        }

        byte[] ecgByteArrI = new byte[waveSize];
        byte[] ecgByteArrII = new byte[waveSize];
        byte[] ecgByteArrIII = new byte[waveSize];
        byte[] ecgByteArrAVR = new byte[waveSize];
        byte[] ecgByteArrAVL = new byte[waveSize];
        byte[] ecgByteArrAVF = new byte[waveSize];
        byte[] ecgByteArrV4 = new byte[waveSize];
        byte[] ecgByteArrV1 = new byte[waveSize];
        byte[] ecgByteArrV2 = new byte[waveSize];
        byte[] ecgByteArrV3 = new byte[waveSize];
        byte[] ecgByteArrV5 = new byte[waveSize];
        byte[] ecgByteArrV6 = new byte[waveSize];

        int i = 0;
        boolean ret = false;

        switch (lead) {
            case 3:
                System.out.println("还没有做3导");
                break;
            case 5:
                for (i = 0; i < (modLen-fixedLen)/6 ; i++) {
                    short dataII = (short)((data[offset+fixedLen+i*6]&0x7f)*128 + (data[offset+fixedLen+i+1]&0x7f));
                    short dataI = (short)((data[offset+fixedLen+i*6+2]&0x7f)*128 + (data[offset+fixedLen+i+3]&0x7f));
                    short dataV4 = (short)((data[offset+fixedLen+i*6+4]&0x7f)*128 + (data[offset+fixedLen+i+5]&0x7f));

                    short dataIII  = (short)(dataII-dataI +8192);
                    short dataAVR = (short)(8192*2-(dataI+dataII)/2);
                    short dataAVL = (short)((dataI-dataIII)/2 +8192);
                    short dataAVF = (short)((dataII+dataIII)/2);

                    ecgShortListI.add(dataI);
                    ecgShortListII.add(dataII);
                    ecgShortListIII.add(dataIII);
                    ecgShortListAVR.add(dataAVR);
                    ecgShortListAVL.add(dataAVL);
                    ecgShortListAVF.add(dataAVF);
                    ecgShortListV4.add(dataV4);
                }

                for(int j=0;j<waveSize;j++){
                    byte wTemp = 0;
                    wTemp = (byte)(ecgShortListI.get(j)/64);
                    ecgByteArrI[j] = wTemp;
                    wTemp = (byte)(ecgShortListII.get(j)/64);
                    ecgByteArrII[j] = wTemp;
                    wTemp = (byte)(ecgShortListIII.get(j)/64);
                    ecgByteArrIII[j] = wTemp;
                    wTemp = (byte)(ecgShortListAVR.get(j)/64);
                    ecgByteArrAVR[j] = wTemp;
                    wTemp = (byte)(ecgShortListAVL.get(j)/64);
                    ecgByteArrAVL[j] = wTemp;
                    wTemp = (byte)(ecgShortListAVF.get(j)/64);
                    ecgByteArrAVF[j] = wTemp;
                    wTemp = (byte)(ecgShortListV4.get(j)/64);
                    ecgByteArrV4[j] = wTemp;
                }
                //加入payload
                ret =  payload.add(
                        ecgHrPayload,
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_NUM,(short)waveSize),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_I,ecgByteArrI),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_II,ecgByteArrII),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_III,ecgByteArrIII),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_AVR,ecgByteArrAVR),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_AVL,ecgByteArrAVL),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_AVF,ecgByteArrAVF),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_V4,ecgByteArrV4)
                );

                break;
            case 12:
                for (i = 0; i < modLen-fixedLen ; i = i + 16) {
                    short dataII = (short)((data[offset+fixedLen+i]&0x7f)*128 + (data[offset+fixedLen+i+1]&0x7f));
                    short dataI = (short)((data[offset+fixedLen+i+2]&0x7f)*128 + (data[offset+fixedLen+i+3]&0x7f));
                    short dataV4 = (short)((data[offset+fixedLen+i+4]&0x7f)*128 + (data[offset+fixedLen+i+5]&0x7f));
                    short dataV1 = (short)((data[offset+fixedLen+i+6]&0x7f)*128 + (data[offset+fixedLen+i+7]&0x7f));
                    short dataV2 = (short)((data[offset+fixedLen+i+8]&0x7f)*128 + (data[offset+fixedLen+i+9]&0x7f));
                    short dataV3 = (short)((data[offset+fixedLen+i+10]&0x7f)*128 + (data[offset+fixedLen+i+11]&0x7f));
                    short dataV5 = (short)((data[offset+fixedLen+i+12]&0x7f)*128 + (data[offset+fixedLen+i+13]&0x7f));
                    short dataV6 = (short)((data[offset+fixedLen+i+14]&0x7f)*128 + (data[offset+fixedLen+i+15]&0x7f));

                    short dataIII  = (short)(dataII-dataI +8192);
                    short dataAVR = (short)(8192*2-(dataI+dataII)/2);
                    short dataAVL = (short)((dataI-dataIII)/2 +8192);
                    short dataAVF = (short)((dataII+dataIII)/2);

                    ecgShortListI.add(dataI);
                    ecgShortListII.add(dataII);
                    ecgShortListIII.add(dataIII);
                    ecgShortListAVR.add(dataAVR);
                    ecgShortListAVL.add(dataAVL);
                    ecgShortListAVF.add(dataAVF);
                    ecgShortListV4.add(dataV4);
                    ecgShortListV1.add(dataV1);
                    ecgShortListV2.add(dataV2);
                    ecgShortListV3.add(dataV3);
                    ecgShortListV5.add(dataV5);
                    ecgShortListV6.add(dataV6);
                }

                for(int j=0;j<waveSize;j++){
                    byte wTemp = 0;
                    wTemp = (byte)(ecgShortListI.get(j)/64);
                    ecgByteArrI[j] = wTemp;
                    wTemp = (byte)(ecgShortListII.get(j)/64);
                    ecgByteArrII[j] = wTemp;
                    wTemp = (byte)(ecgShortListIII.get(j)/64);
                    ecgByteArrIII[j] = wTemp;
                    wTemp = (byte)(ecgShortListAVR.get(j)/64);
                    ecgByteArrAVR[j] = wTemp;
                    wTemp = (byte)(ecgShortListAVL.get(j)/64);
                    ecgByteArrAVL[j] = wTemp;
                    wTemp = (byte)(ecgShortListAVF.get(j)/64);
                    ecgByteArrAVF[j] = wTemp;
                    wTemp = (byte)(ecgShortListV4.get(j)/64);
                    ecgByteArrV4[j] = wTemp;
                    wTemp = (byte)(ecgShortListV1.get(j)/64);
                    ecgByteArrV1[j] = wTemp;
                    wTemp = (byte)(ecgShortListV2.get(j)/64);
                    ecgByteArrV2[j] = wTemp;
                    wTemp = (byte)(ecgShortListV3.get(j)/64);
                    ecgByteArrV3[j] = wTemp;
                    wTemp = (byte)(ecgShortListV5.get(j)/64);
                    ecgByteArrV5[j] = wTemp;
                    wTemp = (byte)(ecgShortListV6.get(j)/64);
                    ecgByteArrV6[j] = wTemp;
                }
                //加入payload
                ret =  payload.add(
                        ecgHrPayload,
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_NUM,(short)waveSize),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_I,ecgByteArrI),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_II,ecgByteArrII),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_III,ecgByteArrIII),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_AVR,ecgByteArrAVR),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_AVL,ecgByteArrAVL),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_AVF,ecgByteArrAVF),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_V4,ecgByteArrV4),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_V1,ecgByteArrV1),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_V2,ecgByteArrV2),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_V3,ecgByteArrV3),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_V5,ecgByteArrV5),
                        DkssUtil.constructPayload(Protocol.ID_ECG_WAVE_V6,ecgByteArrV6)
                );

                break;
            default:
                System.err.println("错误的导联");
                break;
        }

        return ret;
    }

    private boolean parseSpO2(byte[] data,int offset,int modLen,Payload payload){
        int fixedLen = 9;
        short spo2Value = data[offset+5];
        //spo2Value = spo2Value == (short)65535?0:spo2Value;

        //处理波形数据
        int waveSize = (modLen-fixedLen)/3;
        byte[] spo2ByteArrBO = new byte[waveSize];

        int i=0;
        if(waveSize!=(modLen-fixedLen)/3){
            System.out.println("Spo2 波形数据长度有错 waveSize="+waveSize+";"+"数据长度:"+(modLen-fixedLen)/3);
        }

        for(i=0;i<waveSize;i++){
            spo2ByteArrBO[i] = (byte)((data[offset+fixedLen+i*3+2]>>2)*128+data[offset+fixedLen+i*3]);
        }

        boolean ret =payload.add(
                DkssUtil.constructPayload(Protocol.ID_SpO2_WAVE_BO,spo2ByteArrBO),
                DkssUtil.constructPayload(Protocol.ID_SpO2,spo2Value)
        );
        return ret;
    }

    private boolean parsePulse(byte[] data, int offset, int modLen, Payload payload){

        short pulsePR = (short)(data[offset+5]*128+data[offset+4]);
//        if(pulsePR == (short)65535 || ((data[offset+5]&0x04)==0x04)){
//            pulsePR =0;
//        }
        boolean ret = payload.add(DkssUtil.constructPayload(Protocol.ID_PR,pulsePR));
        return ret;
    }

    private boolean parseResp(byte[] data, int offset, int modLen, Payload payload){
        int fixedLen = 7;
        int waveSize = (modLen-fixedLen)/2;
        ArrayList<Short> respShortList = new ArrayList<>();
        byte[] respByteArr = new byte[waveSize];
        short respRR = data[offset+4];
//        if((data[offset+5]&0x01)==0 && (data[offset+4] != (byte)0xff)){
//            respRR = data[offset+4];
//        }

        for(int i=0;i<waveSize;i++){
            respShortList.add((short)((data[offset+fixedLen+i*2]>>2)*128+data[offset+fixedLen+i*2+1]));
        }
        for(int i=0;i<waveSize;i++){
            respByteArr[i] = (byte)(respShortList.get(i)/2);
        }
        boolean ret = payload.add(
                DkssUtil.constructPayload(Protocol.ID_RESP_RR,respRR),
                DkssUtil.constructPayload(Protocol.ID_RESP_WAVE,respByteArr));
        return ret;
    }

    private boolean parseTemp(byte[] data,int offset,int modLen,Payload payload){

        float tempValue = (float)((data[offset+6]*128+data[offset+5])*0.1);
        //判断是否有体温，当体温所对应的位全为1时，表示无体温值
//        if(data[offset+5] ==(byte)0x7f && data[offset+6] == (byte)0x3f){
//            tempValue = 0;
//        }

        byte[][] id = new byte[][]{
                Protocol.ID_TEMP_T1,
                Protocol.ID_TEMP_T2,
                Protocol.ID_TEMP_ESO,
                Protocol.ID_TEMP_NASO,
                Protocol.ID_TEMP_TYMP,
                Protocol.ID_TEMP_RECT,
                Protocol.ID_TEMP_BLAD,
                Protocol.ID_TEMP_SKIN
        } ;
        int index = -1;
        switch (data[offset+1]){
            case 50: index = 0;break;
            case 51: index = 1;break;
            case 52: index = 2;break;
            case 53: index = 3;break;
            case 54: index = 4;break;
            case 55: index = 5;break;
            case 56: index = 6;break;
            case 57: index = 7;break;
        }

        boolean ret = payload.add(DkssUtil.constructPayload(id[index],tempValue));
        return ret;
    }

    private boolean parseNIBP(byte[] data,int offset,int modLen,Payload payload){
        if((data[offset+5]&0xf3) != 1){
            return true;
        }

        short nibpSys = (short)((data[offset+9]*128+data[offset+8])*0.1);
        short nibpMean = (short)((data[offset+11]*128+data[offset+10])*0.1);
        short nibpDia = (short)((data[offset+13]*128+data[offset+12])*0.1);
//        if(nibpSys==793 && nibpMean==818 && nibpDia ==818){
//            nibpSys = 0;
//            nibpMean = 0;
//            nibpDia = 0;
//        }

        boolean ret = payload.add(
                DkssUtil.constructPayload(Protocol.ID_NIBP_SYS,nibpSys),
                DkssUtil.constructPayload(Protocol.ID_NIBP_MEAN,nibpMean),
                DkssUtil.constructPayload(Protocol.ID_NIBP_DIA,nibpDia));

        return ret;
    }

    private boolean parseIBP(byte[] data,int offset,int modLen,Payload payload){
        //                                    P1           P2               ART               CVP               PA           RAP        LAP         ICP
        byte[][] sysID  = new byte[][]{Protocol.ID_IBP_P1_SYS,Protocol.ID_IBP_P2_SYS,Protocol.ID_IBP_ART_SYS,Protocol.ID_IBP_CVP_SYS,
                Protocol.ID_IBP_PA_SYS,Protocol.ID_IBP_RAP_SYS,Protocol.ID_IBP_LAP_SYS,Protocol.ID_IBP_ICP_SYS};
        byte[][] meanID  = new byte[][]{Protocol.ID_IBP_P1_MEAN,Protocol.ID_IBP_P2_MEAN,Protocol.ID_IBP_ART_MEAN,Protocol.ID_IBP_CVP_MEAN,
                Protocol.ID_IBP_PA_MEAN,Protocol.ID_IBP_RAP_MEAN,Protocol.ID_IBP_LAP_MEAN,Protocol.ID_IBP_ICP_MEAN};
        byte[][] diaID  = new byte[][]{Protocol.ID_IBP_P1_DIA,Protocol.ID_IBP_P2_DIA,Protocol.ID_IBP_ART_DIA,Protocol.ID_IBP_CVP_DIA,
                Protocol.ID_IBP_PA_DIA,Protocol.ID_IBP_RAP_DIA,Protocol.ID_IBP_LAP_DIA,Protocol.ID_IBP_ICP_DIA};
        byte[][] waveID  = new byte[][]{Protocol.ID_IBP_P1_WAVE,Protocol.ID_IBP_P2_WAVE,Protocol.ID_IBP_ART_WAVE,Protocol.ID_IBP_CVP_WAVE,
                Protocol.ID_IBP_PA_WAVE,Protocol.ID_IBP_RAP_WAVE,Protocol.ID_IBP_LAP_WAVE,Protocol.ID_IBP_ICP_WAVE};
        int index = -1;
        int fixedLen = 11;
        int waveSize = (modLen-fixedLen)/2;
        byte[] ibpWaveByteArr = new byte[waveSize];
        short[] ibpWaveShortArr = new short[waveSize];
        switch (data[offset+1]){
            case 20:
                index = 0;
                break;
            case 21:
                index = 1;
                break;
            case 22:
                index = 2;
                break;
            case 23:
                index = 3;
                break;
            case 24:
                index = 4;
                break;
            case 25:
                index = 5;
            case 26:
                index = 6;
                break;
            case 27:
                index = 7;
                break;
        }

        short ibpSys = (short)((data[offset + 6] * 128 + data[offset + 5])*0.1-100);
        short ibpMean = (short)((data[offset + 8] * 128 + data[offset + 7])*0.1-100);
        short ibpDia = (short)((data[offset + 10] * 128 + data[offset + 9])*0.1-100);

        for(int i=0;i<waveSize;i++){
            ibpWaveShortArr[i] = (short)(data[offset+fixedLen+i*2+1]*128+data[offset+fixedLen+i*2]);
            ibpWaveByteArr[i] = (byte)(ibpWaveShortArr[i]>>5);
        }

        boolean ret = payload.add(
                DkssUtil.constructPayload(sysID[index],ibpSys),
                DkssUtil.constructPayload(meanID[index],ibpMean),
                DkssUtil.constructPayload(diaID[index],ibpDia),
                DkssUtil.constructPayload(waveID[index],ibpWaveByteArr)
        );

        return ret;
    }

    private byte[] parseCO2(byte[] data,int offset,int modLen){
        return null;
    }

    private byte[] parseO2(byte[] data,int offset,int modLen){
        return null;
    }

    private byte[] parseN2O(byte[] data,int offset,int modLen){
        return null;
    }

    private byte[] parseAA(byte[] data,int offset,int modLen){
        return null;
    }

    private byte[] parseICG(byte[] data,int offset,int modLen){
        return null;
    }

    private byte[] parseFC(byte[] data,int offset,int modLen){
        return null;
    }

    private byte[] parseFD(byte[] data,int offset,int modLen){
        return null;
    }

    private byte[] parseFE(byte[] data,int offset,int modLen){
        return null;
    }
}
