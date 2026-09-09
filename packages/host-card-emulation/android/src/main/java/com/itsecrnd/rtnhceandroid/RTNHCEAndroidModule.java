package com.itsecrnd.rtnhceandroid;

import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;

public class RTNHCEAndroidModule extends NativeHCEModuleSpec {
    public static final String TAG = "RTNHCEAndroidModule";
    public static final String NAME = "NativeHCEModule";

    private volatile boolean sessionRunning;
    private volatile boolean hceRunning;
    private volatile boolean hceDeselected;
    private volatile boolean hceBrokenConnection;

    private volatile boolean hceBackgroundReady;

    private HCEServiceCallback serviceCb;

    RTNHCEAndroidModule(ReactApplicationContext context) {
        super(context);

        this.serviceCb = null;

        this.sessionRunning = false;
        this.hceRunning = false;
        this.hceDeselected = true;
        this.hceBrokenConnection = false;
        this.hceBackgroundReady = false;
    }

    @Override
    public void invalidate() {
        Log.d(TAG, "RTNHCEAndroidModule:invalidate");
    }

    @Override
    @NonNull
    public String getName() {
        Log.d(TAG, "RTNHCEAndroidModule:getName");
        return RTNHCEAndroidModule.NAME;
    }

    void sendEvent(final String type, final String arg) {
        Log.d(TAG, "RTNHCEAndroidModule:sendEvent");

        if (this.mEventEmitterCallback != null) {
            WritableMap map = Arguments.createMap();
            map.putString("type", type);
            map.putString("arg", arg);
            emitOnEvent(map);
        } else {
            Log.d(TAG, "RTNHCEAndroidModule:sendEvent missing event emitter, ignore");
        }
    }

    void sendBackgroundEvent(final String audience, final String type, final String arg) {
        Log.d(TAG, "RTNHCEAndroidModule:sendBackgroundEvent");

        if (this.mEventEmitterCallback != null) {
            WritableMap map = Arguments.createMap();
            map.putString("audience", audience);
            map.putString("type", type);
            map.putString("arg", arg);
            emitOnBackgroundEvent(map);
        } else {
            Log.d(TAG, "RTNHCEAndroidModule:sendBackgroundEvent missing event emitter, ignore");
        }
    }

    boolean _isHCERunning() {
        return this.hceRunning;
    }

    void setHCEBrokenConnection() {
        this.hceBrokenConnection = true;
    }

    boolean isHCEBackgroundHandlerReady() {
        return this.hceBackgroundReady;
    }

    boolean isHCEActiveConnection() {
        return !this.hceBrokenConnection;
    }

    void setHCEService(HCEServiceCallback serviceCallback) {
        Log.d(TAG, "RTNHCEAndroidModule:setHCEService");

        this.serviceCb = serviceCallback;
        this.hceDeselected = true;
        this.hceBackgroundReady = false;
        this.hceBrokenConnection = false;
    }

    @Override
    public boolean isPlatformSupported() {
        Log.d(TAG, "RTNHCEAndroidModule:isPlatformSupported");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }

        PackageManager pm = getReactApplicationContext().getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_NFC)
                && pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);
    }

    @Override
    public void acquireExclusiveNFC(Promise promise) {
        Log.d(TAG, "RTNHCEAndroidModule:acquireExclusiveNFC");
        promise.reject("err_platform_unsupported", "acquireExclusiveNFC() is not supported on Android.");
    }

    @Override
    public void releaseExclusiveNFC() {
        Log.d(TAG, "RTNHCEAndroidModule:releaseExclusiveNFC");
        // unsupported on Android, no-op
    }

    @Override
    public boolean isExclusiveNFC() {
        Log.d(TAG, "RTNHCEAndroidModule:isExclusiveNFC");
        // unsupported on Android, always false
        return false;
    }

    @Override
    public void beginSession(Promise promise) {
        Log.d(TAG, "RTNHCEAndroidModule:beginSession");

        if (!isPlatformSupported()) {
            promise.reject("err_platform_unsupported", "Unsupported Android version or missing NFC/HCE feature.");
            return;
        }

        if (!this.sessionRunning) {
            this.sessionRunning = true;
            sendEvent("sessionStarted", "");
            promise.resolve(null);
        } else {
            promise.reject("err_card_session_exists", "Session already exists.");
        }
    }

    @Override
    public void setSessionAlertMessage(String message) {
        Log.d(TAG, "RTNHCEAndroidModule:setSessionAlertMessage");
        // unsupported on Android, no-op
    }

    @Override
    public void invalidateSession() {
        Log.d(TAG, "RTNHCEAndroidModule:invalidateSession");
        if (this.sessionRunning) {
            this.hceRunning = false;
            this.sessionRunning = false;
            this.hceBrokenConnection = true;
            sendEvent("sessionInvalidated", "userInvalidated");
        }
    }

    @Override
    public boolean isSessionRunning() {
        Log.d(TAG, "RTNHCEAndroidModule:isSessionRunning");
        return this.sessionRunning;
    }

    @Override
    public boolean beginBackgroundHCE(String handle) {
        Log.d(TAG, "RTNHCEAndroidModule:beginBackgroundHCE");

        try {
            this.serviceCb.onBackgroundHCEInit(handle);
        } catch (IllegalStateException e) {
            return false;
        }

        this.hceBackgroundReady = true;
        return true;
    }

    @Override
    public boolean finishBackgroundHCE(String handle) {
        Log.d(TAG, "RTNHCEAndroidModule:finishBackgroundHCE");

        try {
            this.serviceCb.onBackgroundHCEFinish(handle);
        } catch (IllegalStateException e) {
            return false;
        }

        return true;
    }

    @Override
    public void startHCE(Promise promise) {
        Log.d(TAG, "RTNHCEAndroidModule:startHCE");
        if (!this.sessionRunning) {
            promise.reject("err_no_session", "No session is active.");
            return;
        }

        if (this.hceRunning) {
            promise.reject("err_start_emulation", "Error trying to start emulation. Emulation already started.");
            return;
        }

        this.hceRunning = true;
        this.hceBrokenConnection = false;
        promise.resolve(null);
    }

    @Override
    public void stopHCE(String status, Promise promise) {
        Log.d(TAG, "RTNHCEAndroidModule:stopHCE");
        if (!this.sessionRunning) {
            promise.reject("err_no_session", "No session is active.");
            return;
        }

        if (this.hceRunning) {
            this.hceRunning = false;

            if (!this.hceDeselected) {
                this.hceDeselected = true;
                this.hceBrokenConnection = true;
                sendEvent("readerDeselected", "");
            }
        }

        promise.resolve(null);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void respondAPDU(String handle, String rapdu, Promise promise) {
        Log.d(TAG, "RTNHCEAndroidModule:respondAPDU");

        if (!this.hceBackgroundReady) {
            if (!this.sessionRunning) {
                promise.reject("err_no_session", "No session is active.");
                return;
            } else if (!this.hceRunning) {
                promise.reject("err_no_hce", "HCE is not running.");
                return;
            }
        }

        Log.d(TAG, "respondAPDU(): Send to service");

        try {
            serviceCb.onRespondAPDU(handle, rapdu);
        } catch (IllegalStateException e) {
            promise.reject("err_illegal_state", "There is no C-APDU to respond to at the time or the provided handle is invalid.");
            return;
        }

        promise.resolve(null);
    }

    @Override
    public void isHCERunning(Promise promise) {
        Log.d(TAG, "RTNHCEAndroidModule:isHCERunning");
        promise.resolve(this.hceRunning);
    }
}
