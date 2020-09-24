package com.dkss.testmonitor.server;

import com.dkss.testmonitor.device.apm.AriconPatientMonitor;
import com.dkss.testmonitor.util.DkssUtil;
import com.dkss.testmonitor.util.Protocol;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AriconPMServer implements Runnable{

    private ServerInfo sInfo;
    private byte[][] header = new byte[3][];
    private int apmPort;

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

            tempMap = (HashMap<String, String>)cfgMap.get("apm");
            String devType = tempMap.get("dev_type");
            apmPort = Integer.parseInt(tempMap.get("apm_port"));

            if(sIp ==null || devType == null || boxID ==null || boxType ==null){
                throw  new NumberFormatException();
            }

            sInfo = new ServerInfo(sIp,sPort,sReadTimeout,sConnectTimeout);
            header[0] = DkssUtil.constructPayload(Protocol.ID_BOX_TYPE,boxType);
            header[1] = DkssUtil.constructPayload(Protocol.ID_BOX_ID,boxID);
            header[2] = DkssUtil.constructPayload(Protocol.ID_DEV_TYPE,devType);


        }catch (NumberFormatException e){
            System.err.println("apm 配置文件出错，请检查配置文件");
            return false;
        }
        return true;
    }
    @Override
    public void run() {
        try {
            ServerSocket ss = null;

            // ss = new ServerSocket(5003);
            ss = new ServerSocket(5000+apmPort);

            // monitor the connect request
            while (true) {
                System.out.println("服务器等待...");
                Socket socket = ss.accept();
                AriconPatientMonitor apm = new AriconPatientMonitor(socket,sInfo,header);
                System.out.println("启动线程，处理请求");
                new Thread(apm).start();
            }

        } catch (Exception e) {
            System.err.println("timeout");
            e.printStackTrace();
        }
    }
}
