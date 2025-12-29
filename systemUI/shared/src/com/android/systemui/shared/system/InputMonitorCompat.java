/**
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.systemui.shared.system;

import android.hardware.input.InputManager;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;
import android.view.Choreographer;
import android.view.InputMonitor;
import android.view.SurfaceControl;

import androidx.annotation.NonNull;

import com.android.systemui.shared.system.InputChannelCompat.InputEventListener;
import com.android.systemui.shared.system.InputChannelCompat.InputEventReceiver;

import java.lang.reflect.Method;

/**
 * @see android.view.InputMonitor
 */
public class InputMonitorCompat {
    static final String TAG = "InputMonitorCompat";
    private final InputMonitor mInputMonitor;
    private final String mName;

    /**
     * Monitor input on the specified display for gestures.
     */
    public InputMonitorCompat(@NonNull String name, int displayId) {
        mName = name + "-disp" + displayId;
        mInputMonitor = getInputMonitor(name, displayId);
        if (mInputMonitor != null) {
            Trace.instant(Trace.TRACE_TAG_INPUT, "InputMonitorCompat-" + mName + " created");
            Log.d(TAG, "Input monitor (" + mName + ") created");
        } else {
            Log.w(TAG, "Input monitor (" + mName + ") could not be created - API not available");
        }
    }

    /**
     * Check if the input monitor was successfully created.
     */
    public boolean isValid() {
        return mInputMonitor != null;
    }

    /**
     * Gets an InputMonitor using the appropriate API for the Android version.
     * Falls back to reflection for Android 13 compatibility.
     */
    private static InputMonitor getInputMonitor(String name, int displayId) {
        // Try InputManagerGlobal.monitorGestureInput first (Android 14+)
        try {
            Class<?> inputManagerGlobalClass = Class.forName("android.hardware.input.InputManagerGlobal");
            Method getInstanceMethod = inputManagerGlobalClass.getMethod("getInstance");
            Object inputManagerGlobal = getInstanceMethod.invoke(null);
            Method monitorMethod = inputManagerGlobalClass.getMethod("monitorGestureInput", String.class, int.class);
            return (InputMonitor) monitorMethod.invoke(inputManagerGlobal, name, displayId);
        } catch (Exception e) {
            Log.d(TAG, "InputManagerGlobal.monitorGestureInput not available");
        }

        // Try InputManager.monitorGestureInput (some Android versions)
        try {
            InputManager inputManager = InputManager.getInstance();
            Method monitorMethod = InputManager.class.getMethod("monitorGestureInput", String.class, int.class);
            return (InputMonitor) monitorMethod.invoke(inputManager, name, displayId);
        } catch (Exception e) {
            Log.d(TAG, "InputManager.monitorGestureInput not available");
        }

        // Try InputManager.createInputMonitor (Android 13 and earlier)
        try {
            InputManager inputManager = InputManager.getInstance();
            Method monitorMethod = InputManager.class.getMethod("createInputMonitor", String.class, int.class);
            return (InputMonitor) monitorMethod.invoke(inputManager, name, displayId);
        } catch (Exception e) {
            Log.d(TAG, "InputManager.createInputMonitor not available");
        }

        // If no method works, return null and let callers handle gracefully
        Log.e(TAG, "No InputMonitor API available on this Android version");
        return null;
    }

    /**
     * @see InputMonitor#pilferPointers()
     */
    public void pilferPointers() {
        if (mInputMonitor != null) {
            mInputMonitor.pilferPointers();
        }
    }

    /**
     * @see InputMonitor#getSurface()
     */
    public SurfaceControl getSurface() {
        return mInputMonitor != null ? mInputMonitor.getSurface() : null;
    }

    /**
     * @see InputMonitor#dispose()
     */
    public void dispose() {
        if (mInputMonitor != null) {
            mInputMonitor.dispose();
            Trace.instant(Trace.TRACE_TAG_INPUT, "InputMonitorCompat-" + mName + " disposed");
            Log.d(TAG, "Input monitor (" + mName + ") disposed");
        }
    }

    /**
     * @see InputMonitor#getInputChannel()
     */
    public InputEventReceiver getInputReceiver(Looper looper, Choreographer choreographer,
            InputEventListener listener) {
        if (mInputMonitor == null) {
            Log.w(TAG, "Input monitor not available, returning null receiver");
            return null;
        }
        Trace.instant(Trace.TRACE_TAG_INPUT, "InputMonitorCompat-" + mName + " receiver created");
        Log.d(TAG, "Input event receiver for monitor (" + mName + ") created");
        return new InputEventReceiver(mName, mInputMonitor.getInputChannel(), looper, choreographer,
                listener);
    }
}
