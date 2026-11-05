package com.google.android.gms.common;

import android.app.Application;
import java.lang.reflect.Method;

public final class R {
    public static final class string {
        public static int common_google_play_services_unknown_issue = 1;
        
        static {
            try {
                Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
                Method currentApplicationMethod = activityThreadClass.getMethod("currentApplication");
                Application app = (Application) currentApplicationMethod.invoke(null);
                if (app != null) {
                    int resId = app.getResources().getIdentifier("common_google_play_services_unknown_issue", "string", app.getPackageName());
                    if (resId != 0) {
                        common_google_play_services_unknown_issue = resId;
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
