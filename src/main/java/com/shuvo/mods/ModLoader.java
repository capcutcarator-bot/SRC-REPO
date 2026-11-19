package com.shuvo.mods;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

public class ModLoader {
    private static final String TAG = "SHUVOxMODS_Loader";

    public static void attach(Context ctx) {
        try {
            Log.i(TAG, "SHUVOxMODS attach called");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(ctx)) {
                Log.w(TAG, "No overlay permission");
            }
            Intent svc = new Intent(ctx, ModService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(svc);
            } else {
                ctx.startService(svc);
            }
            Log.i(TAG, "ModService started");
        } catch (Throwable t) {
            Log.e(TAG, "attach failed: " + t.getMessage());
        }
    }
}
