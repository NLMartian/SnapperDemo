package me.nlmartian.android.snaperandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import me.nlmartian.android.snaperandroid.util.Logger;

/**
 * Created by nlmartian on 6/19/15.
 */
public class ServiceStartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.d("ServiceStartReceiver", intent.getAction());
        App.mainHandler.post(new Runnable() {
            @Override
            public void run() {
                App.startPushService();
            }
        });
    }
}
