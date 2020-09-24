package com.dkss.testmonitor.device;

import com.dkss.testmonitor.server.ServerInfo;
import com.dkss.testmonitor.util.Payload;

import java.util.ArrayList;
import java.util.Map;

public interface Device {
    boolean init(Map<String, Object> cfgMap);
    void  parse(byte[] packet, ArrayList<byte[]> bufQueue);
    void registDev(ServerInfo info, Payload payload);
    void flushBuf(ServerInfo info, ArrayList<byte[]> bufQueue);
    int addBuf(ArrayList<byte[]> bufQueue,byte[] data);
}

