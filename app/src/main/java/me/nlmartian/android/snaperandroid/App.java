package me.nlmartian.android.snaperandroid;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Handler;

import com.github.curioustechizen.xlog.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

import me.nlmartian.android.snaperandroid.util.Logger;

/**
 * Created by nlmartian on 5/26/15.
 */
public class App extends Application {
    public static final String TAG = "APP";

    public static volatile Handler mainHandler = null;
    public static volatile Context applicationContext = null;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(this.getMainLooper());
        applicationContext = getApplicationContext();

        initFileLog();

        copyWatchdog();


    }


    public static void startPushService() {

        if (!isServiceRun(applicationContext, "me.nlmartian.android.snaperandroid.MessageService2")) {
            Logger.d(TAG, "start message service *---------------->   *------------->   *----------->");
            applicationContext.startService(new Intent(applicationContext, MessageService2.class));
        } else {
            Logger.d(TAG, "message service is running <----------------*   <-------------*   <-----------*");
        }

        if (android.os.Build.VERSION.SDK_INT >= 19) {

//            PendingIntent pintent = PendingIntent.getService(applicationContext, 0, new Intent(applicationContext, MessageService2.class), 0);
//            AlarmManager alarm = (AlarmManager)applicationContext.getSystemService(Context.ALARM_SERVICE);
//            alarm.cancel(pintent);
        }
    }

    private void initFileLog() {
        File sdCard = App.applicationContext.getExternalFilesDir(null);
        if (sdCard == null) {
            return;
        }
        File dir = new File(sdCard.getAbsolutePath() + "/logs");
        if (dir == null) {
            return;
        }
        dir.mkdirs();
        File logFile = null;
        logFile = new File(dir, System.currentTimeMillis() + ".txt");
        if (logFile == null) {
            return;
        }

        try {
            Log.init(this, true, logFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isServiceRun(Context mContext, String className) {
        boolean isRun = false;
        ActivityManager activityManager = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager
                .getRunningServices(300);
        int size = serviceList.size();
        for (int i = 0; i < size; i++) {
            if (serviceList.get(i).service.getClassName().equals(className)) {
                isRun = true;
                break;
            }
        }
        return isRun;
    }

    public static int getServicePid(Context mContext, String className) {
        ActivityManager activityManager = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager
                .getRunningServices(300);
        int size = serviceList.size();
        for (int i = 0; i < size; i++) {
            ActivityManager.RunningServiceInfo serviceInfo = serviceList.get(i);
            if (serviceInfo.service.getClassName().equals(className)) {
                return serviceInfo.pid;
            }
        }
        return 0;
    }

    private void copyWatchdog() {
        File fileDir = App.applicationContext.getFilesDir();
        File watchDog = new File(fileDir.getPath() + "/watchdog");
        InputStream inputStream = null;
        OutputStream outputStream = null;
        byte[] buf = new byte[1024];
        int bytesRead;

        if (!watchDog.exists()) {
            AssetManager assetManager = getAssets();
            try {
                inputStream = assetManager.open("watchdog");
                outputStream = new FileOutputStream(watchDog);
                while ((bytesRead = inputStream.read(buf)) > 0) {
                    outputStream.write(buf, 0, bytesRead);
                }
            } catch (IOException e) {
                Logger.d(TAG, "copy watchdog failed");
            } finally {
                try {
                    inputStream.close();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Logger.d(TAG, "watchdog exists!");
        }
    }

    public static void runWatchDog(int pid) {
        File fileDir = App.applicationContext.getFilesDir();
        File watchDog = new File(fileDir.getPath() + "/watchdog");
        if (watchDog.exists()) {
            int resCode;
            resCode = runCmd("chmod 744 /data/data/me.nlmartian.android.snaperandroid/files/watchdog");
            Logger.d(TAG, "chmod res:" + resCode);
            resCode = runCmd("/data/data/me.nlmartian.android.snaperandroid/files/watchdog " + pid);
            Logger.d(TAG, "run watchdog res:" + resCode);
        }
    }

    private static int runCmd(String command) {
        Runtime runtime = Runtime.getRuntime();
        InputStream in;
        InputStreamReader is;
        BufferedReader br;
        StringBuilder sb = new StringBuilder();
        String read;

        try {
            Process proc = runtime.exec(command);
            in = proc.getErrorStream();
            is = new InputStreamReader(in);
            br = new BufferedReader(is);
            read = br.readLine();
            while(read != null) {
                sb.append(read);
                read =br.readLine();
            }
            Logger.d(TAG, "exec " + command + " stderr:" + sb.toString());
            return proc.exitValue();
        } catch (IOException e) {
            e.printStackTrace();
            Logger.e(TAG, "exec " + command + " error", e);
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e(TAG, "exec " + command + " error", e);
            return -1;
        }
    }
}
