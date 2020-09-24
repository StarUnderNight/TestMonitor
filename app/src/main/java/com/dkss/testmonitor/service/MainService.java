package com.dkss.testmonitor.service;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.dkss.testmonitor.R;
import com.dkss.testmonitor.device.max485.Max485;
import com.dkss.testmonitor.device.pmr.PatientMonitor;
import com.dkss.testmonitor.server.AriconPMServer;
import com.dkss.testmonitor.server.RespiratorServer;
import com.dkss.testmonitor.util.IniConfig;

import java.util.Map;

public class MainService extends Service {
    private Context context;
    private String notifyContext = "成功启动程序";

    public MainService() {
        context = this;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        boolean ret = runProgram(context);
        if(!ret ){
            notifyContext = "配置文件错误";
        }

        Notification notification = new NotificationCompat.Builder(this, "110")
                .setContentTitle("medical")
                .setContentText(notifyContext)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .build();

        startForeground(1, notification);
    }


    @Override
    public void onDestroy() {
        Toast.makeText(this,"service stop",Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }




    private boolean runProgram(Context context) {


//        String cfgPath = null;
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
//             cfgPath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)+"/dkss_medical.ini";
//        }
//        if(cfgPath == null){
//            return false;
//        }
//
//        Map<String,Object> cfgMap  = null;
//        try{
//            cfgMap  =  IniConfig.readIni(new FileInputStream(cfgPath));
//        }catch (Exception e){
//            e.printStackTrace();
//            return false;
//        }



        //用这种方式，方便修改配置文件
        AssetManager assetManager = getAssets();

        Map<String,Object> cfgMap = null;
        try {
            cfgMap = IniConfig.readIni(assetManager.open("dkss_medical.ini"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        PatientMonitor pmr = new PatientMonitor();
        if(pmr.init(cfgMap)){
            System.out.println("pmr配置文件缺少");
            return false;
        }
       // new Thread(pmr).start();

        AriconPMServer apmServer = new AriconPMServer();
        if(!apmServer.init(cfgMap)){
            return  false;
        }
        new Thread(apmServer).start();


        return true;
    }


}
