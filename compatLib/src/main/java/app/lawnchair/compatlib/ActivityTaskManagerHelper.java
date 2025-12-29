package app.lawnchair.compatlib;

import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.TaskInfo;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Helper class to access ActivityTaskManager methods using reflection
 * to avoid NoSuchMethodError on older Android versions.
 */
public class ActivityTaskManagerHelper {
    private static final String TAG = "ATMHelper";
    private static IActivityTaskManager sService;
    private static boolean sServiceInitialized = false;
    private static ActivityTaskManager sInstance;
    private static boolean sInstanceInitialized = false;
    private static Method sGetRootTaskInfoMethod;
    private static boolean sGetRootTaskInfoMethodInitialized = false;

    /**
     * Gets IActivityTaskManager service using reflection.
     * Caches the result for subsequent calls.
     *
     * @return IActivityTaskManager instance, or null if not available
     */
    public static synchronized IActivityTaskManager getService() {
        if (!sServiceInitialized) {
            try {
                Method getServiceMethod = ActivityTaskManager.class.getMethod("getService");
                sService = (IActivityTaskManager) getServiceMethod.invoke(null);
            } catch (Exception e) {
                Log.w(TAG, "ActivityTaskManager.getService() not available", e);
                sService = null;
            }
            sServiceInitialized = true;
        }
        return sService;
    }

    /**
     * Gets ActivityTaskManager instance using reflection.
     * Caches the result for subsequent calls.
     *
     * @return ActivityTaskManager instance, or null if not available
     */
    public static synchronized ActivityTaskManager getInstance() {
        if (!sInstanceInitialized) {
            try {
                Method getInstanceMethod = ActivityTaskManager.class.getMethod("getInstance");
                sInstance = (ActivityTaskManager) getInstanceMethod.invoke(null);
            } catch (Exception e) {
                Log.w(TAG, "ActivityTaskManager.getInstance() not available", e);
                sInstance = null;
            }
            sInstanceInitialized = true;
        }
        return sInstance;
    }

    /**
     * Safely gets root task info using reflection.
     * Returns null if the method is not available or fails.
     *
     * @param windowingMode The windowing mode to query
     * @param activityType The activity type to query
     * @return TaskInfo (RootTaskInfo) or null if not available
     */
    public static synchronized TaskInfo getRootTaskInfo(int windowingMode, int activityType) {
        IActivityTaskManager service = getService();
        if (service == null) {
            return null;
        }

        if (!sGetRootTaskInfoMethodInitialized) {
            try {
                sGetRootTaskInfoMethod = IActivityTaskManager.class.getMethod(
                        "getRootTaskInfo", int.class, int.class);
            } catch (NoSuchMethodException e) {
                Log.w(TAG, "getRootTaskInfo method not available", e);
                sGetRootTaskInfoMethod = null;
            }
            sGetRootTaskInfoMethodInitialized = true;
        }

        if (sGetRootTaskInfoMethod == null) {
            return null;
        }

        try {
            return (TaskInfo) sGetRootTaskInfoMethod.invoke(service, windowingMode, activityType);
        } catch (Exception e) {
            Log.w(TAG, "Failed to call getRootTaskInfo", e);
            return null;
        }
    }
}
