package com.shuvo.mods;

import android.content.Context;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedEntry implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpp) {
        if (!"com.DVloper.Granny3".equals(lpp.packageName)) return;
        XposedHelpers.findAndHookMethod("android.app.Application", lpp.classLoader, "attach",
                Context.class, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        ModLoader.attach((Context) param.args[0]);
                    }
                });
    }
}
