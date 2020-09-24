package com.dkss.testmonitor.device;

import com.dkss.testmonitor.server.ServerInfo;
import com.dkss.testmonitor.util.DkssUtil;
import com.dkss.testmonitor.util.Payload;
import com.dkss.testmonitor.util.SocketUtil;

import java.util.ArrayList;
import java.util.Map;

public class BasicDevice implements Device,Runnable{
    protected final int bufQueueLen = 10;


    @Override
    public boolean init(Map<String, Object> cfgMap) {
        return false;
    }

    @Override
    public void parse(byte[] packet, ArrayList<byte[]> bufQueue) {


    }

    @Override
    public void flushBuf(ServerInfo info, ArrayList<byte[]> bufQueue) {
        while(bufQueue.size()>0){
            byte[] ret = SocketUtil.__DeliveryDataToServer(info,bufQueue.get(0));
            if(ret ==null){
                return ;
            }
            bufQueue.remove(0);;
        }
    }

    @Override
    public int addBuf(ArrayList<byte[]> bufQueue, byte[] data) {
        if(bufQueue.size() > bufQueueLen){
            return bufQueue.size();
        }
        if(bufQueue.size() == bufQueueLen){
            bufQueue.remove(0);
        }
        bufQueue.add(data);
        return bufQueue.size();
    }

    @Override
    public void registDev(ServerInfo info, Payload payload) {
        byte[] packet = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_REGISTER_DEV,
                payload.getNum(),payload.getData());

        while(true){
            byte[] ret = SocketUtil.__DeliveryDataToServer(info,packet);
            if(ret == null){
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if(DkssUtil.parseReply(ret)){
                System.out.println("Regist dev success");
                break;
            }
        }

    }


    @Override
    public void run() {

    }
}
