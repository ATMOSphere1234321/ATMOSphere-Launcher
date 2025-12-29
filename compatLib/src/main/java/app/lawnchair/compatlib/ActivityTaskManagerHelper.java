package app.lawnchair.compatlib;

import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Helper class to access ActivityTaskManager.getService() using reflection
 * to avoid NoSuchMethodError on older Android versions.
 */
public class ActivityTaskManagerHelper {
    private static final String TAG = "ATMHelper";
    private static IActivityTaskManager sService;
    private static boolean sInitialized = false;

    /**
     * Gets IActivityTaskManager service using reflection.
     * Caches the result for subsequent calls.
     *
     * @return IActivityTaskManager instance, or null if not available
     */
    public static synchronized IActivityTaskManager getService() {
        if (!sInitialized) {
            try {
                Method getServiceMethod = ActivityTaskManager.class.getMethod("getService");
                sService = (IActivityTaskManager) getServiceMethod.invoke(null);
            } catch (Exception e) {
                Log.w(TAG, "ActivityTaskManager.getService() not available", e);
                sService = null;
            }
            sInitialized = true;
        }
        return sService;
    }
}
