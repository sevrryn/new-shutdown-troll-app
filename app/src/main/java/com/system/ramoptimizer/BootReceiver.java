package com.system.ramoptimizer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            SharedPreferences prefs = context.getSharedPreferences("ramopt", Context.MODE_PRIVATE);
            boolean isSet = prefs.getBoolean("isSet", false);

            if (isSet) {
                Intent serviceIntent = new Intent(context, SchedulerService.class);
                context.startForegroundService(serviceIntent);
            }
        }
    }
}
