package com.dkss.testmonitor.device.max485;

import android.util.Log;

import com.dkss.testmonitor.device.BasicDevice;
import com.dkss.testmonitor.serial.Max485Serial;
import com.dkss.testmonitor.server.ServerInfo;
import com.dkss.testmonitor.util.DkssUtil;
import com.dkss.testmonitor.util.Payload;
import com.dkss.testmonitor.util.Protocol;
import com.dkss.testmonitor.util.SerialUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Max485 extends BasicDevice {

    private ArrayList<byte[]> bufDataQueue;

    private byte[] boxIDPayload; // 盒子ID
    private byte[] boxTypePayload;   //盒子类型
    private byte[] vDevTypePayload;   //电量仪设备类型负载
    private byte[] oDevTypePayload;   //氧气设备类型负载
    private float vSleepTime;
    private float oSleepTime;
    private int vFTT;
    private int oFTT;

    private ServerInfo sInfo;

    private static final int VOLTAGE_RANGE = 250; //单位V
    private static final int CURRENT_RANGE = 10;  //单位A

    public boolean init(Map<String,Object> cfgMap){
        try{
            HashMap<String,String> tempMap = (HashMap)cfgMap.get("box");
            String boxID = tempMap.get("box_id");
            String boxType = tempMap.get("box_type");

            tempMap = (HashMap)cfgMap.get("server_host");
            String sIp = tempMap.get("host");
            int sPort = Integer.parseInt(tempMap.get("port"));
            int sReadTimeout = Integer.parseInt(tempMap.get("read_timeout"));
            int sConnectTimeout = Integer.parseInt(tempMap.get("connect_timeout"));

            tempMap = (HashMap<String, String>)cfgMap.get("vor");
            String vDevType = tempMap.get("dev_type");
            vSleepTime = Float.parseFloat(tempMap.get("sleep_time"));
            vFTT = Integer.parseInt(tempMap.get("FTT"));

            tempMap = (HashMap<String, String>)cfgMap.get("oxy");
            String oDevType = tempMap.get("dev_type");
            oSleepTime = Float.parseFloat(tempMap.get("sleep_time"));
            oFTT = Integer.parseInt(tempMap.get("FTT"));

            if(sIp ==null || vDevType == null || oDevType == null || boxID ==null || boxType ==null){
                throw  new NumberFormatException();
            }

            sInfo = new ServerInfo(sIp,sPort,sReadTimeout,sConnectTimeout);
            boxIDPayload = DkssUtil.constructPayload(Protocol.ID_BOX_ID,boxID);
            boxTypePayload = DkssUtil.constructPayload(Protocol.ID_BOX_TYPE,boxType);
            oDevTypePayload = DkssUtil.constructPayload(Protocol.ID_DEV_TYPE,oDevType);
            vDevTypePayload = DkssUtil.constructPayload(Protocol.ID_DEV_TYPE,vDevType);

        }catch (NumberFormatException e){
            System.err.println("vor和oxy配置文件有误");
            return false;
        }
        return true;
    }

    private int[] readFromSerial(Max485Serial serial,int[] cmd,int readLen,int faultTolerant,float sleepTime){
        int count = 0;
        while(count++ < faultTolerant){

            serial.write(cmd,cmd.length,sleepTime);
            int[] buffer = serial.read();

            if(buffer ==null){
                System.out.println("buffer is null");
                continue;
            }

            //判断校验和是否对不对
            if(!SerialUtil.getCRC(buffer).equals("0")){
                System.out.println("crc error");
                continue;
            }
            return buffer;
        }
        return null;
    }

    private boolean readVolta(Max485Serial serial, int[] cmd, int readLen, Payload payload){
        int[] data = readFromSerial(serial,cmd,readLen, vFTT, vSleepTime);
        if(data == null){
            System.out.println("data is null");
            return false;
        }

        byte[][] id = {Protocol.ID_VOR_0, Protocol.ID_VOR_1, Protocol.ID_VOR_2, Protocol.ID_VOR_3, Protocol.ID_VOR_4, Protocol.ID_VOR_5, Protocol.ID_VOR_6,
                Protocol.ID_VOR_7, Protocol.ID_VOR_8, Protocol.ID_VOR_9, Protocol.ID_VOR_10, Protocol.ID_VOR_11, Protocol.ID_VOR_12, Protocol.ID_VOR_13};

        int len = 14;
        float[] vArr = new float[len];
        //电压
        vArr[0] = ( (float)(data[3]*256+data[4]) * VOLTAGE_RANGE ) / 10000;
        //电流
        vArr[1] = ( (float)(data[5]*256+data[6]) * CURRENT_RANGE ) / 10000;
        //有功功率
        vArr[2] = ( (float)(data[7]*256+data[8]) * CURRENT_RANGE * VOLTAGE_RANGE ) / 10000;
        //无功功率
        vArr[3] = ( (float)(data[9]*256+data[10] ) * CURRENT_RANGE * VOLTAGE_RANGE ) / 10000;
        //功率因数
        vArr[4] = ( (float)(data[11]*256+data[12]) ) / 10000;
        //F频率
        vArr[5] = ( (float)(data[13]*256+data[14]) ) / 100;
        //正向有功电度
        vArr[6] = ( (float)(data[15]*256+data[16]) * VOLTAGE_RANGE * CURRENT_RANGE ) / (1000*3600);
        //正向无功电度
        vArr[7] = ( (float)(data[17]*256+data[18]) * VOLTAGE_RANGE * CURRENT_RANGE ) / (1000*3600);
        //反向有功电度
        vArr[8] = ( (float)(data[19]*256+data[20]) * VOLTAGE_RANGE * CURRENT_RANGE ) / (1000*3600);
        //反向无功电度
        vArr[9] = ( (float)(data[21]*256+data[22]) * VOLTAGE_RANGE * CURRENT_RANGE ) / (1000*3600);
        //视在功率
        vArr[10] = ( (float)(data[23]*256+data[24] ) * CURRENT_RANGE * VOLTAGE_RANGE ) / 10000;
        //谐波有功功率
        vArr[11] = ( (float)(data[25]*256+data[26] ) * CURRENT_RANGE * VOLTAGE_RANGE ) / 10000;
        //基波有功功率
        vArr[12] = ( (float)(data[27]*256+data[28] ) * CURRENT_RANGE * VOLTAGE_RANGE ) / 10000;
        //基波无功功率
        vArr[13] = ( (float)(data[29]*256+data[30] ) * CURRENT_RANGE * VOLTAGE_RANGE ) / 10000;

        for(int i=0;i<len;i++){
            payload.add(DkssUtil.constructPayload(id[i],vArr[i]));
        }

        return true;
    }

    private boolean readOxygen(Max485Serial serial, int[][] cmd, int readLen,Payload payload){
        int[] data = readFromSerial(serial,cmd[0],readLen, oFTT, oSleepTime);
        if(data == null){
            return false;
        }
        float oxyA = ((float)(data[3]*256+data[4])) /100;

        data = readFromSerial(serial,cmd[0],readLen, oFTT, oSleepTime);
        if(data == null){
            return false;
        }
        float oxyB = ((float)(data[3]*256+data[4])) /100;

        payload.add(DkssUtil.constructPayload(Protocol.ID_OXY_A,oxyA),
                DkssUtil.constructPayload(Protocol.ID_OXY_B,oxyB));

        return true;
    }

    @Override
    public void run() {
        Max485Serial serial = new Max485Serial();

        int ret = -1;
        try {
            ret = serial.open(1, 9600, 8, 'N', 1);
        }catch (Exception e){
            System.err.println("无法打开485串口");
        }
        if(ret<0){
            Log.i("serial485","串口打开失败");
            return;
        }

        Payload payload  = new Payload();
        //注册设备
        payload.add(boxTypePayload,boxIDPayload,vDevTypePayload,DkssUtil.getTimePayload());
        registDev(sInfo,payload);
        payload.clear();
        payload.add(boxTypePayload,boxIDPayload,oDevTypePayload,DkssUtil.getTimePayload());
        registDev(sInfo,payload);
        payload.clear();

        int[] vCmd = {3,3,0,0,0,15,4,44};
        int[][] oCmd = {{1,3,0,1,0,1,213,202},{2,3,0,1,0,1,213,249}};

        int vDataLen = 35;
        int oDataLen = 8;

        while(true){
            //读取电量仪数据
            byte[] timePayload = DkssUtil.getTimePayload();
            readVolta(serial,vCmd,vDataLen,payload);
            if(payload.getNum()!=0){

                payload.add(0, timePayload);
                payload.add(0,vDevTypePayload);
                payload.add(0,boxIDPayload);
                payload.add(0,boxTypePayload);
                byte[] buf = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData());
                addBuf(bufDataQueue,buf);
                payload.clear();
            }

            //读取氧气设备数据
            readOxygen(serial,oCmd,oDataLen,payload);

            if(payload.getNum()!=0){
                payload.add(0,timePayload);
                payload.add(0,oDevTypePayload);
                payload.add(0,boxIDPayload);
                payload.add(0,boxTypePayload);

                byte[] buf = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData());
                addBuf(bufDataQueue,buf);
                payload.clear();
            }

            //刷写缓冲区
            flushBuf(sInfo,bufDataQueue);

        }
    }




}
