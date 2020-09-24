package com.dkss.testmonitor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;


import com.dkss.testmonitor.service.MainService;


public class MainActivity extends Activity {

    private Intent serviceIntent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        serviceIntent = new Intent(this,MainService.class);
        startService(serviceIntent);
    }
}
