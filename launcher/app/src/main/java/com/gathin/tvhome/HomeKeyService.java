package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Map;

/**
 * Clean, lightweight, rock-solid Home key interception for Fire TV:
 * - Listens for Amazon Launcher foreground appearance via zero-latency logcat and accessibility events.
 * - Instantly redirects to CleanYourTV MainActivity with FLAG_ACTIVITY_NEW_TASK.
 * - Zero intrusive overlays, zero black screen masks, zero screen freezing.
 * - Provides 0ms instant shortcut via remote MENU (☰) and double-press BACK from any app.
 * - Completely autonomous and survives reboots permanently.
 */
public class HomeKeyService extends AccessibilityService {

    private static final String TAG = "HomeKeyService";
    private static final String AMAZON_LAUNCHER = "com.amazon.tv.launcher";
    private static final String AMAZON_HOMESTARTER = "com.amazon.firehomestarter";

    public static volatile HomeKeyService instance;

    private int[] settingsScancodes = new int[0];
    private int[] settingsKeycodes = new int[0];
    private boolean settingsPanel;
    private Map<Integer, String> appKeys = Collections.emptyMap();

    private long lastLaunchTime = 0;
    private long lastBackDownTime = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Thread logcatThread = null;
    private volatile boolean logcatRunning = false;

    public static void notifyCleanYourTVResumed() {
        // No-op hook for compatibility with MainActivity
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        settingsScancodes = getResources().getIntArray(R.array.settings_key_scancodes);
        Config config = Config.load(this);
        settingsKeycodes = config.settingsKeycodes;
        settingsPanel = config.settingsKeyPanel;
        appKeys = RemoteKeys.parseAppKeys(config.appKeys);

        try {
            AccessibilityServiceInfo info = getServiceInfo();
            if (info == null) {
                info = new AccessibilityServiceInfo();
            }
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
            Log.i(TAG, "HomeKeyService connected successfully");
        } catch (Throwable t) {
            Log.e(TAG, "setServiceInfo notice", t);
        }

        startLogcatMonitor();

        // Check if Amazon launcher is already open upon service connection
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                CharSequence pkg = root.getPackageName();
                if (pkg != null) {
                    String pkgStr = pkg.toString();
                    if (AMAZON_LAUNCHER.equals(pkgStr) || AMAZON_HOMESTARTER.equals(pkgStr)) {
                        launchCleanYourTV();
                    }
                }
                try { root.recycle(); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private void startLogcatMonitor() {
        if (logcatRunning) return;
        logcatRunning = true;
        logcatThread = new Thread(() -> {
            while (logcatRunning) {
                Process process = null;
                BufferedReader reader = null;
                try {
                    process = Runtime.getRuntime().exec(new String[]{
                            "logcat", "-v", "brief", "-b", "system", "-b", "main", "-T", "1", "-s", "ActivityManager:I"
                    });
                    reader = new BufferedReader(new InputStreamReader(process.getInputStream()), 1024);
                    String line;
                    while (logcatRunning && (line = reader.readLine()) != null) {
                        if (line.contains("cmp=com.amazon.tv.launcher") || line.contains("cmp=com.amazon.firehomestarter")) {
                            if (line.contains("START") || line.contains("category.HOME") || line.contains("HomeActivity")) {
                                handler.post(this::launchCleanYourTV);
                            }
                        }
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "Logcat monitor notice: " + t.getMessage());
                } finally {
                    if (reader != null) {
                        try { reader.close(); } catch (Throwable ignored) {}
                    }
                    if (process != null) {
                        try { process.destroy(); } catch (Throwable ignored) {}
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }
        }, "HomeLogcatMonitor");
        logcatThread.setPriority(Thread.MAX_PRIORITY);
        logcatThread.start();
    }

    private void launchCleanYourTV() {
        if (MainActivity.isForeground) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastLaunchTime < 400) {
            return;
        }
        lastLaunchTime = now;

        startMainActivityDirect();

        // Follow up with scheduled retries across Fire OS app switch window
        handler.postDelayed(() -> {
            if (!MainActivity.isForeground) startMainActivityDirect();
        }, 300);

        handler.postDelayed(() -> {
            if (!MainActivity.isForeground) startMainActivityDirect();
        }, 800);

        handler.postDelayed(() -> {
            if (!MainActivity.isForeground) startMainActivityDirect();
        }, 1800);

        handler.postDelayed(() -> {
            if (!MainActivity.isForeground) startMainActivityDirect();
        }, 5050);
    }

    private void startMainActivityDirect() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "startActivity notice", e);
        }
    }

    private boolean isSettingsKey(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_SETTINGS) return true;
        for (int code : settingsScancodes) if (event.getScanCode() == code) return true;
        for (int code : settingsKeycodes) if (event.getKeyCode() == code) return true;
        return false;
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();

        if (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_ALL_APPS) {
            if (action == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                launchCleanYourTV();
            }
            return true;
        }

        // 0ms Instant Home shortcut: Press MENU (☰) from any app to return to CleanYourTV instantly
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (!MainActivity.isForeground) {
                    startMainActivityDirect();
                    return true;
                }
            }
        }

        // Double-click BACK shortcut: Double press BACK within 450ms opens CleanYourTV instantly
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            long now = System.currentTimeMillis();
            if (action == KeyEvent.ACTION_DOWN) {
                if (now - lastBackDownTime < 450) {
                    lastBackDownTime = 0;
                    startMainActivityDirect();
                    return true;
                }
                lastBackDownTime = now;
            }
        }

        if (isSettingsKey(event)) {
            if (action == KeyEvent.ACTION_UP && !event.isCanceled()) {
                Intent open = settingsPanel ? new Intent(this, QuickSettingsActivity.class)
                        : new Intent(Settings.ACTION_SETTINGS);
                startActivity(open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            return true;
        }

        String app = appKeys.get(keyCode);
        if (app != null) {
            if (action == KeyEvent.ACTION_UP && !event.isCanceled()) RemoteKeys.launch(this, app);
            return true;
        }

        return false;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence pkgSeq = event.getPackageName();
        if (pkgSeq == null) return;
        String pkg = pkgSeq.toString();

        if (pkg.equals(getPackageName())) {
            return;
        }

        if (pkg.equals(AMAZON_LAUNCHER) || pkg.equals(AMAZON_HOMESTARTER)) {
            launchCleanYourTV();
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        logcatRunning = false;
        if (logcatThread != null) {
            logcatThread.interrupt();
            logcatThread = null;
        }
        if (instance == this) instance = null;
    }
}
