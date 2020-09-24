package com.dkss.testmonitor.autoboot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.dkss.testmonitor.service.MainService;

public class BootBroadcast extends BroadcastReceiver {

    protected static final String ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(ACTION)){
            context.startService(new Intent(context, MainService.class));
        }
    }
}
