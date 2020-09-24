package com.dkss.testmonitor.device.pmr;

import com.dkss.testmonitor.device.BasicDevice;
import com.dkss.testmonitor.server.ServerInfo;
import com.dkss.testmonitor.util.DkssUtil;
import com.dkss.testmonitor.util.PMUtil;
import com.dkss.testmonitor.util.Protocol;

import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PatientMonitor extends BasicDevice {
    private static final int BROAD_PORT = 8002;
    public volatile boolean exit = false;
    private byte cMachineNO;   //板子的机器号
    private ServerInfo sInfo;   //远程服务器的信息

    private byte[] boxIDPayload;
    private byte[] boxTypePayload;
    private byte[] devTypePayload;

    private byte[] broadData;
    private byte pMachineNO;
    private String pIP;
    private byte pVersion;







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

            tempMap = (HashMap<String, String>)cfgMap.get("pmr");
            String devType = tempMap.get("dev_type");
            cMachineNO = Byte.parseByte(tempMap.get("machine_num"));

            if(sIp ==null || devType == null || boxID ==null || boxType ==null){
                throw  new NumberFormatException();
            }

            sInfo = new ServerInfo(sIp,sPort,sReadTimeout,sConnectTimeout);
            boxIDPayload = DkssUtil.constructPayload(Protocol.ID_BOX_ID,boxID);
            boxTypePayload = DkssUtil.constructPayload(Protocol.ID_BOX_TYPE,boxType);
            devTypePayload = DkssUtil.constructPayload(Protocol.ID_DEV_TYPE,devType);

        }catch (NumberFormatException e){
            System.err.println("pmr配置文件有误");
            return false;
        }
        return true;
    }




    private int parsePacketDA(byte[] data) {

        int cmdNum = (data[9]&0x05)*128 + data[10];
        byte[] responseCmd = {(byte)0xff,(byte)0xda,(byte)0x7f, cMachineNO,0x00,pMachineNO,0x00,0x03,0x0a,data[9],data[10]};

        switch (cmdNum) {
            case 512://200
                //System.out.println("从机收到了我的数据请求");
                return 200;
            case 513://201
                //System.out.println("请求重发的数据不存在");
                return 201;
            case 514://202
                //System.out.println("上传大气压力,需要回复");
                return 202;
            case 515://203
                //System.out.println("收入病人，更新病人信息，需要回复");
                PMUtil.sendUDP(pIP,BROAD_PORT,responseCmd);
                return 203;
            case 516://204
                //System.out.println("接触病人并待机，需要回复");
                PMUtil.sendUDP(pIP,BROAD_PORT,responseCmd);
                return 204;
            case 517://205
                //System.out.println("心律失常数据包，需要回复");
                PMUtil.sendUDP(pIP,BROAD_PORT,responseCmd);
                // TODO处理心律失常

                return 205;
            case 518://206
                // System.out.println("从机上传配置信息，需要回复");
                PMUtil.sendUDP(pIP,BROAD_PORT,responseCmd);
                //parsePacketD4(data);
                return 206;
            case 13://206
                System.out.println("从机返回时间同步响应");
                return 13;
            default:
                System.out.println("默认");
                //DkssUtil.printByte(data);
                break;
        }
        return -1;
    }


    @Override
    public void run() {
        byte[] data = null;

        //监听8002端口，获取监护仪协议的版本号
        System.out.println("监听8002端口，获取监护仪协议的版本号");
        while (true) {
            DatagramPacket dp = PMUtil.receiveUDP(BROAD_PORT, 0);
            if (dp == null) {
                continue;
            }
            data = new byte[dp.getLength()];
            System.arraycopy(dp.getData(), 0, data, 0, data.length);

            //判断是否是监护仪的广播
            if (data[1] == (byte) 0xd0 && data[5] == (byte) 0xfe) {
                pIP = dp.getAddress().getHostAddress();
                pVersion = data[8];
                pMachineNO = data[3];
                System.out.println(pIP + String.format(" pVersion=%02x; pMachineNO=%02x", pVersion, pMachineNO));
                break;
            }
        }

        System.out.println("本地广播地址:" + PMUtil.getLocalBroadCast());
        broadData = new byte[]{(byte) 0xff, (byte) 0xd0, (byte) 0x7f, cMachineNO, 0x00, (byte) 0xfe, 0x00, 0x01, pVersion};
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String broadIP = PMUtil.getLocalBroadCast();
                    PMUtil.sendUDP(broadIP, BROAD_PORT, broadData);
                }
            }
        }).start();

        ListenMonitor monitor = new ListenMonitor(sInfo,new byte[][]{boxTypePayload,boxIDPayload,devTypePayload});
        monitor.pmrInfoMap.put("cMachineNO",cMachineNO);
        monitor.pmrInfoMap.put("pMachineNO",pMachineNO);
        monitor.pmrInfoMap.put("pIP",pIP);
        new Thread(monitor).start();


        while (true) {

            DatagramPacket dp = PMUtil.receiveUDP(BROAD_PORT, 0);

            data = new byte[dp.getLength()];
            System.arraycopy(dp.getData(), 0, data, 0, data.length);

            if(data[1] == (byte) 0xD0 && !monitor.isGetData){
                //更新ip，监护仪机器号
                pMachineNO = data[3];
                pIP = dp.getAddress().getHostAddress();
                monitor.pmrInfoMap.put("pMachineNO",pMachineNO);
                monitor.pmrInfoMap.put("pIP",pIP);
            }else if (data[1] == (byte) 0xDA) {

                int ret = parsePacketDA(data);

                if (ret == 200) {
                    System.out.println("监护仪收到了我的数据请求");
                }
                if (ret == 13) {
                    System.out.println("监护仪收到时间同步响应");
                }
            }

        }
    }

}
